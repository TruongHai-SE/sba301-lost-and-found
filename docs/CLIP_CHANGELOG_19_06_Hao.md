# CLIP Service – Changelog & Implementation Notes

Tài liệu này ghi lại các chỉnh sửa kỹ thuật được thực hiện trên **CLIP service** (`clip-service/main.py`) và **Java client** (`backend/.../client/ClipClient.java`) nhằm cải thiện khả năng debug, logging và độ ổn định khi giao tiếp giữa backend và CLIP service.

---

## 1. `clip-service/main.py` – Bổ sung Exception Handlers & Logging

### 1.1. Mục đích

Trước đây, khi API nhận request sai định dạng (validation fail) hoặc exception nội bộ (lỗi 500), response chỉ trả về thông báo generic, **không log gì cả** khiến việc debug rất khó khăn. Các thay đổi dưới đây nhằm:

- Ghi log toàn bộ stack trace cho mọi lỗi server-side.
- Trả về response có cấu trúc rõ ràng kèm raw request body để dễ reproduce.
- Hỗ trợ debug nhanh khi backend Java gọi sang mà payload bị sai schema.

### 1.2. Các thay đổi chi tiết

#### a) Thêm import và logger

```python
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
import logging
import traceback

logger = logging.getLogger("uvicorn.error")
```

- Dùng logger `uvicorn.error` để tận dụng pipeline log đã có của Uvicorn (xuất ra cùng stdout khi chạy server).
- Module `traceback` được import lazily bên trong handler để tránh overhead khi khởi động.

#### b) Handler lỗi validation (HTTP 422)

```python
@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request, exc):
    body = await request.body()
    logger.error(f"Validation error: {exc.errors()}")
    logger.error(f"Request body: {body.decode('utf-8', errors='ignore')}")
    return JSONResponse(
        status_code=422,
        content={
            "detail": exc.errors(),
            "body": body.decode('utf-8', errors='ignore'),
        },
    )
```

**Tác dụng:**
- Log lại danh sách field bị sai (theo format của Pydantic) **và** raw body client gửi lên.
- Response trả về 422 có thêm key `body` để frontend dễ truy vết lỗi payload.

**Ví dụ response khi thiếu `post_type`:**
```json
{
  "detail": [
    {
      "type": "missing",
      "loc": ["body", "post_type"],
      "msg": "Field required"
    }
  ],
  "body": "{\"post_id\":1,\"text\":\"...\"}"
}
```

#### c) Handler exception toàn cục (HTTP 500)

```python
@app.exception_handler(Exception)
async def global_exception_handler(request, exc):
    logger.error(f"Global error: {str(exc)}")
    logger.error(traceback.format_exc())
    return JSONResponse(
        status_code=500,
        content={"detail": str(exc), "traceback": traceback.format_exc()},
    )
```

**Tác dụng:**
- Bắt **mọi** exception chưa được xử lý (kể cả lỗi trong service layer, YOLO, CLIP, psycopg2, …).
- Trả về traceback đầy đủ trong response để dev có thể debug mà không cần truy cập log server (chỉ nên dùng trong môi trường dev – xem cảnh báo bảo mật bên dưới).

#### d) Bổ sung log trong route `embed_image` và `embed_text`

```python
except Exception as e:
    logger.error(f"Error in embed_image: {e}", exc_info=True)
    raise HTTPException(status_code=500, detail=str(e))
```

`exc_info=True` đảm bảo traceback được ghi vào log ngay cả khi lỗi được raise lại. Đây là lớp log thứ hai, bổ trợ cho global handler phía trên.

### 1.3. Cảnh báo bảo mật ⚠️

> Handler trả về **traceback đầy đủ** trong response 500. Điều này tiện cho việc debug ở môi trường development, nhưng có thể **lộ thông tin nội bộ** (đường dẫn file, cấu trúc thư mục, biến môi trường, …) nếu deploy production.
>
> **Khuyến nghị:** Trước khi go-live, nên đổi `global_exception_handler` để chỉ trả về `{"detail": "Internal server error"}` ở production, và chỉ giữ traceback trong log file.

### 1.4. Kiểm thử nhanh

```bash
# Gửi payload thiếu post_type → kỳ vọng 422 kèm body
curl -X POST http://localhost:8000/api/v1/embeddings/text \
  -H "Content-Type: application/json" \
  -d '{"post_id": 1, "text": "wallet", "translate": true}'

# Trigger exception (ví dụ: post_id không tồn tại trong DB)
curl -X POST http://localhost:8000/api/v1/embeddings/text \
  -H "Content-Type: application/json" \
  -d '{"post_id": 99999, "text": "wallet", "translate": true, "post_type": "LOST"}'
```

