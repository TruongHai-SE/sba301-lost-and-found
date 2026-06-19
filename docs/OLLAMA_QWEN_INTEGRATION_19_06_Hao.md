# Tích hợp Ollama + Qwen-VL cho AI Image Enrichment

> **Tác giả:** Hao | **Ngày:** 19/06/2026 | **Trạng thái:** ✅ Build SUCCESS (82 source files compile)
> **Mục tiêu:** Tự động sinh mô tả chi tiết + tags cho ảnh đăng tin bằng vision-language model chạy local, không phụ thuộc cloud API.

---

## 1. Tổng quan

### Vấn đề cũ
- User upload ảnh + viết mô tả ngắn gọn (hoặc không viết gì).
- CLIP chỉ embed ảnh để matching, **không hiểu nội dung** ảnh là gì.
- Matching quality thấp khi user mô tả sơ sài.

### Giải pháp mới
Thêm 1 layer **AI Vision** chạy nền:
- Ảnh được gửi sang **Ollama** (chạy local) với model **Qwen2.5-VL**.
- Model trả về: `description` (mô tả chi tiết bằng tiếng Việt) + `tags` (5-8 từ khóa).
- Lưu vào 3 cột mới: `ai_description`, `ai_tags`, `ai_enriched_at`.
- Dùng để bổ sung cho `description` của user → tăng độ chính xác khi matching.

### Tại sao chọn Ollama + Qwen-VL?
| Tiêu chí | OpenAI Vision API | Ollama + Qwen-VL |
|----------|-------------------|------------------|
| Chi phí | Tính theo request | **Miễn phí** (chạy local) |
| Data privacy | Ảnh gửi lên cloud | **Ảnh ở local** |
| Latency | ~2-5s + network | **5-30s** (tùy GPU) |
| Tiếng Việt | Tốt | **Tốt** (Qwen trained multilingual) |
| Offline | ❌ | **✅** |

---

## 2. Kiến trúc tổng thể

```
┌─────────────────────────────────────────────────────────────────────┐
│                         USER (FE)                                   │
└────────────────────────────────┬────────────────────────────────────┘
                                 │ POST /api/v1/posts (multipart: ảnh + form)
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│              PostController (Spring MVC)                            │
└────────────────────────────────┬────────────────────────────────────┘
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│              PostServiceImpl                                        │
│                                                                     │
│  createLostPost() / createFoundPost()                                │
│       │                                                             │
│       ├── 1. Save Post vào DB (PostgreSQL)                          │
│       │      ↳ INSERT vào bảng posts                                 │
│       │                                                             │
│       ├── 2. runClipMatching()                                       │
│       │      ↳ Gọi CLIP service (sync, ~200ms)                       │
│       │      ↳ Trả về List<ClipMatch> cho FE                         │
│       │                                                             │
│       └── 3. triggerAiEnrichment()                                   │
│              ↳ @Async fire-and-forget                                 │
└────────────────┬────────────────────────────────────┬───────────────┘
                 │ (sync)                            │ (async, background)
                 ▼                                   ▼
        ┌────────────────────┐           ┌────────────────────────────┐
        │  CLIP Service      │           │  PostAiEnrichmentService   │
        │  (Python/FastAPI)  │           │  @Async("aiAnalysisExecutor")│
        │  port 8000         │           │  Thread pool: core=2, max=4│
        └─────────┬──────────┘           └─────────────┬──────────────┘
                  │                                    │
                  ▼                                    ▼
        ┌────────────────────┐           ┌────────────────────────────┐
        │  Vector Store      │           │  ImageAnalysisService      │
        │  (pgvector)        │           │  ↓ gọi OllamaClient        │
        └────────────────────┘           └─────────────┬──────────────┘
                                                      │
                                                      ▼
                                          ┌────────────────────────────┐
                                          │  OllamaClient (RestClient) │
                                          │  POST /api/generate        │
                                          │  timeout 60s               │
                                          └─────────────┬──────────────┘
                                                        │
                                                        ▼
                                          ┌────────────────────────────┐
                                          │  Ollama (Docker)           │
                                          │  qwen2.5vl (7B)            │
                                          │  port 11434                │
                                          │  GPU optional              │
                                          └─────────────┬──────────────┘
                                                        │
                                                        │ JSON: {description, tags}
                                                        ▼
                                          ┌────────────────────────────┐
                                          │  ImageAnalysisServiceImpl  │
                                          │  Parse JSON, validate      │
                                          │  → Optional<OllamaTags>    │
                                          └─────────────┬──────────────┘
                                                        │ result.isPresent()
                                                        ▼
                                          ┌────────────────────────────┐
                                          │  TransactionTemplate       │
                                          │  UPDATE posts SET          │
                                          │    ai_description = ?      │
                                          │    ai_tags = ?             │
                                          │    ai_enriched_at = NOW()  │
                                          │  WHERE id = ?              │
                                          └────────────────────────────┘
```

