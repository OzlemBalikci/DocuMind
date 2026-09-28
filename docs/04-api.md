# 4. API Sözleşmesi

- Base URL: `http://localhost:8087/api`
- Format: JSON, tarih alanları ISO-8601 UTC (`2026-09-26T11:00:00Z`)
- Kimlik doğrulama: `Authorization: Bearer <jwt>` (`/api/auth/register`, `/api/auth/login`, `/actuator/health` hariç hepsi korumalı)

**Asenkron sözleşme.** İki uzun işlem — doküman indeksleme ve soru cevaplama — kuyruğa alınır ve `202 Accepted` döner. İstemci sonucu **polling** ile öğrenir. Sebep: 40 sayfalık bir PDF'in indekslenmesi dakikalar, lokal modelle bir cevap 20–60 saniye sürer.

## Özet Tablo

| Metot | Yol | Açıklama | Yanıt | AI |
|---|---|---|---|---|
| POST | `/api/auth/register` | Kayıt ol, token al | `201` | – |
| POST | `/api/auth/login` | Giriş yap, token al | `200` | – |
| GET | `/api/auth/me` | Kullanıcı + kalan günlük kota | `200` | – |
| POST | `/api/documents` | Doküman yükle, indekslemeyi kuyruğa al | **`202`** | kuyrukta |
| GET | `/api/documents?page=0&size=10` | Dokümanlarım (sayfalı) | `200` | – |
| GET | `/api/documents/{id}` | Doküman detayı + durum — **polling için** | `200` | – |
| POST | `/api/documents/{id}/retry` | Başarısız indekslemeyi tekrar dene | **`202`** | kuyrukta |
| DELETE | `/api/documents/{id}` | Dokümanı, chunk'larını ve dosyayı sil | `204` | – |
| POST | `/api/conversations` | Yeni sohbet aç | `201` | – |
| GET | `/api/conversations?page=0&size=20` | Sohbetlerim | `200` | – |
| GET | `/api/conversations/{id}` | Sohbet detayı (mesajlar + kaynaklar) — **polling için** | `200` | – |
| POST | `/api/conversations/{id}/messages` | Soru sor, cevabı kuyruğa al | **`202`** | kuyrukta |
| DELETE | `/api/conversations/{id}` | Sohbeti sil | `204` | – |
| GET | `/api/stats/me` | Panel istatistikleri | `200` | – |
| GET | `/actuator/health` | Sağlık (DB + RabbitMQ) | `200` | – |

---

## Auth

### `POST /api/auth/register`

```json
{ "email": "ayse@example.com", "password": "GucluSifre123", "fullName": "Ayşe Yılmaz" }
```

Validasyon: `email` geçerli e-posta, `password` 8–72 karakter (BCrypt 72 byte sınırı), `fullName` 2–100.

