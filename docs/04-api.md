# 4. API Sözleşmesi

- Base URL: lokal `http://localhost:8087/api`, canlı `https://<render-servisin>.onrender.com/api`
- Format: JSON, tarih alanları ISO-8601 UTC (`2026-09-23T11:00:00Z`)
- Kimlik doğrulama: `Authorization: Bearer <jwt>` (`/api/auth/register`, `/api/auth/login`, `/actuator/health` hariç hepsi korumalı)

## Özet Tablo

| Metot | Yol | Açıklama | LLM çağrısı |
|---|---|---|---|
| POST | `/api/auth/register` | Kayıt ol, token al | – |
| POST | `/api/auth/login` | Giriş yap, token al | – |
| GET | `/api/auth/me` | Oturumdaki kullanıcı + kalan günlük kota | – |
| POST | `/api/interviews` | Yeni mülakat oluştur, soruları üret | ✅ 1 |
| GET | `/api/interviews?page=0&size=10` | Mülakatlarım (sayfalı) | – |
| GET | `/api/interviews/{id}` | Mülakat detayı (sorular + cevaplar) | – |
| POST | `/api/interviews/{id}/questions/{questionId}/answer` | Cevap gönder, değerlendirme al | ✅ 1 |
| POST | `/api/interviews/{id}/complete` | Mülakatı bitir, özet üret | ✅ 1 |
| DELETE | `/api/interviews/{id}` | Mülakatı sil | – |
| GET | `/api/stats/me` | Panel istatistikleri | – |
| GET | `/actuator/health` | Sağlık kontrolü (Render için) | – |

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
  "quota": { "dailyLimit": 15, "usedToday": 7, "remaining": 8, "resetsAt": "2026-09-24T00:00:00Z" }
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

`201 Created` → `InterviewDetailResponse`:

```json
{
  "id": 42,
  "jobTitle": "Java Backend Developer",
  "seniority": "JUNIOR",
  "techStack": "Java, Spring Boot, PostgreSQL, Docker",
  "language": "TR",
  "status": "CREATED",
  "questionCount": 5,
  "answeredCount": 0,
  "overallScore": null,
  "summaryFeedback": null,
  "createdAt": "2026-09-23T11:00:00Z",
  "completedAt": null,
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

> `expectedKeyPoints` bu yanıtta **yoktur**. Sadece soru cevaplandıktan sonra `answer` içinde döner.

Hatalar: `400 VALIDATION_ERROR`, `429 QUOTA_EXCEEDED`, `502 LLM_BAD_RESPONSE`, `503 LLM_UNAVAILABLE`

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

### `GET /api/interviews/{id}`

`200 OK` → `InterviewDetailResponse` (cevaplanmış sorularda `answer` dolu):

```json
{
  "id": 101,
  "orderIndex": 1,
  "questionText": "...",
  "category": "TECHNICAL",
  "difficulty": "MEDIUM",
  "answer": {
    "answerText": "Proxy tabanlı çalışır ...",
    "score": 7,
    "strengths": ["Proxy mekanizmasını doğru açıkladı"],
    "improvements": ["Self-invocation problemine çözüm önermedi"],
    "idealAnswer": "Spring @Transactional'ı AOP proxy ile uygular ...",
    "expectedKeyPoints": ["AOP proxy", "self-invocation", "propagation"],
    "createdAt": "2026-09-23T11:03:12Z"
  }
}
```

Hatalar: `404 NOT_FOUND` (yoksa ya da başkasınınsa)

### `POST /api/interviews/{id}/questions/{questionId}/answer`

```json
{ "answerText": "Spring @Transactional'ı proxy ile uygular..." }
```

Validasyon: `answerText` 1–4000 karakter.

`200 OK` → güncellenmiş soru nesnesi (yukarıdaki gibi `answer` dolu).

Hatalar: `404 NOT_FOUND`, `409 ALREADY_ANSWERED`, `409 SESSION_COMPLETED`, `429 QUOTA_EXCEEDED`, `502/503 LLM_*`

### `POST /api/interviews/{id}/complete`

Gövde yok. Tüm sorular cevaplanmış olmalı.

`200 OK` → `InterviewDetailResponse`, `status: "COMPLETED"`, `overallScore` (cevap puanlarının ortalaması, backend hesaplar — LLM'e hesaplatma) ve `summaryFeedback` (LLM üretir) dolu.

Hatalar: `409 UNANSWERED_QUESTIONS`, `409 SESSION_COMPLETED`

### `DELETE /api/interviews/{id}`

`204 No Content`

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
| 409 | `EMAIL_ALREADY_EXISTS`, `ALREADY_ANSWERED`, `SESSION_COMPLETED`, `UNANSWERED_QUESTIONS` | İş kuralı çakışması |
| 429 | `QUOTA_EXCEEDED` | Günlük LLM kotası doldu (`Retry-After` header'ı ekle) |
| 502 | `LLM_BAD_RESPONSE` | LLM parse edilemeyen/geçersiz JSON döndü (retry sonrası) |
| 503 | `LLM_UNAVAILABLE` | Groq zaman aşımı / 5xx / Groq'un kendi 429'u |
| 500 | `INTERNAL_ERROR` | Beklenmeyen hata (stack trace dönme, logla) |

> Not: Spring Security'nin 401/403 cevapları `@RestControllerAdvice`'a düşmez. Aynı formatı vermek için `AuthenticationEntryPoint` ve `AccessDeniedHandler` implementasyonu yaz.

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
| enum'lar (`Seniority`, `SessionStatus`, ...) | string union tipleri (`"JUNIOR" \| "MID" ...`) |

Tarih alanlarını `string` olarak tut, gösterirken `Intl.DateTimeFormat("tr-TR")` ile biçimlendir. Bonus: backend'e springdoc eklersen `openapi-typescript` ile bu tipleri otomatik üretebilirsin.