---

## 3. Luồng chi tiết theo timeline

```
T+0ms      User bấm "Đăng tin" trên FE
           │
T+50ms     FE → POST /api/v1/posts (multipart/form-data)
           │     fields: title, description, image (file), location, type=LOST|FOUND
           │
T+80ms     PostController nhận request, validate
           │
T+120ms    PostServiceImpl.createLostPost() / createFoundPost()
           │  ├── UserRepository.findById(userId)
           │  ├── LocationRepository.save(Location) [nếu có location]
           │  ├── Cloudinary.upload(image) → ImageRepository.save
           │  └── PostRepository.save(Post) ← 4 fields: title, desc, type, status
           │
T+250ms    ClipClient.embedImage() hoặc embedText()
           │  └── CLIP service trả List<ClipMatch>
           │
T+450ms    Response trả FE: { post, matches: [...] }   ← FE render ngay
           │
           ╔══════════════════════════════════════════════════════════╗
           ║  Từ đây: MAIN THREAD ĐÃ XONG, USER ĐÃ THẤY KẾT QUẢ    ║
           ╚══════════════════════════════════════════════════════════╝
           │
T+455ms    [BACKGROUND] triggerAiEnrichment() → PostAiEnrichmentService.enrichAsync()
           │  Spring chuyển sang thread pool "aiAnalysisExecutor"
           │
T+460ms    ImageAnalysisService.analyzeImage(imageUrl, userDescription)
           │  │
           │  ├── Build prompt cho Qwen-VL:
           │  │     "Bạn là trợ lý phân tích ảnh đồ vật.
           │  │      Mô tả chi tiết đồ vật trong ảnh bằng tiếng Việt.
           │  │      Trả về JSON: {description: string, tags: [string]}"
           │  │
           │  ├── OllamaClient.generate(base64Image, prompt)
           │  │     POST http://ollama:11434/api/generate
           │  │     body: { model: "qwen2.5vl", prompt, images: [base64], stream: false }
           │  │     timeout: 60s
           │  │
           │  └── Parse response: { response: "...JSON string..." }
           │
T+5-30s    Qwen-VL xử lý (tùy GPU/CPU)
           │
T+30s      Nhận response, parse JSON, validate
           │  ├── Thành công → Optional.of(OllamaTags(description, tags))
           │  └── Lỗi parse → log warn, return Optional.empty()
           │
T+30.1s    transactionTemplate.executeWithoutResult(status -> applyToPost(...))
           │  │
           │  ├── postRepository.findById(postId)
           │  ├── post.setAiDescription(tags.description())
           │  ├── post.setAiTags(String.join(",", tags.tags()))
           │  ├── post.setAiEnrichedAt(LocalDateTime.now())
           │  ├── Logic ghép description:
           │  │   ├── User desc < 30 chars → replace description
           │  │   └── User desc ≥ 30 chars → append "[AI mô tả thêm] ..."
           │  └── postRepository.save(post) ← COMMIT
           │
T+30.2s    Log: "Saved AI enrichment for post {id}"
           │
T+30.3s    Future completes, thread về pool
```

---

## 4. Tại sao thiết kế như vậy?

### 4.1. Tại sao PHẢI async?
- Qwen-VL inference: **5-30 giây** (chậm hơn CLIP ~100 lần).
- Nếu sync → user phải đợi 30s mới thấy response → UX rất tệ.
- Async = main thread trả response ngay, AI chạy nền, user không biết.

### 4.2. Tại sao tách `PostAiEnrichmentService` riêng?
**Vấn đề Spring AOP "self-invocation":**
```java
// ❌ KHÔNG HOẠT ĐỘNG
@Service
public class PostServiceImpl {
    @Async
    public void createPost() {
        // Khi gọi this.anotherAsyncMethod() → bypass @Async proxy
        this.anotherAsyncMethod();  // Chạy trên main thread, không async!
    }
}
```
Khi gọi method từ chính class đó, Spring proxy bị bypass → `@Async` không có tác dụng. Tách sang class riêng để đảm bảo proxy hoạt động.

### 4.3. Tại sao dùng `TransactionTemplate` thay vì `@Transactional`?
- `@Transactional` + `@Async` cùng method: thứ tự proxy không đảm bảo.
- `@Transactional` trên method riêng + gọi từ chính class: bị self-invocation, proxy không bắt được.
- `TransactionTemplate` giải quyết cả 2 vấn đề, code rõ ràng, dễ test.

