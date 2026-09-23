# 7. Deploy

Hepsi ücretsiz, hiçbirinde kredi kartı gerekmez:

| Katman | Servis | Ücretsiz plan | Dikkat |
|---|---|---|---|
| Veritabanı | [Neon](https://neon.com) | 0,5 GB depolama, ayda 100 CU-saat | 5 dk boşta kalınca uykuya geçer, ilk sorguda milisaniyeler içinde uyanır |
| Backend | [Render](https://render.com) Web Service (Docker) | 0,1 CPU / 512 MB RAM | 15 dk istek gelmezse uyur, uyanması ~1 dk |
| Frontend | [Vercel](https://vercel.com) Hobby | Statik site + CDN | Ticari kullanım yok (portfolyo için sorun değil) |
| LLM | [Groq](https://console.groq.com) | Model bazlı günlük limit | Bkz. [LLM dokümanı](05-llm-entegrasyonu.md) |

**Sıra önemli:** Neon → Render → Vercel → Render'da CORS'u güncelle → CI secret'larını ekle.

Üç servisin de bölgesini **Frankfurt / eu-central-1** seç. Backend ile DB aynı bölgede olunca sorgu gecikmesi düşük olur, Türkiye'ye de yakındır.

## 0. Lokal Ortam

Lokal PostgreSQL için repo kökünde `docker-compose.yml` koy:
- Servis: `postgres:16-alpine`
- Port: `${POSTGRES_PORT}:5432` (varsayılan `POSTGRES_PORT=5432`; çakışma varsa `.env`'de değiştir)
- Kullanıcı/şifre/veritabanı adı **`.env` dosyasından** okunur (`POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`) — `docker-compose.yml` içine şifre yazma, GitHub'a güvenle push edilir
- Verinin kaybolmaması için isimli volume

Repo köküne `.env.example` koy (değerler boş/örnek), gerçek `.env` dosyasını `.gitignore`'a ekle:

| Değişken | Lokal örnek | Açıklama |
|---|---|---|
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` / `POSTGRES_PORT` | Docker Compose için |
| `DB_URL` | `jdbc:postgresql://localhost:5432/interviewpilot_db` | Spring JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `interviewpilot_app` / `.env`'deki şifre | Spring ile POSTGRES_* aynı olmalı |
| `JWT_SECRET` | `openssl rand -base64 48` çıktısı | En az 32 karakter |
| `GROQ_API_KEY` | `gsk_...` | Boşsa `LLM_PROVIDER=mock` kullan |
| `LLM_PROVIDER` | `groq` veya `mock` | |
| `LLM_MODEL` | `llama-3.3-70b-versatile` | |
| `LLM_DAILY_LIMIT` | `15` | Kullanıcı başına günlük çağrı |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:5199` | Virgülle birden fazla verilebilir |
| `VITE_API_URL` | `http://localhost:8087` | Sadece frontend (`frontend/.env.local`) |

Portlar: backend **8087**, frontend **5199** (`vite.config.ts` → `server.port`), DB **5432** (`.env` → `POSTGRES_PORT`).

## 1. Neon (PostgreSQL)

1. <https://console.neon.tech> → GitHub ile giriş → **New Project**
2. Ad: `interviewpilot`, Postgres sürümü: 16 veya 17, Region: **AWS Europe Central (Frankfurt)**
3. Dashboard → **Connect** → bağlantı bilgilerini al. Neon şöyle bir URL verir:
   `postgresql://<kullanici>:<sifre>@ep-xxx-123456.eu-central-1.aws.neon.tech/neondb?sslmode=require`
4. Bunu Spring için **JDBC formatına** çevir ve kullanıcı/şifreyi ayrı değişkene al:

| Değişken | Değer |
|---|---|
| `DB_URL` | `jdbc:postgresql://ep-xxx-123456.eu-central-1.aws.neon.tech/neondb?sslmode=require` |
| `DB_USERNAME` | `<kullanici>` |
| `DB_PASSWORD` | `<sifre>` |

- **Pooled** (host'unda `-pooler` geçen) değil **direct** bağlantıyı kullan. Flyway ve Hibernate'in hazırlanmış sorguları PgBouncer'ın transaction modunda sorun çıkarabilir; Hikari zaten kendi havuzunu tutuyor.
- `maximum-pool-size: 5` yeterli.
- Tabloları elle oluşturma: backend ilk açıldığında Flyway `V1__init_schema.sql`'i çalıştırır. Neon'un **SQL Editor**'ünden `flyway_schema_history` tablosuna bakarak doğrulayabilirsin.
- Bonus: Neon **branch** özelliğiyle `dev` branch'i açıp lokal geliştirmede de bulut DB'yi kullanabilirsin.

## 2. Render (Backend)

1. <https://dashboard.render.com> → GitHub ile giriş → **New → Web Service** → repoyu seç
2. Ayarlar:

| Alan | Değer |
|---|---|
| Name | `interviewpilot-api` (URL: `https://interviewpilot-api.onrender.com`) |
| Region | Frankfurt (EU Central) |
| Branch | `main` |
| Root Directory | `backend` |
| Runtime / Language | **Docker** (Dockerfile'ı `backend/` altında bulur) |
| Instance Type | **Free** |
| Health Check Path | `/actuator/health` |
| Auto-Deploy | **Off** (deploy'u GitHub Actions deploy hook ile tetikleyecek) |

3. **Environment** sekmesi:

| Değişken | Değer |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Neon'dan |
| `JWT_SECRET` | Yeni üret, lokaldekinden **farklı** olsun |
| `GROQ_API_KEY` | Groq konsolundan |
| `LLM_PROVIDER` | `groq` |
| `LLM_MODEL` | `llama-3.3-70b-versatile` |
| `LLM_DAILY_LIMIT` | `15` |
| `APP_CORS_ALLOWED_ORIGINS` | Şimdilik boş bırak, Vercel'den sonra dolduracaksın |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `JAVA_TOOL_OPTIONS` | `-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Xss512k` |

`PORT` değişkenini Render kendisi verir, ekleme.

4. **Settings → Deploy Hook** URL'ini kopyala → GitHub secret `RENDER_DEPLOY_HOOK_URL` olarak ekle. Bu URL'i bilen herkes deploy tetikleyebilir, gizli tut.
5. İlk deploy'u elle yap (**Manual Deploy → Deploy latest commit**). Loglarda Flyway'in migration'ı uyguladığını ve `Started InterviewPilotApplication` satırını gör.
6. Kontrol: tarayıcıda `https://interviewpilot-api.onrender.com/actuator/health` → `{"status":"UP"}`

Alternatif: Deploy hook yerine Auto-Deploy'u **"After CI Checks Pass"** yaparsan Render, GitHub'daki check'ler yeşil olunca kendisi deploy eder. Daha az ayar gerektirir ama deploy adımı pipeline'da görünmez; bu projede pipeline'ı göstermek istediğimiz için hook'u tercih ediyoruz.

### Soğuk açılış (cold start) problemi

Ücretsiz planda servis 15 dk sonra uyur; ilk istek ~1 dk bekler. Bir işe alımcı linke tıklayıp boş ekran görürse kötü izlenim bırakır. Çözümler:

1. **Frontend'de ele al (mutlaka yap):** Uygulama açılınca arka planda `/actuator/health`'e istek at. 3 sn içinde cevap gelmezse üstte bir bant göster: "Sunucu uyanıyor, bu ücretsiz barındırmada ~1 dakika sürebilir." Cevap gelince bandı kaldır.
2. **Uyanık tutmak (opsiyonel):** [UptimeRobot](https://uptimerobot.com) (ücretsiz) ile her 10 dk'da bir `/actuator/health`'e istek at. Tek servis için ayda 750 ücretsiz saat yetiyor. Actuator health isteği DB'ye de dokunduğu için Neon'u da uyanık tutar ve CU-saat harcar; ayda 100 CU-saat sınırına dikkat et (gerekirse sadece gündüz saatlerinde ping at).
3. README'ye not: "Ücretsiz barındırma nedeniyle ilk açılış ~1 dk sürebilir."

## 3. Vercel (Frontend)

1. <https://vercel.com> → GitHub ile giriş → **Add New → Project** → repoyu seç
2. Ayarlar:

| Alan | Değer |
|---|---|
| Framework Preset | Vite |
| Root Directory | `frontend` |
| Build Command | `npm run build` |
| Output Directory | `dist` |
| Environment Variable | `VITE_API_URL` = `https://interviewpilot-api.onrender.com` |

3. **SPA yönlendirmesi:** React Router kullandığın için `/interviews/42` gibi bir sayfayı yenileyince Vercel 404 verir. `frontend/vercel.json` içine tüm yolları `/index.html`'e yönlendiren bir `rewrites` kuralı ekle.
4. CI/CD Seçenek A'yı kullanıyorsan aynı `vercel.json`'da `git.deploymentEnabled: false` yap ([CI/CD dokümanı](06-ci-cd.md#job-frontend-deploy-sadece-main-needs-frontend-test)).
5. Deploy bitince URL'i al (ör. `https://interviewpilot.vercel.app`).

## 4. CORS'u Bağla

Render → Environment → `APP_CORS_ALLOWED_ORIGINS=https://interviewpilot.vercel.app` → kaydet (servis yeniden başlar).

- Sonunda `/` olmasın; origin eşleşmesi birebir yapılır.
- Vercel preview URL'lerine de izin vermek istersen Spring'in `allowedOriginPatterns` özelliğini kullan (ör. `https://interviewpilot-*.vercel.app`).

## 5. Son Kontroller

- [ ] `https://<api>/actuator/health` → `UP`
- [ ] Vercel URL'inde kayıt ol → giriş yap
- [ ] Mülakat oluştur (sorular geliyor mu?) → cevapla → tamamla
- [ ] Sayfayı `/interviews/<id>` üzerindeyken yenile → 404 yok
- [ ] Tarayıcı konsolunda CORS hatası yok
- [ ] Render loglarında hata / stack trace yok
- [ ] Neon'da tablolar ve veriler oluşmuş
- [ ] Küçük bir değişiklik push'la → Actions yeşil → Render ve Vercel otomatik güncellendi
- [ ] README'ye canlı link, demo kullanıcı bilgisi (opsiyonel) ve ekran görüntüsü eklendi

## Sorun Giderme

| Belirti | Olası sebep | Çözüm |
|---|---|---|
| Render build'i "out of memory" | Maven build 512 MB'a sığmıyor | Build'i GitHub Actions'ta yapıp GHCR imajını Render'a "Existing Image" olarak ver |
| Servis açılıp kapanıyor (restart loop) | JVM bellek aşımı | `JAVA_TOOL_OPTIONS` değerlerini kontrol et |
| `Connection refused` / `SSL required` | JDBC URL yanlış | `jdbc:` öneki ve `?sslmode=require` var mı? |
| `Schema validation: missing table` | Flyway çalışmadı | Migration dosyası `db/migration` altında mı, adı `V1__` (iki alt çizgi) ile mi başlıyor? |
| Frontend'de `Network Error`, backend logunda istek yok | CORS veya yanlış `VITE_API_URL` | Tarayıcı Network sekmesine bak; Vercel'de env değiştirdikten sonra **yeniden deploy** et (Vite değişkenleri build anında gömülür) |
| LLM çağrıları `503` | Groq anahtarı yanlış / limit doldu | Render loglarında Groq cevabına bak, `console.groq.com` → Usage |
| Giriş sonrası her istek 401 | `JWT_SECRET` deploy'lar arasında değişti ya da saat/expiry hatası | Secret'ı sabit tut, token süresini kontrol et |
