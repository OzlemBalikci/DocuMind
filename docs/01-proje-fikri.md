# 1. Proje Fikri ve Kapsam

## Fikir: DocuMind — Şirket İçi Doküman Asistanı (RAG)

Her şirkette aynı problem var: bilgi PDF'lerde, Confluence export'larında, onboarding dokümanlarında dağınık duruyor. Yeni gelen "yıllık izin kaç gün?" veya "deploy süreci nasıl işliyor?" diye sorduğunda cevabı 40 sayfalık bir PDF'in içinde aramak zorunda.

DocuMind, yüklenen dokümanlar üzerinde **kaynak göstererek** soru cevaplayan bir asistan:

1. Kullanıcı doküman yükler (PDF, Markdown, düz metin).
2. Sistem dokümanı arka planda işler: metni çıkarır, anlamlı parçalara (chunk) böler, her parçayı embedding'e çevirip pgvector'a yazar.
3. Kullanıcı soru sorar.
4. Soru embedding'e çevrilir, vektör benzerlik aramasıyla en ilgili parçalar bulunur.
5. Bu parçalar bağlam olarak LLM'e verilir, LLM cevabı **sadece bu bağlamdan** üretir.
6. Cevabın altında hangi dokümanın hangi sayfasından geldiği gösterilir.

```text
Soru → Embedding → Vector Search (pgvector) → İlgili Chunk'lar → Ollama Prompt → Cevap + Kaynaklar
```

Tüm yığın lokalde çalışır: LLM ve embedding modeli **Ollama**, vektör deposu **pgvector**, doküman işleme kuyruğu **RabbitMQ**. API anahtarı, kredi kartı ve internet bağlantısı gerekmez — bu, gerçek şirket dokümanlarıyla çalışan bir araç için sadece bir maliyet avantajı değil, **veri gizliliği gereksinimi**.

## Neden bu fikir iyi bir portfolyo projesi?

| Kriter | Karşılığı |
|---|---|
| Sektörde en çok aranan LLM becerisi | RAG, 2024'ten beri kurumsal LLM projelerinin fiilî standardı. "LLM'e prompt attım" demekle "retrieval pipeline kurdum" demek arasında uçurum var |
| Klasik backend becerileri merkezde | Auth, dosya yükleme, ilişkisel veri modeli, migration, validasyon, test |
| Asenkron mimari zorunlu, süs değil | 40 sayfalık bir PDF ≈ 80 chunk ≈ 80 embedding çağrısı ≈ dakikalar. HTTP isteği bunu bekleyemez → RabbitMQ |
| Veritabanı derinliği | pgvector, HNSW indeksi, kosinüs benzerliği, kısmi indeks — ORM'in ötesinde SQL bilgisi |
| Halüsinasyonla mücadele somut | Kaynak gösterimi + benzerlik eşiği + "bilmiyorum" davranışı ölçülebilir kalite kararları |
| Sıfır maliyet, sıfır hesap | Ollama + Docker; veri makineden çıkmıyor |
| Demo etmesi kolay | Bir PDF yükle, soru sor, cevabın kaynağını göster — 20 saniyelik GIF anlatıyor |

## Kullanıcı Hikâyeleri (MVP)

