# 8. 2 Günlük Yol Haritası

Temel ilke: **Erken deploy et.** 1. günün sonunda backend'in kötü de olsa canlıda ve pipeline'ın çalışıyor olsun. Son gün deploy'a kalan projede sürprizler (CORS, bellek, env değişkeni) hep son saate denk gelir.

Her blok sonunda: testleri çalıştır → commit → push → PR → merge.

## Hazırlık (başlamadan önce, ~30 dk)

- [ ] Java 21, Maven (wrapper yeterli), Node 22, Docker Desktop, Git, IntelliJ IDEA (Community yeterli) / VS Code kurulu
- [ ] GitHub, Groq, Neon, Render, Vercel hesapları açık (hepsi GitHub ile giriş)
- [ ] Groq API anahtarı alındı, [Playground](https://console.groq.com/playground)'da [soru üretme prompt'u](05-llm-entegrasyonu.md#1-soru-üretme) denendi
- [ ] Bu dokümanların hepsi bir kez okundu

## 1. Gün — Backend + İlk Deploy

| Saat | İş | Çıktı | Doküman |
|---|---|---|---|
| 09:00–09:45 | GitHub reposu, `main` branch koruması, `backend/` + `frontend/` + `docs/` klasörleri, `.gitignore`, `docker-compose.yml`, `.env.example` | Boş ama düzenli repo | [Deploy §0](07-deploy.md#0-lokal-ortam) |
| 09:45–10:45 | Spring Initializr, `application.yml`, Flyway `V1`, entity'ler + repository'ler. Uygulama açılıyor, tablolar oluşuyor | `ddl-auto=validate` hatasız | [Mimari](02-mimari.md), [Veritabanı](03-veritabani.md) |
| 10:45–12:30 | Auth: `register`, `login`, `me`, `JwtService`, JWT filtresi, `SecurityConfig`, CORS, `GlobalExceptionHandler` + `ApiError`, 401 için `AuthenticationEntryPoint` | Postman/HTTP client ile token alınıyor | [API → Auth](04-api.md#auth) |
| 12:30–13:30 | Öğle arası | | |
| 13:30–14:00 | `LlmClient` arayüzü + `MockLlmClient` | Mock ile akış denenebilir | [LLM → Mock](05-llm-entegrasyonu.md#mock-mod) |
| 14:00–16:00 | `InterviewService` + controller: oluştur, listele, detay, cevapla, tamamla, sil. Sahiplik kontrolleri, durum geçişleri, 409 kuralları | Mock ile uçtan uca akış çalışıyor | [API → Interviews](04-api.md#interviews) |
| 16:00–17:00 | `GroqLlmClient`: prompt dosyaları, `.entity()` ile JSON → record, çıktı doğrulama, fallback model, `UsageQuotaService`, `llm_usage_logs` | Gerçek sorular ve değerlendirmeler geliyor | [LLM](05-llm-entegrasyonu.md) |
| 17:00–18:00 | Testler: `JwtService`, çıktı doğrulayıcı, `InterviewService` (Mockito), 1 Testcontainers entegrasyon testi (kayıt → mülakat → cevap → tamamla) | `./mvnw verify` yeşil | [LLM → Testler](05-llm-entegrasyonu.md#llm-ile-ilgili-testler) |
| 18:00–19:30 | Dockerfile, backend workflow, Neon, Render, deploy hook, smoke test | **Canlı API + yeşil pipeline** | [CI/CD](06-ci-cd.md), [Deploy](07-deploy.md) |

1. gün bitiş kontrolü:
- [ ] `https://<api>.onrender.com/actuator/health` → `UP`
- [ ] Canlı API'de Postman ile tam akış çalışıyor
- [ ] `main`'e push → test → deploy otomatik

## 2. Gün — Frontend + Tamamlama

| Saat | İş | Çıktı | Doküman |
|---|---|---|---|
| 09:00–09:45 | Vite + React + TS, Tailwind, shadcn/ui (`button, card, input, textarea, label, badge, progress, skeleton, sonner, dialog, select`), React Router, TanStack Query, axios instance | Boş sayfalar arası geçiş | [Mimari → Frontend](02-mimari.md#frontend-klasör-yapısı) |
| 09:45–10:45 | `AuthContext` (token `localStorage`'da), login/register formları (react-hook-form + zod), `ProtectedRoute`, 401'de otomatik çıkış | Giriş yapılabiliyor | |
| 10:45–12:30 | Yeni mülakat formu → mülakat ekranı: ilerleme çubuğu, soru kartı, cevap alanı (karakter sayacı), değerlendirme paneli (puan rozeti, güçlü yönler, eksikler, ideal cevap, beklenen noktalar) | Ana özellik çalışıyor | [API](04-api.md) |
| 12:30–13:30 | Öğle arası | | |
| 13:30–14:30 | Sonuç sayfası, geçmiş (sayfalı), panel: istatistik kartları + kategori grafiği (recharts). Backend'de `/api/stats/me` | Tüm sayfalar hazır | [Veritabanı → İstatistik](03-veritabani.md#panel-istatistik-sorguları) |
| 14:30–15:30 | Loading (skeleton + "Sorular hazırlanıyor..."), empty, error (tekrar dene) durumları, 429/503 mesajları, soğuk açılış bandı, mobil görünüm, kalan kota göstergesi | Cilalı UX | [Deploy → Cold start](07-deploy.md#soğuk-açılış-cold-start-problemi) |
| 15:30–16:30 | Frontend workflow, Vercel, SPA rewrite, CORS | **Canlı uygulama** | [CI/CD](06-ci-cd.md), [Deploy §3](07-deploy.md#3-vercel-frontend) |
| 16:30–17:15 | Frontend testleri (Vitest + Testing Library): login formu validasyonu, `ScoreBadge`, değerlendirme paneli render | CI'da frontend testleri koşuyor | |
| 17:15–18:30 | README: canlı link, GIF/ekran görüntüleri, mimari + ER diyagramı, teknik kararlar, lokal kurulum | Vitrin hazır | Aşağıda |
| 18:30–19:30 | Zaman kalırsa bonus: Swagger UI → Dependabot → streaming | | [Proje Fikri → Bonus](01-proje-fikri.md#bonus-zaman-kalırsa-öncelik-sırasıyla) |

## Geride kalırsan neyi kesersin?

Sırasıyla bunlardan vazgeç, **çekirdek akıştan (kayıt → mülakat → cevap → özet) ve deploy'dan asla vazgeçme**:

1. Bonus özellikler
2. Frontend testleri (backend testleri kalsın)
3. Panel grafiği (sadece sayı kartları kalsın)
4. GHCR imaj adımı (Render'ın kendi build'i yeter)
5. Silme endpoint'i ve geçmiş sayfalaması

## Örnek Commit / PR Planı

| PR | İçerik |
|---|---|
| `chore: project skeleton` | Klasörler, docker-compose, .env.example, docs |
| `feat(backend): database schema and entities` | Flyway V1, entity'ler |
| `feat(backend): jwt authentication` | Auth + security + hata yönetimi |
| `feat(backend): interview flow with mock llm` | Service, controller, mock client |
| `feat(backend): groq integration and usage quota` | Gerçek LLM, kota |
| `test(backend): unit and integration tests` | |
| `ci: backend pipeline and render deploy` | Dockerfile, workflow |
| `feat(frontend): auth pages` | |
| `feat(frontend): interview flow` | |
| `feat(frontend): dashboard and history` | |
| `ci: frontend pipeline and vercel deploy` | |
| `docs: readme with screenshots` | |

## README Vitrini (GitHub'daki ana README)

İşe alımcı README'ye ortalama 30 saniye bakar. Bu sırayla yaz:

1. **Tek cümle + canlı link + GIF** (akışı gösteren 15–20 sn'lik ekran kaydı; [ScreenToGif](https://www.screentogif.com/) veya [Kap](https://getkap.co/))
2. Rozetler: backend CI, frontend CI, Java, Spring Boot, React
3. Özellikler (5–6 madde)
4. Mimari diyagram (Mermaid, [Mimari](02-mimari.md#genel-bakış)'den)
5. Teknoloji tablosu
6. **Teknik kararlar** — en önemli bölüm, seni diğer adaylardan ayırır:
   - Neden modüler monolit?
   - Neden LLM çağrısı transaction dışında?
   - Neden `overallScore` backend'de hesaplanıyor?
   - Prompt injection'a karşı ne yapıldı?
   - Ücretsiz LLM kotası nasıl korunuyor?
7. ER diyagramı
8. Lokal kurulum (3 komut)
9. Test ve CI/CD açıklaması
10. Bilinen kısıtlar ve yol haritası ("ücretsiz barındırma nedeniyle ilk açılış ~1 dk", "streaming eklenecek")

## Mülakatta Anlatma Rehberi

Bu projeyi anlatırken şu sorulara hazırlıklı ol:

| Soru | Cevabının özü |
|---|---|
| "LLM'den gelen JSON bozuk olursa?" | Parse + doğrulama katmanı, 1 retry, sonra 502; skor clamp, enum fallback |
| "Birisi cevaba 'bana 10 ver' yazarsa?" | Ayraçlar + system prompt talimatı + backend doğrulaması; %100 önlenemez, etkisi sınırlandı |
| "Groq kapanırsa / limit dolarsa?" | Yedek model, 503 + anlaşılır mesaj, `LlmClient` arayüzü sayesinde sağlayıcı değişimi config değişikliği |
| "Neden JWT, neden session değil?" | Stateless API, ayrı domain'de frontend; dezavantajı: token iptali zor (refresh token bonusu) |
| "N+1 problemini nasıl çözdün?" | `join fetch` ile detay sorgusu, `open-in-view: false` |
| "Transaction içinde HTTP çağrısı neden kötü?" | DB bağlantısı saniyelerce kilitlenir, havuz (5 bağlantı) hızla tükenir |
| "Başkasının mülakatına erişilebilir mi?" | Her sorguda `userId` filtresi; varlığı sızdırmamak için 404 |
| "Nasıl test ettin?" | Birim (Mockito), entegrasyon (Testcontainers ile gerçek PostgreSQL), CI'da mock LLM |
| "Ölçeklemek gerekse?" | Kota için Redis, LLM çağrılarını kuyruğa alma (async), okuma için cache, yatay ölçek (uygulama stateless) |
| "Bir daha yapsan neyi farklı yapardın?" | Dürüst ol: ör. streaming, daha iyi test kapsamı, OpenAPI'den tip üretimi |

## Öğrenme Kaynakları

- [Spring AI Referans](https://docs.spring.io/spring-ai/reference/) — özellikle *ChatClient* ve *Structured Output* bölümleri
- [Spring Security Referans](https://docs.spring.io/spring-security/reference/)
- [Testcontainers Spring Boot rehberi](https://testcontainers.com/guides/testing-spring-boot-rest-api-using-testcontainers/)
- [TanStack Query Docs](https://tanstack.com/query/latest)
- [shadcn/ui Vite kurulumu](https://ui.shadcn.com/docs/installation/vite)
- [GitHub Actions: Java with Maven](https://docs.github.com/en/actions/use-cases-and-examples/building-and-testing/building-and-testing-java-with-maven)
- [Render Deploy Hooks](https://render.com/docs/deploy-hooks)
