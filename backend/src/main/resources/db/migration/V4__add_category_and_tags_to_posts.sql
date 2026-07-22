-- Migration V4: Add category enum and tags array columns to posts table
ALTER TABLE posts ADD COLUMN IF NOT EXISTS category VARCHAR(30) DEFAULT 'OTHER';
ALTER TABLE posts ADD COLUMN IF NOT EXISTS tags TEXT[] DEFAULT '{}';

CREATE INDEX IF NOT EXISTS idx_posts_category ON posts(category);
CREATE INDEX IF NOT EXISTS idx_posts_tags ON posts USING GIN (tags);