Trong log của Uvicorn, bạn sẽ thấy:
```
ERROR: Validation error: [{'type': 'missing', 'loc': ['body', 'post_type'], ...}]
ERROR: Request body: {"post_id": 1, "text": "wallet", "translate": true}
```

---

## 2. `ClipClient.java` – Khai báo `contentType` rõ ràng

### 2.1. Mục đích

Một số endpoint của CLIP service (đặc biệt là `/api/v1/embeddings/text` và `/api/v1/embeddings/image`) từng **thi thoảng nhận request với `Content-Type` không rõ ràng** khi Spring `RestClient` tự suy luận. Trong một số phiên bản/proxy, điều này khiến FastAPI từ chối body (422) vì parser không nhận diện được JSON.

Thay đổi dưới đây **khai báo tường minh** `Content-Type: application/json` ngay trong mỗi request POST, tránh phụ thuộc vào hành vi mặc định của RestClient.

### 2.2. Các thay đổi chi tiết

#### Trước

```java
public ClipEmbedResponse embedText(Long postId, String text, String postType) {
    return restClient.post()
        .uri("/api/v1/embeddings/text")
        .body(new EmbedTextRequest(postId, text, true, postType))
        .retrieve()
        .body(ClipEmbedResponse.class);
}

public ClipEmbedResponse embedImage(Long postId, String imageUrl, Long imageId, String postType) {
    return restClient.post()
        .uri("/api/v1/embeddings/image")
        .body(new EmbedImageRequest(postId, imageUrl, imageId, postType))
        .retrieve()
        .body(ClipEmbedResponse.class);
}
```

#### Sau

```java
public ClipEmbedResponse embedText(Long postId, String text, String postType) {
    return restClient.post()
        .uri("/api/v1/embeddings/text")
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .body(new EmbedTextRequest(postId, text, true, postType))
        .retrieve()
        .body(ClipEmbedResponse.class);
}

public ClipEmbedResponse embedImage(Long postId, String imageUrl, Long imageId, String postType) {
    return restClient.post()
        .uri("/api/v1/embeddings/image")
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .body(new EmbedImageRequest(postId, imageUrl, imageId, postType))
        .retrieve()
        .body(ClipEmbedResponse.class);
}
```

**Khác biệt duy nhất:** thêm `.contentType(MediaType.APPLICATION_JSON)` trước `.body(...)` ở cả 2 method.

### 2.3. Tác động

- **Ổn định hơn khi giao tiếp với FastAPI:** tránh trường hợp request không có Content-Type đúng hoặc RestClient suy luận sai (`application/x-www-form-urlencoded` chẳng hạn) khiến FastAPI từ chối body.
- **Không thay đổi chức năng:** endpoint, payload, response và business logic vẫn giữ nguyên.
- **Không ảnh hưởng tới method `getHealthStatus`:** method này là GET nên không cần Content-Type body.

### 2.4. Kiểm thử nhanh

Sau khi rebuild backend (`./mvnw clean install` rồi restart), gọi các service có trigger embed (ví dụ `POST /api/posts` tạo bài LOST/FOUND mới). Trong log của CLIP service, bạn sẽ thấy request đến với header chính xác:

```
POST /api/v1/embeddings/text
Content-Type: application/json
{"post_id":42,"text":"...","translate":true,"post_type":"LOST"}
```

---

## 3. Tóm tắt thay đổi

| File | Loại thay đổi | Mục đích chính |
|------|---------------|-----------------|
| `clip-service/main.py` | Thêm 2 exception handler + logger + log trong route | Debug dễ hơn khi API fail, response có cấu trúc rõ ràng |
| `backend/.../ClipClient.java` | Thêm `.contentType(APPLICATION_JSON)` cho 2 method POST | Đảm bảo Content-Type đúng khi gọi CLIP service từ Java |

## 4. Hướng phát triển tiếp theo (TODO)

- [ ] Tách logging config ra file `logging.yaml` để cấu hình level, format, rotate dễ hơn.
- [ ] Giấu traceback trong response 500 ở production (chỉ giữ trong log).
- [ ] Viết unit test cho 2 exception handler trong `main.py`.
- [ ] Refactor `ClipClient` để dùng `body()` builder chung, tránh lặp lại code giữa `embedText` và `embedImage`.
- [ ] Thêm retry/backoff khi gọi CLIP service bị timeout hoặc trả 5xx.
