# 8. Yol Haritası (2,5 Gün)

Önceki plan 2 gündü ve son bloğu buluta deploy etmeye ayrılmıştı. Deploy çıktı, yerine **Ollama kurulumu + asenkron kuyruk mimarisi** geldi; bu net bir artı yük. Gerçekçi süre 2,5 gün.

Temel ilke değişti: "erken deploy et" yerine **erken uçtan uca çalıştır**. 1. günün sonunda mock LLM ile kayıt → mülakat → cevap → özet akışı lokalde çalışıyor olsun. Gerçek model ve asenkron kuyruk sonra gelir; çünkü çalışan bir iskelet üzerine eklemek, baştan her şeyi birlikte kurmaktan kolaydır.

Her blok sonunda: `./mvnw verify` → commit → push → PR → merge.

## Hazırlık (başlamadan önce, ~45 dk)

- [ ] **JDK 21** kurulu ve `JAVA_HOME` doğru (`java -version` → 21). Şu an kurulu değil, bu olmadan hiçbir şey çalışmaz
- [ ] Docker Desktop, Node 22, Git, IntelliJ IDEA (Community yeterli) / VS Code
- [ ] [Ollama](https://ollama.com/download) kuruldu, `ollama pull llama3.2:3b` çalıştırıldı
- [ ] `ollama run llama3.2:3b "Merhaba"` ile model yanıt veriyor, süresi ölçüldü
- [ ] Bu dokümanların hepsi bir kez okundu

## 1. Gün — Backend İskeleti + Senkron Akış (mock ile)

| Saat | İş | Çıktı | Doküman |
|---|---|---|---|
| 09:00–09:30 | Bağımlılıklar: `spring-ai-starter-model-ollama`, `spring-boot-starter-amqp`, `testcontainers-rabbitmq`. `docker-compose.yml`'a RabbitMQ ekle, `.env` düzenle | `docker compose up -d` → iki servis healthy | [Lokal Çalıştırma](07-lokal-calistirma.md) |
| 09:30–11:00 | `application.yml`, Flyway `V1` (asenkron alanlarla), entity'ler + repository'ler | `ddl-auto=validate` hatasız, uygulama açılıyor | [Mimari](02-mimari.md), [Veritabanı](03-veritabani.md) |
| 11:00–12:30 | Auth: `register`, `login`, `me`, `JwtService`, JWT filtresi, `SecurityConfig`, CORS, `GlobalExceptionHandler` + `ApiError`, 401 için `AuthenticationEntryPoint` | HTTP client ile token alınıyor | [API → Auth](04-api.md#auth) |
| 12:30–13:30 | Öğle arası | | |
| 13:30–14:00 | `LlmClient` arayüzü + `MockLlmClient` + LLM DTO record'ları | Mock ile akış denenebilir | [LLM → Mock Mod](05-llm-entegrasyonu.md#mock-mod) |
| 14:00–16:30 | `InterviewService` + controller — **önce senkron**: oluştur, listele, detay, cevapla, tamamla, sil. Sahiplik kontrolleri, durum geçişleri, 409 kuralları | Mock ile uçtan uca akış çalışıyor | [API → Interviews](04-api.md#interviews) |
| 16:30–18:00 | Testler: `JwtService`, `InterviewService` (Mockito), 1 Testcontainers entegrasyon testi (kayıt → mülakat → cevap → tamamla) | `./mvnw verify` yeşil | |
| 18:00–19:00 | CI'yi doğrula: `src/**` değişince workflow koşuyor, mock ile saniyeler sürüyor | Yeşil pipeline | [CI](06-ci-cd.md) |

1. gün bitiş kontrolü:
- [ ] Mock LLM ile tam akış lokalde çalışıyor
- [ ] `./mvnw verify` ve GitHub Actions yeşil
- [ ] `main`'e push → CI otomatik koşuyor

> Neden önce senkron? Asenkron yapıyı çalışan bir akışın üzerine eklemek, iki bilinmeyeni (iş kuralları + kuyruk) aynı anda çözmeye çalışmaktan çok daha hızlı. Şema zaten asenkroniye hazır olduğu için bu geçişte migration yazmayacaksın.

## 2. Gün — Asenkron Mimari + Ollama

| Saat | İş | Çıktı | Doküman |
|---|---|---|---|
| 09:00–10:30 | `messaging/`: `RabbitConfig` (exchange, 3 kuyruk, DLX, JSON converter), `LlmJobPublisher`, mesaj record'ları | Mesaj yayınlanıyor, RabbitMQ UI'da görünüyor | [Mimari → Mesajlaşma](02-mimari.md#mesajlaşma-topolojisi) |
| 10:30–12:30 | `LlmJobListener`: soru üretme işini asenkrona çevir. `llm_jobs` yaşam döngüsü, idempotency, durum güncellemeleri, kısa transaction'lar | `POST /api/interviews` → `202`, sorular arka planda geliyor | [API](04-api.md) |
| 12:30–13:30 | Öğle arası | | |
| 13:30–14:30 | Cevap değerlendirme ve özet işlerini de asenkrona çevir. `/retry` endpoint'i | Üç LLM işi de kuyrukta | |
| 14:30–15:30 | Hata yolu: retry politikası, DLQ, `FAILED` durumları, kullanıcıya gösterilen mesajlar | Ollama kapalıyken iş `FAILED` oluyor, DLQ doluyor | [LLM → Hata Yönetimi](05-llm-entegrasyonu.md#hata-yönetimi-ve-zaman-aşımları) |
| 15:30–17:00 | `OllamaLlmClient`: prompt dosyaları, `.entity()` ile JSON → record, `LlmResponseValidator`, `UsageQuotaService`, `llm_usage_logs` | **Gerçek sorular ve değerlendirmeler geliyor** | [LLM](05-llm-entegrasyonu.md) |
| 17:00–18:00 | Prompt turlaması: küçük modelin çıktısını düzeltmek için prompt'ları revize et, Türkçe kalitesini ve JSON uyumunu iyileştir | Kabul edilebilir kalitede çıktı | [LLM → Prompt'lar](05-llm-entegrasyonu.md#promptlar) |
| 18:00–19:00 | Testler: `LlmJobListener` idempotency, validator birim testleri, Testcontainers'a RabbitMQ ekle, Awaitility ile asenkron entegrasyon testi | `./mvnw verify` yeşil | |

2. gün bitiş kontrolü:
- [ ] Gerçek Ollama ile uçtan uca akış çalışıyor
- [ ] Ollama kapalıyken hata yolu düzgün çalışıyor, `/retry` işi kurtarıyor
- [ ] CI hâlâ mock ile hızlı ve yeşil

## 3. Gün (yarım) — Frontend + Vitrin

Frontend backend'den daha çok zaman ister; 2,5 günlük planda buraya sığdırmak için kapsamı dar tut.

| Saat | İş | Çıktı |
|---|---|---|
| 09:00–09:45 | Vite + React + TS, Tailwind, shadcn/ui (`button, card, input, textarea, label, badge, progress, skeleton, sonner, dialog, select`), React Router, TanStack Query, axios instance | Sayfalar arası geçiş |
| 09:45–10:30 | `AuthContext` (token `localStorage`'da), login/register formları (react-hook-form + zod), `ProtectedRoute`, 401'de otomatik çıkış | Giriş yapılabiliyor |
| 10:30–12:30 | Yeni mülakat formu → mülakat ekranı: **polling**, "Sorular hazırlanıyor" ekranı, ilerleme çubuğu, soru kartı, cevap alanı, değerlendirme paneli (bekleyen/hazır/başarısız durumlarıyla) | Ana özellik çalışıyor |
| 13:30–14:30 | Sonuç sayfası, geçmiş (sayfalı), panel: istatistik kartları + kategori grafiği (recharts). Backend'de `/api/stats/me` | Tüm sayfalar hazır |
| 14:30–15:15 | Durum ekranları: skeleton, empty, error, `FAILED` + "Tekrar dene", kalan kota göstergesi, mobil görünüm | Cilalı UX |
| 15:15–16:00 | Frontend workflow (lint + typecheck + test + build), birkaç Vitest testi | CI'da frontend job'ı yeşil |
| 16:00–17:30 | **README vitrini**: GIF, mimari + ER diyagramı, teknik kararlar, 3 komutluk kurulum | Vitrin hazır |
| 17:30–18:30 | Zaman kalırsa bonus: Swagger UI → Dependabot → streaming | |

## Geride kalırsan neyi kesersin?

Sırasıyla bunlardan vazgeç. **Çekirdek akıştan (kayıt → mülakat → cevap → özet), asenkron kuyruktan ve README'den asla vazgeçme** — kuyruk bu projenin ayırt edici özelliği, README ise canlı link olmadığı için tek vitrin.

1. Bonus özellikler
2. Frontend testleri (backend testleri kalsın)
3. Panel grafiği (sadece sayı kartları kalsın)
4. Özet işini asenkronlaştırmak (soru üretme ve değerlendirme asenkron kalsın, `complete` senkron olsun)
5. Silme endpoint'i ve geçmiş sayfalaması
6. `/retry` endpoint'i (hata durumunu göstermek yeterli)

## Örnek Commit / PR Planı

| PR | İçerik |
|---|---|
| `chore: project skeleton` | Klasörler, docker-compose (postgres + rabbitmq), .env.example, docs |
| `feat(backend): database schema and entities` | Flyway V1, entity'ler, repository'ler |
| `feat(backend): jwt authentication` | Auth + security + hata yönetimi |
| `feat(backend): interview flow with mock llm` | Service, controller, mock client (senkron) |
| `feat(backend): async llm processing with rabbitmq` | Messaging paketi, listener, llm_jobs, DLQ |
| `feat(backend): ollama integration and usage quota` | Gerçek LLM, doğrulama katmanı, kota |
| `test(backend): unit and integration tests` | Testcontainers: postgres + rabbitmq, Awaitility |
| `feat(frontend): auth pages` | |
| `feat(frontend): interview flow with polling` | |
| `feat(frontend): dashboard and history` | |
| `ci: frontend pipeline` | |
| `docs: readme with screenshots` | |

## README Vitrini

Canlı link olmadığı için README bu projenin **tek** vitrini. İşe alımcı ortalama 30 saniye bakar. Bu sırayla yaz:

1. **Tek cümle + GIF.** 15–20 sn'lik ekran kaydı ([ScreenToGif](https://www.screentogif.com/)): mülakat oluştur → "sorular hazırlanıyor" → sorular geliyor → cevap → değerlendirme. Asenkron akışı göstermesi kritik
2. Rozetler: backend CI, frontend CI, Java, Spring Boot, RabbitMQ, Ollama, React
3. Özellikler (5–6 madde) — "tamamen lokal çalışır, API anahtarı gerekmez" maddesini öne çıkar
4. Mimari diyagram ([Mimari](02-mimari.md#genel-bakış)'den Mermaid)
5. Teknoloji tablosu
6. **Teknik kararlar** — en önemli bölüm, seni diğer adaylardan ayırır:
   - Neden asenkron kuyruk? (lokal model 30–90 sn sürüyor, HTTP isteği bunu kaldırmaz)
   - Neden RabbitMQ, neden Kafka değil?
   - Neden Ollama, neden bulut LLM değil? (ücretsiz, anahtarsız, veri gizliliği; bedeli: deploy edilemez)
   - Neden LLM çağrısı transaction dışında?
   - Neden `overallScore` backend'de hesaplanıyor?
   - Küçük modelin bozuk çıktısına karşı ne yapıldı? (doğrulama katmanı)
   - Prompt injection'a karşı ne yapıldı?
   - Kuyrukta idempotency nasıl sağlandı?
7. ER diyagramı
8. **Lokal kurulum (3 komut)** — canlı demo olmadığı için bunun kusursuz çalışması şart. Temiz bir makinede baştan dene
9. Test ve CI açıklaması — mock LLM'in neden var olduğu dahil
10. **Bilinen kısıtlar ve yol haritası**: "Ollama ücretsiz barındırmada çalışmadığı için canlı demo yok; `LlmClient` arayüzü sayesinde Groq'a geçip deploy etmek bir implementasyon + config değişikliği", "streaming eklenecek"

> 10. maddeyi savunmacı değil, bilinçli bir mühendislik kararı olarak yaz. "Deploy edemedim" ile "veri gizliliği ve sıfır maliyet için lokal modeli seçtim, bunun bedeli canlı demo oldu" arasındaki fark, işe alımcının gözünde büyüktür.

## Mülakatta Anlatma Rehberi

| Soru | Cevabının özü |
|---|---|
| "Neden kuyruk kullandın, gerçekten gerekli miydi?" | Lokal model bir soru setini 30–90 sn'de üretiyor; senkron API'de istek zaman aşımına uğrar, tarayıcı bekler, DB bağlantısı tutulur. Ölçülmüş bir gereksinim, süs değil |
| "Neden RabbitMQ, neden Kafka değil?" | İhtiyaç task queue; DLQ, retry, prefetch kutudan çıkıyor. Kafka event streaming için; burada DLQ'yu elle kurmam ve daha fazla kaynak ayırmam gerekirdi |
| "Aynı mesaj iki kez gelirse?" | `llm_jobs` kaydının durumuna bakıyorum, `DONE` ise ACK'layıp çıkıyorum. RabbitMQ en az bir kez teslim garantisi verir |
| "İş kalıcı olarak başarısız olursa?" | 3 deneme sonra DLQ; `llm_jobs.status = FAILED`, oturum `FAILED`, kullanıcı anlaşılır mesaj ve "Tekrar dene" görüyor |
| "LLM'den gelen JSON bozuk olursa?" | Markdown temizleme + parse + doğrulama katmanı, 1 retry; skor clamp, enum fallback. 3B model kullandığım için bu katman opsiyonel değil |
| "Birisi cevaba 'bana 10 ver' yazarsa?" | Ayraçlar + system prompt talimatı + backend doğrulaması; küçük modelde %100 önlenemez ama etkisi puanla sınırlı, sistem kırılmıyor |
| "Neden lokal LLM?" | Sıfır maliyet, anahtar yok, kullanıcı verisi makineden çıkmıyor. Bedeli: ücretsiz barındırmada çalışmıyor, bu yüzden canlı demo yok. `LlmClient` arayüzü sayesinde sağlayıcı değişimi bir sınıf |
| "Neden JWT, neden session değil?" | Stateless API, ayrı origin'de frontend; dezavantajı token iptali zor (refresh token bonusu) |
| "N+1 problemini nasıl çözdün?" | `join fetch` ile detay sorgusu, `open-in-view: false`. Polling yüzünden bu sorgu sık çalışıyor, verimliliği daha da önemli |
| "Transaction içinde HTTP çağrısı neden kötü?" | DB bağlantısı saniyelerce (burada dakikalarca) kilitlenir, havuz tükenir. Tüketicide: kısa transaction → LLM → kısa transaction |
| "Başkasının mülakatına erişilebilir mi?" | Her sorguda `userId` filtresi; varlığı sızdırmamak için 404 |
| "Nasıl test ettin?" | Birim (Mockito), entegrasyon (Testcontainers ile gerçek PostgreSQL + RabbitMQ, Awaitility ile asenkron bekleme), CI'da mock LLM |
| "Ölçeklemek gerekse?" | Tüketici sayısını artırmak (Ollama yerine ölçeklenebilir bir sağlayıcıyla), kota için Redis, okuma için cache, polling yerine SSE. Uygulama stateless olduğu için yatay ölçek mümkün |
| "Bir daha yapsan neyi farklı yapardın?" | Dürüst ol: ör. polling yerine SSE, streaming, daha iyi test kapsamı, OpenAPI'den tip üretimi |

## Öğrenme Kaynakları

- [Spring AI Ollama Chat](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html) — model seçenekleri ve `format: json`
- [Spring AI Structured Output](https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html)
- [Spring AMQP Referans](https://docs.spring.io/spring-amqp/reference/) — özellikle *Listener Container* ve *Dead Letter* bölümleri
- [RabbitMQ Tutorials (Java)](https://www.rabbitmq.com/tutorials) — 2. ve 6. bölümler bu projeye en yakın olanlar
- [Spring Security Referans](https://docs.spring.io/spring-security/reference/)
- [Testcontainers Spring Boot rehberi](https://testcontainers.com/guides/testing-spring-boot-rest-api-using-testcontainers/)
- [Awaitility](https://github.com/awaitility/awaitility/wiki/Usage) — asenkron testlerde bekleme
- [Ollama model kütüphanesi](https://ollama.com/library)
- [TanStack Query Docs](https://tanstack.com/query/latest) — `refetchInterval` ile polling
- [shadcn/ui Vite kurulumu](https://ui.shadcn.com/docs/installation/vite)