### 4.4. Tại sao có 3 lớp defensive?
```
Ollama lỗi
    ↓
ImageAnalysisServiceImpl.parseResponse() → bắt exception → trả Optional.empty()
    ↓
PostAiEnrichmentService.enrichAsync() → bắt RuntimeException → log warn
    ↓
PostServiceImpl.triggerAiEnrichment() → bắt RuntimeException → log warn
    ↓
FE nhận response 200 OK như bình thường
```
**Cam kết:** Dù Ollama có lỗi/sập/disconnected, user vẫn đăng tin thành công, post vẫn lưu DB. AI chỉ là "phần thưởng" thêm.

---

## 5. Files đã thay đổi

### 5.1. Files MỚI (10 files)

| # | File | Loại | Mô tả |
|---|------|------|-------|
| 1 | `backend/.../dto/VisionDescription.java` | DTO | Schema mô tả từ Qwen-VL |
| 2 | `backend/.../dto/OllamaTags.java` | DTO | Container cho (description, tags) |
| 3 | `backend/.../config/OllamaProperties.java` | Config | `@ConfigurationProperties("services.ollama")` |
| 4 | `backend/.../config/OllamaClientConfig.java` | Config | RestClient bean + timeout 60s |
| 5 | `backend/.../client/OllamaClient.java` | Client | Gọi Ollama API, retry 1 lần |
| 6 | `backend/.../service/ImageAnalysisService.java` | Interface | Service abstraction |
| 7 | `backend/.../service/impl/ImageAnalysisServiceImpl.java` | Service | Parse JSON, validate, fallback |
| 8 | `backend/.../config/AsyncConfig.java` | Config | Thread pool `aiAnalysisExecutor` |
| 9 | `backend/.../service/PostAiEnrichmentService.java` | Service | @Async + TransactionTemplate |
| 10 | `backend/src/main/resources/db/migration/V4__add_ai_description.sql` | Migration | Thêm 3 cột vào bảng posts |

### 5.2. Files SỬA (5 files)

| # | File | Thay đổi |
|---|------|----------|
| 1 | `backend/.../entity/Post.java` | Thêm `@Setter` annotation |
| 2 | `backend/.../service/impl/PostServiceImpl.java` | Inject `PostAiEnrichmentService`, gọi `triggerAiEnrichment()` sau khi save post |
| 3 | `backend/src/main/resources/application.yml` | Thêm block `services.ollama` |
| 4 | `.env.example` | Thêm 4 biến `OLLAMA_*` |
| 5 | `docker-compose.yml` | Thêm service `ollama` (port 11434, volume `./docker-data/ollama`) |

---

## 6. Cấu hình

### 6.1. `.env` (thêm vào file .env hiện tại)
```bash
# ===== Ollama (AI vision-language enrichment) =====
OLLAMA_ENABLED=true
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_VISION_MODEL=qwen2.5vl
OLLAMA_TIMEOUT_SECONDS=60
```

### 6.2. `docker-compose.yml` (service mới)
```yaml
ollama:
  image: ollama/ollama:latest
  container_name: lostfound-ollama
  restart: unless-stopped
  ports:
    - "11434:11434"
  volumes:
    - ./docker-data/ollama:/root/.ollama
  environment:
    - OLLAMA_KEEP_ALIVE=10m
  deploy:
    resources:
      reservations:
        devices:
          - driver: nvidia
            count: all
            capabilities: [gpu]
```

### 6.3. `application.yml` (block mới)
```yaml
services:
  ollama:
    base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
    vision-model: ${OLLAMA_VISION_MODEL:qwen2.5vl}
    timeout-seconds: ${OLLAMA_TIMEOUT_SECONDS:60}
    enabled: ${OLLAMA_ENABLED:true}
```

### 6.4. Migration SQL
```sql
-- V4__add_ai_description.sql
ALTER TABLE posts
    ADD COLUMN ai_description TEXT,
    ADD COLUMN ai_tags TEXT,
    ADD COLUMN ai_enriched_at TIMESTAMP;
```

---

## 7. Hướng dẫn chạy End-to-End

### Bước 1: Khởi động infrastructure
```bash
# Từ thư mục gốc project
cd /Users/ttcenter/workplace/SBA/lost-and-found/sba301-lost-and-found

# Copy .env nếu chưa có
cp .env.example .env
# Sửa POSTGRES_PASSWORD, JWT_SECRET, CLOUDINARY_* trong .env

# Khởi PostgreSQL + Ollama
docker compose up -d
```

