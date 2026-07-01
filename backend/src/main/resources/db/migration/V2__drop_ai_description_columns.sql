-- Bỏ 3 cột AI enrichment không còn được sử dụng.
-- Lý do: ai_description/ai_tags/ai_enriched_at là dead data - được ghi nhưng
-- không bao giờ được đọc ra cho matching/search (CLIP chỉ embed title+description
-- của user). Xem entity Post.java và PostAiEnrichmentServiceImpl.

DROP INDEX IF EXISTS idx_posts_ai_enriched_at;
ALTER TABLE posts DROP COLUMN IF EXISTS ai_description;
ALTER TABLE posts DROP COLUMN IF EXISTS ai_tags;
ALTER TABLE posts DROP COLUMN IF EXISTS ai_enriched_at;
