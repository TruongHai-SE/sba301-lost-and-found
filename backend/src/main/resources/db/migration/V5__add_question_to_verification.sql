-- Migration: thêm câu hỏi xác minh (verification questions) do AI sinh ra.
-- Mỗi Verification = 1 câu hỏi (do AI tạo từ ảnh).
-- User (chủ post) trả lời → lưu vào CorrectAnswer (đáp án đúng).
-- Khi có match, claimer trả lời → lưu vào VerificationResponse, score so với CorrectAnswer.

ALTER TABLE verifications
    ADD COLUMN IF NOT EXISTS question TEXT,
    ADD COLUMN IF NOT EXISTS question_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    ADD COLUMN IF NOT EXISTS question_index INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS options TEXT;

-- Index phục vụ query "lấy tất cả câu hỏi của 1 post" (sắp xếp theo thứ tự)
CREATE INDEX IF NOT EXISTS idx_verifications_post_idx
    ON verifications(post_id, question_index)
    WHERE post_id IS NOT NULL;