| ID | Hikâye | Kabul kriteri |
|---|---|---|
| US-1 | E-posta ve şifreyle kayıt olabilmeliyim | Aynı e-posta ikinci kez kaydedilemez, şifre BCrypt ile saklanır |
| US-2 | Giriş yapıp JWT almalıyım | Hatalı giriş 401 döner, token 24 saat geçerli |
| US-3 | Doküman yükleyebilmeliyim | PDF / MD / TXT, max 20 MB; API `202` ve `status: PENDING` döner |
| US-4 | Dokümanın işlenme durumunu görmeliyim | `PENDING → PARSING → EMBEDDING → READY`; chunk sayısı ve sayfa sayısı görünür |
| US-5 | Dokümanlarımı listeleyip silebilmeliyim | Sayfalı liste; silme chunk'ları ve diskteki dosyayı da temizler |
| US-6 | Doküman içeriği hakkında soru sorabilmeliyim | Cevap sadece yüklediğim dokümanlara dayanır |
| US-7 | Cevabın kaynağını görmeliyim | Her cevapta kullanılan chunk'lar, doküman adı, sayfa no ve benzerlik skoru |
| US-8 | Bağlamda cevap yoksa uydurulmamalı | Benzerlik eşiğinin altındaysa "Dokümanlarında bu bilgiyi bulamadım" döner |
| US-9 | Sohbet geçmişimi görebilmeliyim | Konuşma başlıkları listelenir, mesajlar sırayla görünür |
| US-10 | Başkasının dokümanına erişemememeliyim | Her sorgu `user_id` ile filtrelenir; vektör aramasında da aynı filtre |
| US-11 | İşlem başarısız olursa görüp tekrar deneyebilmeliyim | `status: FAILED` + hata mesajı, "Tekrar dene" işi yeniden kuyruğa alır |
| US-12 | Günlük kullanım hakkım sınırlı olmalı | Limit aşılırsa 429 ve anlaşılır mesaj |

> US-10 sadece bir yetki kontrolü değil, **RAG'e özgü bir güvenlik meselesi.** Vektör aramasını kullanıcıya göre filtrelemeyi unutursan, benzerlik araması başkasının dokümanından bir parçayı getirir ve LLM onu cevaba yazar. Klasik CRUD'da böyle bir sızıntı yolu yok; mülakatta anlatmaya değer.

## Bonus (zaman kalırsa, öncelik sırasıyla)

1. **Streaming cevap** (SSE, Spring AI `stream()`) — lokal modelin yavaşlığını avantaja çevirir
2. **Hibrit arama**: pgvector + PostgreSQL full-text (`tsvector`) skorlarını birleştirmek. Özel isim ve kod parçası aramalarında saf vektör aramasından belirgin daha iyi
3. **Re-ranking**: top-20 chunk'ı çekip küçük bir modelle yeniden sıralamak
4. **Konuşma bağlamı**: önceki soruları dikkate alan soru yeniden yazımı (query rewriting)
5. **Doküman kapsamı seçimi**: "sadece şu 2 dokümanda ara"
6. **Chunk önizleme**: kaynağa tıklayınca ilgili metni vurgulu göstermek
7. **Swagger UI** (springdoc-openapi)
8. **Model karşılaştırma**: aynı soruyu iki farklı chat modeline sordurup cevapları yan yana göstermek

## Kapsam Dışı (bilerek yapmıyoruz)

- **Bulut deploy.** Ollama ücretsiz barındırmada (512 MB RAM) çalışmaz. `LlmClient` / `EmbeddingClient` arayüzleri sayesinde ileride Groq veya OpenAI'ye geçip deploy etmek bir implementasyon + config değişikliği.
- **Gerçek zamanlı doküman senkronizasyonu** (Confluence/Drive webhook'ları) — elle yükleme yeterli
- **OCR** (taranmış PDF'lerden görüntü tanıma) — metin katmanı olan PDF'lerle sınırlıyız
- **Çok kullanıcılı workspace / paylaşım** — doküman sahibi tek kullanıcı
- Mikroservis mimarisi (tek servis yeterli; kuyruk varlığı mikroservis gerektirmez)
- Kendi modelini eğitmek / fine-tune (RAG'in tüm amacı bundan kaçınmak)

## Başarı Tanımı

- [ ] PDF yükle → arka planda işlensin → `READY` olsun
- [ ] Soru sor → cevap **yüklediğin dokümandan** gelsin, kaynak gösterilsin
- [ ] Dokümanda olmayan bir şey sorulunca uydurmasın, "bulamadım" desin
- [ ] Doküman işleme ve cevap üretimi RabbitMQ üzerinden asenkron
- [ ] Hata DLQ'ya düşsün, kullanıcı `FAILED` görüp tekrar deneyebilsin
- [ ] `main`'e push'ta CI yeşil (Testcontainers ile gerçek pgvector + RabbitMQ)
- [ ] README'de akışı gösteren GIF, mimari diyagram ve 3 komutluk kurulum
