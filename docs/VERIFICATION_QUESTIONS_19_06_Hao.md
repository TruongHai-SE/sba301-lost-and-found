# Verification Questions (Approach 1: AI hỏi - User trả lời)

> **Ngày triển khai:** 19/06/2026
> **Tác giả:** Hao
> **Trạng thái:** ✅ Done (build pass, 2/2 test pass)

## 1. Bối cảnh & vấn đề

Trước đây, hệ thống matching chỉ dựa trên **CLIP embedding** (ảnh ↔ ảnh) và **AI description** (text). Vấn đề:

- **Sai khớp cao:** 2 chiếc ví da màu nâu trông giống nhau trên ảnh → CLIP match nhầm.
- **Không có cách xác minh chủ sở hữu:** Sau khi match, làm sao biết người claim có thật sự là chủ?

## 2. Giải pháp: AI sinh câu hỏi xác minh

Khi user tạo post (LOST/FOUND) có ảnh:

1. **Ollama + Qwen-VL** phân tích ảnh → sinh **3-5 câu hỏi xác minh** (loại TEXT / MULTIPLE_CHOICE / BOOLEAN).
2. Lưu vào bảng `verifications` (đã có sẵn, mở rộng thêm 4 cột nhờ V5 migration).
3. **Chủ post** trả lời → lưu vào bảng `correct_answers`.
4. **Claimer** (người match) phải trả lời đúng để được chấp nhận.

Ví dụ câu hỏi AI có thể sinh từ ảnh chiếc ví:
- *"Có sticker / hình vẽ gì trên ví không?"* (TEXT)
- *"Ví có khóa kéo hay không?"* (BOOLEAN)
- *"Màu chính của ví là gì?"* (MULTIPLE_CHOICE: Nâu / Đen / Xanh)

## 3. Kiến trúc & luồng dữ liệu

```
┌────────────┐  upload ảnh    ┌────────────────┐
│  FE/Client │ ─────────────► │  PostController│
└────────────┘                └────────┬───────┘
                                      │ save post
                                      ▼
                              ┌────────────────┐
                              │   PostService  │
                              └───────┬────────┘
                                      │ trigger async
                                      ▼
                    ┌────────────────────────────────┐
                    │  PostAiEnrichmentService (async)│
                    └────────────────┬───────────────┘
                                     │ call Ollama
                                     ▼
                        ┌──────────────────────┐
                        │ Ollama + Qwen-VL     │
                        │ (qwen2.5vl:3b)       │
                        └──────────┬───────────┘
                                   │ JSON: questions[]
                                   ▼
                    ┌──────────────────────────────┐
                    │ save to verifications table  │
                    │ (PostgreSQL)                  │
                    └──────────────────────────────┘
```

### Sequence: chủ post lưu đáp án

```
1. GET  /api/v1/posts/{postId}/verifications
   ← trả về danh sách câu hỏi AI sinh
2. User trả lời (FE gom thành SubmitAnswersRequest)
3. POST /api/v1/posts/{postId}/verifications/answers
   → server upsert vào correct_answers
```

## 4. Các API endpoint mới

| Method | URL | Mô tả |
|---|---|---|
| `GET` | `/api/v1/posts/{postId}/verifications` | Lấy câu hỏi AI sinh. Trả về `status: PENDING` (có câu hỏi) hoặc `NOT_GENERATED` (AI chưa sinh / lỗi). |
| `POST` | `/api/v1/posts/{postId}/verifications/answers` | Chủ post lưu đáp án. Body: `SubmitAnswersRequest`. Response: `SubmitAnswersResponse{postId, saved, message}`. |

## 5. Database migration (V5)

File: `backend/src/main/resources/db/migration/V5__add_question_to_verification.sql`

```sql
ALTER TABLE verifications
  ADD COLUMN question       TEXT,
  ADD COLUMN question_type  VARCHAR(20),   -- TEXT | MULTIPLE_CHOICE | BOOLEAN
  ADD COLUMN question_index INTEGER,       -- thứ tự câu hỏi (0, 1, 2, ...)
  ADD COLUMN options        TEXT;          -- JSON array (cho MULTIPLE_CHOICE)
```

