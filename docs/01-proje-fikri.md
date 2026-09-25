# 1. Proje Fikri ve Kapsam

## Fikir: InterviewPilot

Junior geliştiricilerin (ve aslında herkesin) en çok zorlandığı konulardan biri teknik mülakatlar.
InterviewPilot, kullanıcının hedeflediği pozisyona göre **kişiselleştirilmiş mülakat simülasyonu** yapan bir web uygulaması:

1. Kullanıcı pozisyonu (ör. "Java Backend Developer"), seviyeyi (Junior/Mid/Senior), teknolojileri (ör. "Spring Boot, PostgreSQL, Docker") ve isteğe bağlı olarak iş ilanı metnini girer.
2. İstek anında bir kuyruğa düşer, API hemen cevap döner. Arka planda çalışan tüketici LLM'den 5–10 soru üretir (teknik, davranışsal, sistem tasarımı karışık).
3. Kullanıcı soruları tek tek cevaplar.
4. Her cevap LLM tarafından değerlendirilir: **0–10 puan, güçlü yönler, geliştirilmesi gerekenler, örnek ideal cevap**.
5. Mülakat bitince genel skor ve özet geri bildirim oluşturulur.
6. Panelde geçmiş mülakatlar, ortalama puan ve kategori bazlı gelişim görünür.

LLM **lokalde Ollama** ile çalışır: API anahtarı, kredi kartı, internet bağlantısı gerekmez. Lokal modeller yavaş olduğu için (bir soru seti 30–90 sn sürebilir) tüm LLM çağrıları **RabbitMQ** üzerinden asenkron işlenir; HTTP isteği beklemez.

## Neden bu fikir iyi bir portfolyo projesi?

| Kriter | Karşılığı |
|---|---|
| Gerçek bir problemi çözüyor | Mülakata hazırlık herkesin derdi, mülakatta anlatması kolay |
| LLM kullanımı "süs" değil, merkezde | Soru üretme + değerlendirme + özetleme = 3 farklı LLM kullanım senaryosu |
| Klasik backend becerilerini gösteriyor | Auth, ilişkisel veri modeli, CRUD, validasyon, test, migration |
| Asenkron mimari gösteriyor | RabbitMQ iş kuyruğu, dead letter queue, retry, durum makinesi — düz CRUD'un bir adım ötesi |
| Sıfır maliyet, sıfır hesap | Ollama + Docker; hiçbir bulut servisine kayıt olmaya gerek yok |
| Demo etmesi kolay | `docker compose up` + 2 komut; README'deki GIF akışın tamamını gösterir |

## Kullanıcı Hikâyeleri (MVP)

| ID | Hikâye | Kabul kriteri |
|---|---|---|
| US-1 | Kullanıcı olarak e-posta ve şifreyle kayıt olabilmeliyim | Aynı e-posta ikinci kez kaydedilemez, şifre BCrypt ile saklanır |
| US-2 | Kullanıcı olarak giriş yapıp JWT almalıyım | Hatalı giriş 401 döner, token 24 saat geçerli |
| US-3 | Yeni bir mülakat oturumu başlatabilmeliyim | Pozisyon, seviye, teknolojiler, soru sayısı (3–10), dil (TR/EN) girilir |
| US-4 | Oturum oluşunca sorular arka planda üretilmeli | API `202` ve `status: PENDING` döner; sorular hazır olunca `status: READY` olur, sorular sıralı/kategorili/zorluk seviyeli kaydedilir |
| US-5 | Her soruya cevap yazıp değerlendirme almalıyım | Cevap kaydedilir, değerlendirme asenkron gelir; puan 0–10, güçlü yönler, eksikler, ideal cevap gösterilir |
| US-6 | Mülakatı bitirince genel skor ve özet görmeliyim | Tüm sorular cevaplanıp değerlendirilmeden bitirilemez |
| US-7 | Geçmiş mülakatlarımı listeleyebilmeliyim | Sayfalı, en yeni üstte |
| US-8 | Panelde istatistiklerimi görmeliyim | Toplam mülakat, ortalama puan, kategori bazlı ortalama |
| US-9 | LLM işi başarısız olursa bunu görmeli ve tekrar deneyebilmeliyim | `status: FAILED` + hata mesajı, "Tekrar dene" ile iş yeniden kuyruğa girer |
| US-10 | Günlük LLM kullanım hakkım sınırlı olmalı | Limit aşılırsa 429 ve anlaşılır mesaj |

> US-10 notu: Lokal Ollama'nın parasal maliyeti yok, ama **aynı anda tek isteği** makul hızda işleyebiliyor. Kota + tek tüketicili kuyruk, bir kullanıcının makineyi kilitlemesini engelliyor. Yani limit yapay değil, gerçek bir kaynak sınırını koruyor.

## Bonus (zaman kalırsa, öncelik sırasıyla)

1. **Değerlendirmeyi streaming ile göstermek** (SSE, Spring AI `stream()`) — çok etkileyici görünür
2. **Oturumu PDF / Markdown olarak dışa aktarma**
3. **Takip sorusu**: LLM cevaba göre bir "follow-up" sorusu sorsun
4. **Model karşılaştırma**: aynı cevabı iki farklı Ollama modeline değerlendirtip sonuçları yan yana göstermek
5. **Refresh token** ve çıkışta token iptali
6. **Swagger UI** (springdoc-openapi) — API'yi canlı görebilmek
7. **Rol bazlı yetki** (ADMIN kullanıcı toplam LLM kullanımını ve kuyruk durumunu görebilsin)

## Kapsam Dışı (bilerek yapmıyoruz)

- **Bulut deploy.** Proje lokalde çalışır. Ollama ücretsiz barındırmada (512 MB RAM) çalışmadığı için canlı ortam yerine lokal kurulum + README'deki GIF tercih edildi. `LlmClient` arayüzü sayesinde ileride Groq'a geçip deploy etmek sadece bir implementasyon ve config değişikliği.
- Ödeme sistemi, e-posta doğrulama, şifre sıfırlama e-postası
- Mikroservis mimarisi (tek servis yeterli ve daha doğru; kuyruk varlığı mikroservis gerektirmez)
- Kendi modelini eğitmek / fine-tune

## Başarı Tanımı

- [ ] Lokalde kayıt → mülakat → değerlendirme → özet akışı uçtan uca çalışıyor
- [ ] Soru üretimi, cevap değerlendirme ve özet RabbitMQ üzerinden asenkron işleniyor
- [ ] LLM hatası DLQ'ya düşüyor, kullanıcı `FAILED` durumu görüp tekrar deneyebiliyor
- [ ] `main`'e push'ta CI yeşil (birim testleri + Testcontainers ile PostgreSQL ve RabbitMQ)
- [ ] README'de akışı gösteren GIF, mimari diyagram ve 3 komutluk kurulum var
