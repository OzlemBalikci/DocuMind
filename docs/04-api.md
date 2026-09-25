# 4. API Sözleşmesi

- Base URL: `http://localhost:8087/api`
- Format: JSON, tarih alanları ISO-8601 UTC (`2026-09-23T11:00:00Z`)
- Kimlik doğrulama: `Authorization: Bearer <jwt>` (`/api/auth/register`, `/api/auth/login`, `/actuator/health` hariç hepsi korumalı)

**Asenkron sözleşme.** LLM gerektiren üç işlem (`soru üretme`, `cevap değerlendirme`, `özet`) kuyruğa alınır ve `202 Accepted` döner. İstemci sonucu `GET /api/interviews/{id}` ile **polling** yaparak öğrenir. Bunun sebebi lokal Ollama'nın 30–90 saniye sürebilmesi; senkron bir API'de bu istek zaman aşımına uğrardı.

## Özet Tablo

| Metot | Yol | Açıklama | Yanıt | LLM |
|---|---|---|---|---|
| POST | `/api/auth/register` | Kayıt ol, token al | `201` | – |
| POST | `/api/auth/login` | Giriş yap, token al | `200` | – |
| GET | `/api/auth/me` | Oturumdaki kullanıcı + kalan günlük kota | `200` | – |
| POST | `/api/interviews` | Yeni mülakat oluştur, soru üretimini kuyruğa al | **`202`** | kuyrukta |
| GET | `/api/interviews?page=0&size=10` | Mülakatlarım (sayfalı) | `200` | – |
| GET | `/api/interviews/{id}` | Mülakat detayı (durum + sorular + cevaplar) — **polling için** | `200` | – |
| POST | `/api/interviews/{id}/questions/{questionId}/answer` | Cevap gönder, değerlendirmeyi kuyruğa al | **`202`** | kuyrukta |
| POST | `/api/interviews/{id}/complete` | Mülakatı bitir, özet üretimini kuyruğa al | **`202`** | kuyrukta |
| POST | `/api/interviews/{id}/retry` | Başarısız işi tekrar kuyruğa al | **`202`** | kuyrukta |
| DELETE | `/api/interviews/{id}` | Mülakatı sil | `204` | – |
| GET | `/api/stats/me` | Panel istatistikleri | `200` | – |
| GET | `/actuator/health` | Sağlık kontrolü (DB + RabbitMQ) | `200` | – |

---

## Auth

### `POST /api/auth/register`

```json
{
  "email": "ayse@example.com",
  "password": "GucluSifre123",
  "fullName": "Ayşe Yılmaz"
}
```

Validasyon: `email` geçerli e-posta, `password` 8–72 karakter (BCrypt 72 byte sınırı), `fullName` 2–100 karakter.

