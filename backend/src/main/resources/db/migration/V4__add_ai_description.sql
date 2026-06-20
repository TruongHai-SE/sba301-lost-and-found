-- Migration: thêm cột lưu kết quả AI enrichment (Ollama + Qwen-VL)
-- An toàn với DB đang chạy vì dùng ADD COLUMN IF NOT EXISTS.

ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS ai_description TEXT;

ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS ai_tags TEXT;

ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS ai_enriched_at TIMESTAMP;

-- Index phục vụ query "tìm post chưa được AI enrich" (cho batch job nếu có)
CREATE INDEX IF NOT EXISTS idx_posts_ai_enriched_at
    ON posts(ai_enriched_at)
    WHERE ai_enriched_at IS NULL;