`201 Created`

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresAt": "2026-09-27T11:00:00Z",
  "user": { "id": 1, "email": "ayse@example.com", "fullName": "Ayşe Yılmaz", "role": "USER" }
}
```

Hatalar: `400 VALIDATION_ERROR`, `409 EMAIL_ALREADY_EXISTS`

### `POST /api/auth/login`

```json
{ "email": "ayse@example.com", "password": "GucluSifre123" }
```

`200 OK` → register ile aynı gövde. Hata: `401 INVALID_CREDENTIALS` (e-posta mı şifre mi yanlış, söyleme).

### `GET /api/auth/me`

```json
{
  "id": 1,
  "email": "ayse@example.com",
  "fullName": "Ayşe Yılmaz",
  "role": "USER",
  "quota": { "dailyLimit": 200, "usedToday": 34, "remaining": 166, "resetsAt": "2026-09-27T00:00:00Z" }
}
```

---

## Documents

### `POST /api/documents`

`multipart/form-data`:

| Alan | Zorunlu | Kural |
|---|---|---|
| `file` | evet | PDF / TXT / MD, max **20 MB** |
| `title` | hayır | 2–255 karakter; verilmezse dosya adından üretilir |

**`202 Accepted`** + `Location: /api/documents/42`

```json
{
  "id": 42,
  "title": "Çalışan El Kitabı 2026",
  "originalFilename": "el-kitabi.pdf",
  "contentType": "application/pdf",
  "sizeBytes": 1048576,
  "status": "PENDING",
  "pageCount": null,
  "chunkCount": 0,
  "embeddingModel": null,
  "errorMessage": null,
  "createdAt": "2026-09-26T11:00:00Z",
  "indexedAt": null
}
```

Hatalar:

| Kod | Ne zaman |
|---|---|
| `400 VALIDATION_ERROR` | Dosya yok, başlık geçersiz |
| `409 DOCUMENT_ALREADY_EXISTS` | Aynı içerik (SHA-256) daha önce yüklenmiş; yanıtta mevcut `documentId` döner |
| `413 FILE_TOO_LARGE` | 20 MB üstü |
| `415 UNSUPPORTED_FILE_TYPE` | PDF/TXT/MD dışı |
| `429 QUOTA_EXCEEDED` | Günlük AI kotası dolu |
| `503 QUEUE_UNAVAILABLE` | RabbitMQ'ya yayınlanamadı |

> İçerik tipini **uzantıya göre değil** gerçek içeriğe göre doğrula (magic byte / `Tika`). `rapor.pdf` adlı bir `.exe` kabul edilmemeli.

### `GET /api/documents/{id}`

Polling endpoint'i. `200 OK`

İndeksleme sürerken:

```json
{ "id": 42, "title": "Çalışan El Kitabı 2026", "status": "EMBEDDING", "pageCount": 38, "chunkCount": 0 }
```

Hazır olduğunda:

```json
{
  "id": 42,
  "title": "Çalışan El Kitabı 2026",
  "status": "READY",
  "pageCount": 38,
  "chunkCount": 82,
  "embeddingModel": "nomic-embed-text",
  "errorMessage": null,
  "indexedAt": "2026-09-26T11:04:22Z"
}
```

Başarısız olduğunda:

```json
{
  "id": 42,
  "status": "FAILED",
  "errorMessage": "PDF'ten metin çıkarılamadı. Taranmış (görüntü) PDF'ler şu an desteklenmiyor.",
  "chunkCount": 0
}
```

Hata: `404 NOT_FOUND` (yoksa ya da başkasınınsa)

### `GET /api/documents?page=0&size=10`

```json
{
  "content": [
    {
      "id": 42,
      "title": "Çalışan El Kitabı 2026",
      "contentType": "application/pdf",
      "sizeBytes": 1048576,
      "status": "READY",
      "pageCount": 38,
      "chunkCount": 82,
      "createdAt": "2026-09-26T11:00:00Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

> Spring'in `Page<T>` nesnesini doğrudan döndürme; kendi `PageResponse<T>` record'unu kullan (kararlı JSON yapısı).

### `POST /api/documents/{id}/retry`

Gövde yok. `FAILED` dokümanı yeniden kuyruğa alır.

Akıllı davranış: chunk'lar zaten varsa parse'ı atlar, doğrudan `EMBED_CHUNKS` işi yaratır (`embedding IS NULL` olanlar için). Chunk yoksa `PARSE_DOCUMENT`'tan başlar.

**`202 Accepted`** → güncel `DocumentResponse`, `status: "PENDING"`.

Hatalar: `404 NOT_FOUND`, `409 NOTHING_TO_RETRY` (doküman `FAILED` değil), `429 QUOTA_EXCEEDED`

### `DELETE /api/documents/{id}`

`204 No Content`. Chunk'lar ve alıntılar cascade ile, **diskteki dosya** servis katmanında silinir. `llm_usage_logs` kayıtları kalır.

---

## Conversations

### `POST /api/conversations`

```json
{ "title": "İzin ve tatil soruları" }
```

`title` opsiyonel (2–200); verilmezse ilk sorudan üretilir (ilk 60 karakter).

`201 Created`

```json
{ "id": 7, "title": "İzin ve tatil soruları", "createdAt": "2026-09-26T12:00:00Z", "messageCount": 0 }
```

### `POST /api/conversations/{id}/messages`

```json
{ "question": "Yıllık izin kaç gün ve nasıl talep ediliyor?" }
```

Validasyon: `question` 3–1000 karakter.

**`202 Accepted`**

```json
{
  "userMessage": {
    "id": 101,
    "role": "USER",
    "content": "Yıllık izin kaç gün ve nasıl talep ediliyor?",
    "status": "DONE",
    "createdAt": "2026-09-26T12:01:00Z"
  },
  "assistantMessage": {
    "id": 102,
    "role": "ASSISTANT",
    "content": null,
    "status": "PENDING",
    "citations": [],
    "createdAt": "2026-09-26T12:01:00Z"
  }
}
```

İki mesaj birlikte oluşturulur: kullanıcının sorusu (`DONE`) ve asistanın yer tutucusu (`PENDING`). Frontend hemen kullanıcı mesajını gösterip asistan baloncuğunda "düşünüyor" animasyonu çalıştırır.

Hatalar: `404 NOT_FOUND`, `409 NO_READY_DOCUMENTS` (hiç `READY` dokümanı yok — aranacak bir şey yok), `429 QUOTA_EXCEEDED`, `503 QUEUE_UNAVAILABLE`

### `GET /api/conversations/{id}`

Polling endpoint'i. `200 OK`

```json
{
  "id": 7,
  "title": "İzin ve tatil soruları",
  "createdAt": "2026-09-26T12:00:00Z",
  "messages": [
    {
      "id": 101,
      "role": "USER",
      "content": "Yıllık izin kaç gün ve nasıl talep ediliyor?",
      "status": "DONE",
      "citations": [],
      "createdAt": "2026-09-26T12:01:00Z"
    },
    {
      "id": 102,
      "role": "ASSISTANT",
      "content": "Yıllık izin süresi kıdeme göre değişiyor: 1–5 yıl arası çalışanlar için 14 gün, 5 yıl üzeri için 20 gün [1]. Talep, İK portalından en az 15 gün önce oluşturulan bir izin formu ile yapılıyor ve yöneticinin onayına düşüyor [2].",
      "status": "DONE",
      "model": "llama3.2:3b",
      "promptTokens": 1180,
      "completionTokens": 96,
      "latencyMs": 24310,
      "errorMessage": null,
      "citations": [
        {
          "rankOrder": 1,
          "chunkId": 9012,
          "documentId": 42,
          "documentTitle": "Çalışan El Kitabı 2026",
          "pageNumber": 12,
          "similarity": 0.78421,
          "excerpt": "Yıllık ücretli izin süreleri, hizmet süresi bir yıldan beş yıla kadar olanlar için on dört gün..."
        },
        {
          "rankOrder": 2,
          "chunkId": 9015,
          "documentId": 42,
          "documentTitle": "Çalışan El Kitabı 2026",
          "pageNumber": 13,
          "similarity": 0.71095,
          "excerpt": "İzin talepleri İK portalı üzerinden, planlanan izin başlangıcından en az on beş gün önce..."
        }
      ],
      "createdAt": "2026-09-26T12:01:00Z",
      "answeredAt": "2026-09-26T12:01:24Z"
    }
  ]
}
```

Bağlamda cevap bulunamadığında:

```json
{
  "id": 102,
  "role": "ASSISTANT",
  "content": "Dokümanlarında bu soruya dair bir bilgi bulamadım. Konuyla ilgili bir doküman yüklediysen farklı kelimelerle sormayı deneyebilirsin.",
  "status": "DONE",
  "citations": []
}
```

Hata durumunda:

```json
{
  "id": 102,
  "role": "ASSISTANT",
  "content": null,
  "status": "FAILED",
  "errorMessage": "Yapay zekâ modeli yanıt vermedi. Ollama'nın çalıştığından emin olup tekrar deneyin.",
  "citations": []
}
```

> `citations` boş + `status: DONE` → model "bulamadım" dedi. `citations` boş + `status: FAILED` → teknik hata. Frontend bu ikisini farklı göstermeli: birincisi normal bir cevap, ikincisi "Tekrar dene" butonu.

`excerpt` chunk içeriğinin ilk ~200 karakteri. Tam içeriği dönmek yanıtı şişirir; kullanıcı kaynağa tıklayınca ayrı bir istekle alınabilir (bonus).

### `DELETE /api/conversations/{id}`

`204 No Content`.

---

## Stats

### `GET /api/stats/me`

```json
{
  "documentCount": 6,
  "readyDocumentCount": 5,
  "totalChunks": 412,
  "totalPages": 186,
  "conversationCount": 9,
  "questionCount": 47,
  "answeredWithSources": 41,
  "unansweredCount": 6,
  "averageAnswerLatencyMs": 22480,
  "storageUsedBytes": 8912345
}
```

`answeredWithSources` / `questionCount` oranı **retrieval kalitesinin** göstergesi. Panelde bunu yüzde olarak göstermek hem kullanışlı hem portfolyoda dikkat çekici.

---

## Hata Formatı

Tüm hatalar `GlobalExceptionHandler` (`@RestControllerAdvice`) üzerinden aynı yapıda:

```json
{
  "timestamp": "2026-09-26T11:00:00Z",
  "status": 409,
  "code": "DOCUMENT_ALREADY_EXISTS",
  "message": "Bu dosyayı daha önce yüklemişsin.",
  "path": "/api/documents",
  "fieldErrors": []
}
```

Validasyon hatasında:

```json
"fieldErrors": [ { "field": "question", "message": "3 ile 1000 karakter arasında olmalı" } ]
```

| HTTP | code | Ne zaman |
|---|---|---|
| 400 | `VALIDATION_ERROR` | `@Valid` başarısız |
| 401 | `UNAUTHORIZED` / `INVALID_CREDENTIALS` | Token yok/geçersiz, hatalı giriş |
| 404 | `NOT_FOUND` | Kaynak yok veya başka kullanıcıya ait |
| 409 | `EMAIL_ALREADY_EXISTS`, `DOCUMENT_ALREADY_EXISTS`, `NO_READY_DOCUMENTS`, `NOTHING_TO_RETRY` | İş kuralı çakışması |
| 413 | `FILE_TOO_LARGE` | Boyut sınırı |
| 415 | `UNSUPPORTED_FILE_TYPE` | Desteklenmeyen dosya |
| 429 | `QUOTA_EXCEEDED` | Günlük AI kotası doldu (`Retry-After` header'ı ekle) |
| 503 | `QUEUE_UNAVAILABLE` | RabbitMQ'ya mesaj yayınlanamadı |
| 500 | `INTERNAL_ERROR` | Beklenmeyen hata (stack trace dönme, logla) |

> Spring Security'nin 401/403 cevapları `@RestControllerAdvice`'a düşmez. Aynı formatı vermek için `AuthenticationEntryPoint` ve `AccessDeniedHandler` yaz. `MaxUploadSizeExceededException` için de ayrı bir handler gerekir, yoksa Spring ham bir 500 döner.

### AI hataları nereye gitti?

Senkron bir tasarımda embedding/LLM hataları HTTP yanıtı olurdu. Asenkron akışta bu hatalar **kaynağın durumunda** taşınır:

| Nerede | Alan |
|---|---|
| Doküman | `status: "FAILED"` + `errorMessage` |
| Asistan mesajı | `status: "FAILED"` + `errorMessage` |

Frontend bunları görünce "Tekrar dene" gösterir (`POST /api/documents/{id}/retry` veya soruyu yeniden gönderme).

## Frontend Tipleri

`src/types/api.ts` içinde birebir TypeScript karşılıkları:

| Backend DTO (Java record) | TypeScript |
|---|---|
| `AuthResponse`, `UserResponse` | `AuthResponse`, `User` |
| `DocumentResponse` | `Document` |
| `ConversationResponse` / `ConversationDetailResponse` | `Conversation`, `ConversationDetail` |
| `MessageResponse` | `Messages` (`content: string \| null`) |
| `CitationResponse` | `Citation` |
| `PageResponse<T>` | `Page<T>` |
| `StatsResponse` | `Stats` |
| `ApiError` | `ApiError` |
| enum'lar | string union (`"PENDING" \| "PARSING" \| "EMBEDDING" \| "READY" \| "FAILED"`) |

### Polling kancaları

Doküman durumu:

```ts
const isIndexing = (s?: DocumentStatus) =>
  s === "PENDING" || s === "PARSING" || s === "EMBEDDING";

useQuery({
  queryKey: ["document", id],
  queryFn: () => api.get(`/documents/${id}`).then(r => r.data),
  refetchInterval: (query) => isIndexing(query.state.data?.status) ? 2000 : false,
});
```

Sohbet:

```ts
useQuery({
  queryKey: ["conversation", id],
  queryFn: () => api.get(`/conversations/${id}`).then(r => r.data),
  refetchInterval: (query) => {
    const waiting = query.state.data?.messages.some(
      m => m.status === "PENDING" || m.status === "RUNNING");
    return waiting ? 1500 : false;
  },
});
```

Büyük dokümanlarda indeksleme dakikalar sürebilir; doküman listesinde de aynı polling'i uygula ve 5 dakika sonra durdurup "Beklenenden uzun sürüyor" mesajı göster.
