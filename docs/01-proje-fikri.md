# 1. Proje Fikri ve Kapsam

## Fikir: InterviewPilot

Junior geliştiricilerin (ve aslında herkesin) en çok zorlandığı konulardan biri teknik mülakatlar.
InterviewPilot, kullanıcının hedeflediği pozisyona göre **kişiselleştirilmiş mülakat simülasyonu** yapan bir web uygulaması:

1. Kullanıcı pozisyonu (ör. "Java Backend Developer"), seviyeyi (Junior/Mid/Senior), teknolojileri (ör. "Spring Boot, PostgreSQL, Docker") ve isteğe bağlı olarak iş ilanı metnini girer.
2. LLM bu bilgilere göre 5–10 soru üretir (teknik, davranışsal, sistem tasarımı karışık).
3. Kullanıcı soruları tek tek cevaplar.
4. Her cevap LLM tarafından değerlendirilir: **0–10 puan, güçlü yönler, geliştirilmesi gerekenler, örnek ideal cevap**.
5. Mülakat bitince genel skor ve özet geri bildirim oluşturulur.
6. Panelde geçmiş mülakatlar, ortalama puan ve kategori bazlı gelişim görünür.

## Neden bu fikir iyi bir portfolyo projesi?

| Kriter | Karşılığı |
|---|---|
| Gerçek bir problemi çözüyor | Mülakata hazırlık herkesin derdi, mülakatta anlatması kolay |
| LLM kullanımı "süs" değil, merkezde | Soru üretme + değerlendirme + özetleme = 3 farklı LLM kullanım senaryosu |
| Klasik backend becerilerini gösteriyor | Auth, ilişkisel veri modeli, CRUD, validasyon, test, migration |
| 2 günde bitirilebilir | MVP net sınırlı, bonuslar ayrı |
| Demo etmesi kolay | Mülakatçıya "şu linke gir, dene" diyebilirsin |
| Ücretsiz çalışır | Groq ücretsiz katman + Neon + Render + Vercel |

## Kullanıcı Hikâyeleri (MVP)

| ID | Hikâye | Kabul kriteri |
|---|---|---|
| US-1 | Kullanıcı olarak e-posta ve şifreyle kayıt olabilmeliyim | Aynı e-posta ikinci kez kaydedilemez, şifre BCrypt ile saklanır |
| US-2 | Kullanıcı olarak giriş yapıp JWT almalıyım | Hatalı giriş 401 döner, token 24 saat geçerli |
| US-3 | Yeni bir mülakat oturumu başlatabilmeliyim | Pozisyon, seviye, teknolojiler, soru sayısı (3–10), dil (TR/EN) girilir |
| US-4 | Oturum oluşunca sorular otomatik üretilmeli | Sorular sıralı, kategorili ve zorluk seviyeli kaydedilir |
| US-5 | Her soruya cevap yazıp değerlendirme almalıyım | Puan 0–10, güçlü yönler, eksikler, ideal cevap gösterilir |
| US-6 | Mülakatı bitirince genel skor ve özet görmeliyim | Tüm sorular cevaplanmadan bitirilemez (veya cevapsızlar 0 sayılır) |
| US-7 | Geçmiş mülakatlarımı listeleyebilmeliyim | Sayfalı, en yeni üstte |
| US-8 | Panelde istatistiklerimi görmeliyim | Toplam mülakat, ortalama puan, kategori bazlı ortalama |
| US-9 | Günlük LLM kullanım hakkım sınırlı olmalı | Limit aşılırsa 429 ve anlaşılır mesaj |

## Bonus (zaman kalırsa, öncelik sırasıyla)

1. **Değerlendirmeyi streaming ile göstermek** (SSE, Spring AI `stream()`) — çok etkileyici görünür
2. **Oturumu PDF / Markdown olarak dışa aktarma**
3. **Takip sorusu**: LLM cevaba göre bir "follow-up" sorusu sorsun
4. **Refresh token** ve çıkışta token iptali
5. **Swagger UI** (springdoc-openapi) — mülakatçılar API'yi canlı görebilir
6. **Rol bazlı yetki** (ADMIN kullanıcı toplam LLM kullanımını görebilsin)
7. **Sesli cevap** (Groq Whisper ücretsiz — ses → metin)

## Kapsam Dışı (bilerek yapmıyoruz)

- Ödeme sistemi, e-posta doğrulama, şifre sıfırlama e-postası
- Mikroservis mimarisi (tek servis yeterli ve daha doğru)
- Kendi modelini eğitmek / fine-tune

## Başarı Tanımı (2. günün sonunda)

- [ ] Canlı URL'de kayıt → mülakat → değerlendirme → özet akışı çalışıyor
- [ ] `main`'e push'ta testler koşuyor, geçerse otomatik deploy oluyor
- [ ] README'de canlı link, ekran görüntüleri/GIF, mimari diyagram var
- [ ] En az birkaç birim testi + 1 Testcontainers entegrasyon testi var