### Bước 2: Pull model lần đầu (tốn ~5GB, 1 lần duy nhất)
```bash
docker exec -it lostfound-ollama ollama pull qwen2.5vl
```

### Bước 3: Verify Ollama hoạt động
```bash
curl http://localhost:11434/api/tags
# Kỳ vọng: {"models":[{"name":"qwen2.5vl:latest",...}]}
```

### Bước 4: Chạy backend
```bash
cd backend
./mvnw spring-boot:run
```

### Bước 5: Test qua Swagger UI
Mở `http://localhost:8080/swagger-ui.html` → chọn `POST /api/v1/posts` → upload ảnh + form.

### Bước 6: Verify AI enrichment đã chạy
```sql
-- Sau khi upload ảnh, đợi 10-30s rồi query:
SELECT 
    id, 
    title, 
    description,
    ai_description,
    ai_tags,
    ai_enriched_at
FROM posts 
WHERE id = <post_id_vừa_tạo>;
```

Kỳ vọng: 3 cột `ai_*` đã có dữ liệu.

### Bước 7: Verify health check tổng thể
```bash
curl http://localhost:8080/api/v1/health
```
Response sẽ bao gồm:
```json
{
  "status": "UP",
  "components": {
    "database": "UP",
    "clip": "UP",
    "ollama": "UP"  // ← MỚI
  }
}
```

---

## 8. Test cases quan trọng

### Test 1: Ollama offline → user vẫn đăng được
```bash
docker stop lostfound-ollama
# Upload post mới qua Swagger
# Kỳ vọng: response 200 OK, post được lưu, log warn "AI enrichment failed"
```

### Test 2: OLLAMA_ENABLED=false → tắt hoàn toàn flow AI
```bash
# Sửa .env
OLLAMA_ENABLED=false

# Restart backend
# Upload post
# Kỳ vọng: response 200 OK, ai_description = null (không gọi Ollama)
```

### Test 3: User viết mô tả dài → AI ghép thêm
Upload với `description = "Ví da nâu của tôi, có khóa kéo"`.

Kỳ vọng trong DB:
- `description` = `"Ví da nâu của tôi, có khóa kéo\n\n[AI mô tả thêm] ..."`

### Test 4: User bỏ trống description → AI thay thế
Upload với `description = ""` (hoặc không gửi).

Kỳ vọng trong DB:
- `description` = `"Ví da nâu hình chữ nhật, ..."` (toàn bộ từ AI)

---

## 9. Monitoring & Logs

Log format (dễ grep):
```
INFO  PostAiEnrichmentService : Saved AI enrichment for post 123: tags=[ví, da, nâu, khóa kéo]
WARN  PostAiEnrichmentService : AI enrichment failed for post 123: Connection refused
INFO  PostAiEnrichmentService : Replaced empty user description with AI description for post 123
```

Grep nhanh:
```bash
grep "AI enrichment" logs/spring.log
```

---

## 10. Tắt AI khi cần (Rollback an toàn)

Không cần xóa code, chỉ cần:
```bash
# Cách 1: Tắt qua env
OLLAMA_ENABLED=false

# Cách 2: Tắt Docker
docker stop lostfound-ollama

# Cách 3: Comment code
# Trong PostServiceImpl.createLostPost():
# triggerAiEnrichment(post, image, request.getDescription());  // ← comment
```

Sau đó restart backend, hệ thống hoạt động y hệt trước khi tích hợp (chỉ mất tính năng AI enrichment).

---

## 11. Cảnh báo & Best Practices

⚠️ **Memory:** Qwen2.5-VL 7B cần **ít nhất 8GB RAM** (CPU) hoặc **6GB VRAM** (GPU).

⚠️ **First request chậm:** Model cần load vào RAM/VRAM lần đầu (10-30s). Sau đó nhanh hơn.

⚠️ **Concurrent limit:** Thread pool `aiAnalysisExecutor` chỉ có core=2, max=4. Nếu user đăng 100 post cùng lúc, 96 post phải đợi trong queue. Tăng max thread nếu cần.

⚠️ **Privacy:** Ảnh user được gửi nội bộ tới Ollama local. KHÔNG bao giờ gửi ra ngoài Internet.

⚠️ **Cost:** Ollama + Qwen-VL miễn phí 100% (chỉ tốn điện + hardware).

---

## 12. Tóm tắt thay đổi trong 1 câu

> Thêm 10 file mới + sửa 5 file để user upload ảnh → Spring Boot tự động gọi Ollama (Qwen-VL) chạy nền phân tích ảnh, lưu mô tả + tags vào DB, tăng chất lượng matching cho cả tin LOST và FOUND. Toàn bộ flow defensive 3 lớp, async, build SUCCESS với 82 source files.