Không tạo bảng mới, tận dụng bảng `verifications` đã có (cũ: dùng cho verification response của claimer, nay mở rộng thêm question/options).

## 6. Cấu trúc thư mục mới

```
backend/src/main/java/com/sba301/lostandfound/
├── dto/
│   ├── VerificationQuestion.java          ← mới: 1 câu hỏi AI sinh
│   ├── VerificationQuestionsResponse.java ← mới: response wrapper
│   ├── SubmitAnswersRequest.java          ← mới: request submit đáp án
│   ├── SubmitAnswersResponse.java         ← mới: response số câu đã lưu
│   └── OllamaQuestionsResponse.java       ← mới: parse JSON từ Ollama
├── service/
│   ├── ImageAnalysisService.java          ← thêm method generateQuestions()
│   ├── impl/ImageAnalysisServiceImpl.java ← implement gọi Ollama
│   ├── PostAiEnrichmentService.java       ← thêm generateVerificationQuestionsAsync()
│   ├── VerificationService.java           ← mới
│   └── impl/VerificationServiceImpl.java  ← mới (scoreAnswer, submitAnswers, getQuestions)
├── controller/
│   └── VerificationController.java        ← mới (2 endpoint GET + POST)
└── entity/
    ├── Verification.java                  ← thêm 4 field + 1 constructor convenience
    └── CorrectAnswer.java                 ← helper (đã có sẵn)
```

## 7. Hợp đồng giữa các thành phần

### `ImageAnalysisService.generateQuestions(imageUrl, userDescription)`
- Gọi Ollama với prompt yêu cầu JSON `{questions: [{question, type, options, importantPoint}]}`.
- Nếu Ollama lỗi / timeout / JSON malformed → trả `Optional.empty()`.
- Không bao giờ throw.

### `PostAiEnrichmentService.generateVerificationQuestionsAsync(postId, imageUrl, userDescription)`
- `@Async("aiAnalysisExecutor")` — chạy trên thread pool riêng.
- Gọi `imageAnalysisService.generateQuestions()`.
- Sắp xếp theo `importantPoint` giảm dần, lấy tối đa **5 câu**.
- Xoá câu hỏi cũ của post (nếu re-generate) rồi insert mới.
- Nếu lỗi → log warning, post vẫn tồn tại bình thường.

### `VerificationService.scoreAnswer(verificationId, claimerAnswer)`
- Trả về điểm 0.0 - 1.0:
  - **BOOLEAN:** exact match sau khi chuẩn hoá (có/không/yes/no/true/false).
  - **MULTIPLE_CHOICE:** exact match sau khi lowercase + bỏ punctuation.
  - **TEXT:** Jaccard similarity + bonus substring match (>= 0.6).
- Nếu chưa có `correct_answer` → trả 0.0 (không cho điểm).

## 8. Test & build

```bash
cd backend && ./mvnw test
# Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
# BUILD SUCCESS
```

## 9. Tích hợp với hệ thống

- **FE:** sau khi `POST /posts` (tạo post), FE poll `GET /posts/{id}/verifications` cho tới khi có câu hỏi → hiển thị form cho user trả lời.
- **Match flow:** khi claimer claim 1 post match, hệ thống build form câu hỏi từ `getQuestionDtos(postId)` → claimer trả lời → dùng `scoreAnswer()` để quyết định pass/fail.

## 10. Lưu ý khi vận hành

- **Ollama phải đang chạy** (`ollama serve`) và model `qwen2.5vl:3b` đã pull.
- **Latency:** lần đầu Ollama load model mất ~5-10s, các lần sau ~2-4s/câu hỏi.
- **Fallback:** nếu Ollama lỗi, post vẫn lưu thành công, chỉ là chưa có câu hỏi xác minh. FE sẽ hiển thị "Đang tạo câu hỏi xác minh..." và retry.
- **Re-generate:** nếu user edit post và up ảnh mới, gọi lại `generateVerificationQuestionsAsync()` sẽ xoá câu cũ + tạo câu mới.

## 11. Cấu hình (application.yml)

```yaml
ollama:
  base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
  questions-model: ${OLLAMA_QUESTIONS_MODEL:qwen2.5vl:3b}
  timeout-seconds: 60
```

Xem chi tiết tại `OllamaProperties.java`.