`201 Created`

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresAt": "2026-09-24T11:00:00Z",
  "user": { "id": 1, "email": "ayse@example.com", "fullName": "Ayşe Yılmaz", "role": "USER" }
}
```

Hatalar: `400 VALIDATION_ERROR`, `409 EMAIL_ALREADY_EXISTS`

### `POST /api/auth/login`

```json
{ "email": "ayse@example.com", "password": "GucluSifre123" }
```

`200 OK` → register ile aynı gövde. Hatalar: `401 INVALID_CREDENTIALS` (e-posta mı şifre mi yanlış, söyleme).

### `GET /api/auth/me`

`200 OK`

```json
{
  "id": 1,
  "email": "ayse@example.com",
  "fullName": "Ayşe Yılmaz",
  "role": "USER",
  "quota": { "dailyLimit": 50, "usedToday": 7, "remaining": 43, "resetsAt": "2026-09-24T00:00:00Z" }
}
```

---

## Interviews

### `POST /api/interviews`

```json
{
  "jobTitle": "Java Backend Developer",
  "seniority": "JUNIOR",
  "techStack": "Java, Spring Boot, PostgreSQL, Docker",
  "jobDescription": "Opsiyonel: iş ilanı metni (max 3000 karakter)",
  "questionCount": 5,
  "language": "TR"
}
```

| Alan | Kural |
|---|---|
| `jobTitle` | zorunlu, 2–120 |
| `seniority` | `INTERN` \| `JUNIOR` \| `MID` \| `SENIOR` |
| `techStack` | zorunlu, 2–500 |
| `jobDescription` | opsiyonel, max 3000 |
| `questionCount` | 3–10 |
| `language` | `TR` \| `EN` |

**`202 Accepted`** → `InterviewDetailResponse` (henüz soru yok):

```json
{
  "id": 42,
  "jobTitle": "Java Backend Developer",
  "seniority": "JUNIOR",
  "techStack": "Java, Spring Boot, PostgreSQL, Docker",
  "language": "TR",
  "status": "PENDING",
  "questionCount": 5,
  "answeredCount": 0,
  "overallScore": null,
  "summaryFeedback": null,
  "errorMessage": null,
  "createdAt": "2026-09-23T11:00:00Z",
  "completedAt": null,
  "questions": []
}
```

Yanıta `Location: /api/interviews/42` header'ı ekle; istemci polling'e bu adresten devam eder.

Hatalar: `400 VALIDATION_ERROR`, `429 QUOTA_EXCEEDED`, `503 QUEUE_UNAVAILABLE` (RabbitMQ'ya bağlanılamadı)

> `502 LLM_BAD_RESPONSE` ve `503 LLM_UNAVAILABLE` bu endpoint'ten **dönmez**; LLM hataları artık HTTP yanıtında değil, oturumun `status: "FAILED"` ve `errorMessage` alanlarında görünür.

### `GET /api/interviews/{id}`

Polling'in yapıldığı endpoint. `200 OK` → `InterviewDetailResponse`.

Soru üretimi sürerken:

```json
{
  "id": 42,
  "status": "GENERATING",
  "questionCount": 5,
  "answeredCount": 0,
  "questions": []
}
```

Sorular hazır olduğunda (`status: "READY"`):

```json
{
  "id": 42,
  "status": "READY",
  "questions": [
    {
      "id": 101,
      "orderIndex": 1,
      "questionText": "Spring'de @Transactional anotasyonu nasıl çalışır? Aynı sınıf içinden çağrıldığında neden etkisiz kalabilir?",
      "category": "TECHNICAL",
      "difficulty": "MEDIUM",
      "answer": null
    }
  ]
}
```

Cevap verilmiş ama değerlendirmesi beklenen bir soru:

```json
{
  "id": 101,
  "orderIndex": 1,
  "questionText": "...",
  "category": "TECHNICAL",
  "difficulty": "MEDIUM",
  "answer": {
    "id": 501,
    "answerText": "Proxy tabanlı çalışır ...",
    "evaluationStatus": "PENDING",
    "score": null,
    "strengths": [],
    "improvements": [],
    "idealAnswer": null,
    "expectedKeyPoints": null,
    "errorMessage": null,
    "createdAt": "2026-09-23T11:03:12Z",
    "evaluatedAt": null
  }
}
```

Değerlendirme tamamlandığında (`evaluationStatus: "DONE"`) `score`, `strengths`, `improvements`, `idealAnswer`, `expectedKeyPoints` ve `evaluatedAt` dolar.

Soru üretimi başarısız olduğunda:

```json
{
  "id": 42,
  "status": "FAILED",
  "errorMessage": "Yapay zekâ modeli yanıt vermedi. Ollama'nın çalıştığından emin olup tekrar deneyin.",
  "questions": []
}
```

> `expectedKeyPoints` soru cevaplanmadan **asla** dönmez (kopya olmasın). Sadece `answer.evaluationStatus = "DONE"` olduğunda `answer` nesnesinin içinde görünür.

Hatalar: `404 NOT_FOUND` (yoksa ya da başkasınınsa)

### `GET /api/interviews?page=0&size=10`

`200 OK`

```json
{
  "content": [
    {
      "id": 42,
      "jobTitle": "Java Backend Developer",
      "seniority": "JUNIOR",
      "status": "COMPLETED",
      "questionCount": 5,
      "answeredCount": 5,
      "overallScore": 7.4,
      "createdAt": "2026-09-23T11:00:00Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

> Spring'in `Page<T>` nesnesini doğrudan döndürme; yukarıdaki gibi kendi `PageResponse<T>` record'unu kullan (kararlı JSON yapısı).

### `POST /api/interviews/{id}/questions/{questionId}/answer`

```json
{ "answerText": "Spring @Transactional'ı proxy ile uygular..." }
```

Validasyon: `answerText` 1–4000 karakter.

**`202 Accepted`**

```json
{
  "id": 501,
  "questionId": 101,
  "answerText": "Spring @Transactional'ı proxy ile uygular...",
  "evaluationStatus": "PENDING",
  "score": null,
  "createdAt": "2026-09-23T11:03:12Z"
}
```

Kullanıcı cevabı gönderir göndermez bir sonraki soruya geçebilir. Değerlendirme hazır olunca polling ile gelir.

Hatalar: `404 NOT_FOUND`, `409 ALREADY_ANSWERED`, `409 SESSION_COMPLETED`, `409 SESSION_NOT_READY` (oturum `PENDING`/`GENERATING`/`FAILED`), `429 QUOTA_EXCEEDED`, `503 QUEUE_UNAVAILABLE`

### `POST /api/interviews/{id}/complete`

Gövde yok. Tüm soruların cevaplanmış **ve** değerlendirmelerinin bitmiş olması gerekir.

**`202 Accepted`** → `InterviewDetailResponse`, `status` hâlâ `IN_PROGRESS`. Özet hazır olunca `status: "COMPLETED"`, `overallScore` (cevap puanlarının ortalaması, **backend hesaplar** — LLM'e hesaplatma) ve `summaryFeedback` dolar.

Hatalar: `409 UNANSWERED_QUESTIONS`, `409 EVALUATIONS_PENDING` (cevaplar var ama değerlendirmeleri sürüyor), `409 SESSION_COMPLETED`

### `POST /api/interviews/{id}/retry`

Başarısız bir LLM işini yeniden kuyruğa koyar. Gövde yok.

Davranış:
- Oturum `FAILED` ise → soru üretimi yeniden kuyruğa girer, `status: "PENDING"` olur.
- Oturumda `evaluationStatus = "FAILED"` cevaplar varsa → hepsi yeniden kuyruğa girer.

**`202 Accepted`** → güncel `InterviewDetailResponse`.

Hatalar: `404 NOT_FOUND`, `409 NOTHING_TO_RETRY` (başarısız iş yok), `429 QUOTA_EXCEEDED`

### `DELETE /api/interviews/{id}`

`204 No Content`. İlgili sorular, cevaplar ve işler cascade ile silinir; `llm_usage_logs` kayıtları kalır (kota atlatılamasın).

---

## Stats

### `GET /api/stats/me`

```json
{
  "totalSessions": 6,
  "completedSessions": 4,
  "averageScore": 6.85,
  "totalAnswers": 27,
  "byCategory": [
    { "category": "TECHNICAL", "averageScore": 7.1, "answered": 15 },
    { "category": "BEHAVIORAL", "averageScore": 8.0, "answered": 6 },
    { "category": "SYSTEM_DESIGN", "averageScore": 5.2, "answered": 6 }
  ],
  "recentScores": [
    { "sessionId": 42, "jobTitle": "Java Backend Developer", "overallScore": 7.4, "completedAt": "2026-09-23T11:20:00Z" }
  ]
}
```

> `byCategory` ve `averageScore` hesaplanırken sadece `evaluationStatus = 'DONE'` cevaplar sayılır.

---

## Hata Formatı

Tüm hatalar `GlobalExceptionHandler` (`@RestControllerAdvice`) üzerinden aynı yapıda döner:

```json
{
  "timestamp": "2026-09-23T11:00:00Z",
  "status": 409,
  "code": "ALREADY_ANSWERED",
  "message": "Bu soru zaten cevaplandı.",
  "path": "/api/interviews/42/questions/101/answer",
  "fieldErrors": []
}
```

Validasyon hatasında `fieldErrors` dolar:

```json
"fieldErrors": [
  { "field": "questionCount", "message": "3 ile 10 arasında olmalı" }
]
```

| HTTP | code | Ne zaman |
|---|---|---|
| 400 | `VALIDATION_ERROR` | `@Valid` başarısız |
| 401 | `UNAUTHORIZED` / `INVALID_CREDENTIALS` | Token yok/geçersiz, hatalı giriş |
| 404 | `NOT_FOUND` | Kaynak yok veya başka kullanıcıya ait |
| 409 | `EMAIL_ALREADY_EXISTS`, `ALREADY_ANSWERED`, `SESSION_COMPLETED`, `SESSION_NOT_READY`, `UNANSWERED_QUESTIONS`, `EVALUATIONS_PENDING`, `NOTHING_TO_RETRY` | İş kuralı çakışması |
| 429 | `QUOTA_EXCEEDED` | Günlük LLM kotası doldu (`Retry-After` header'ı ekle) |
| 503 | `QUEUE_UNAVAILABLE` | RabbitMQ'ya mesaj yayınlanamadı |
| 500 | `INTERNAL_ERROR` | Beklenmeyen hata (stack trace dönme, logla) |

> Not: Spring Security'nin 401/403 cevapları `@RestControllerAdvice`'a düşmez. Aynı formatı vermek için `AuthenticationEntryPoint` ve `AccessDeniedHandler` implementasyonu yaz.

### LLM hataları nereye gitti?

Senkron tasarımda `502 LLM_BAD_RESPONSE` ve `503 LLM_UNAVAILABLE` HTTP yanıtı olarak dönüyordu. Asenkron akışta LLM çağrısı HTTP isteğinden sonra çalıştığı için bu hatalar **kaynağın durumunda** taşınır:

| Nerede | Alan | Örnek |
|---|---|---|
| Oturum | `status: "FAILED"` + `errorMessage` | Soru üretimi başarısız |
| Cevap | `evaluationStatus: "FAILED"` + `errorMessage` | Değerlendirme başarısız |

Frontend bu durumları görünce "Tekrar dene" butonu gösterir ve `POST /api/interviews/{id}/retry` çağırır.

## Frontend Tipleri

Frontend'de `src/types/api.ts` içinde yukarıdaki JSON gövdelerinin birebir TypeScript karşılıklarını tanımla:

| Backend DTO (Java record) | TypeScript tipi |
|---|---|
| `AuthResponse`, `UserResponse` | `AuthResponse`, `User` |
| `InterviewDetailResponse` | `InterviewDetail` |
| `QuestionResponse` / `AnswerResponse` | `Question`, `Answer` (`answer: Answer \| null`) |
| `InterviewSummaryResponse` + `PageResponse<T>` | `InterviewListItem`, `Page<T>` |
| `StatsResponse` | `Stats` |
| `ApiError` | `ApiError` |
| enum'lar (`Seniority`, `SessionStatus`, `EvaluationStatus`, ...) | string union tipleri (`"PENDING" \| "GENERATING" \| "READY" ...`) |

Tarih alanlarını `string` olarak tut, gösterirken `Intl.DateTimeFormat("tr-TR")` ile biçimlendir. Bonus: backend'e springdoc eklersen `openapi-typescript` ile bu tipleri otomatik üretebilirsin.

### Polling kancası

TanStack Query ile:

```ts
const isPending = (s?: SessionStatus) => s === "PENDING" || s === "GENERATING";

useQuery({
  queryKey: ["interview", id],
  queryFn: () => api.get(`/interviews/${id}`).then(r => r.data),
  refetchInterval: (query) => {
    const d = query.state.data;
    if (!d) return 2000;
    const waiting = isPending(d.status)
      || d.questions.some(q => q.answer?.evaluationStatus === "PENDING"
                            || q.answer?.evaluationStatus === "RUNNING");
    return waiting ? 2000 : false;
  },
});
```

2 dakikadan uzun süren beklemelerde polling'i durdurup kullanıcıya "Beklenenden uzun sürüyor, Ollama çalışıyor mu?" mesajı göster.
