-- Adopt the original local schema and establish Flyway as the schema owner.
-- Future schema changes must use new versioned migrations.

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255),
    password    TEXT,
    type        VARCHAR(15) NOT NULL DEFAULT 'USER',
    phone       VARCHAR(10) UNIQUE,
    mail        VARCHAR(255) UNIQUE,
    social_link TEXT,
    create_at   DATE DEFAULT CURRENT_DATE
);

CREATE TABLE IF NOT EXISTS locations (
    id              BIGSERIAL PRIMARY KEY,
    address         TEXT,
    city            VARCHAR(50),
    district        VARCHAR(50),
    latitude        DOUBLE PRECISION,
    longitude       DOUBLE PRECISION,
    location_level  INTEGER
);

CREATE TABLE IF NOT EXISTS images (
    id          BIGSERIAL PRIMARY KEY,
    url         TEXT NOT NULL,
    private_url TEXT,
    create_at   TIMESTAMP DEFAULT NOW()
);

ALTER TABLE images ADD COLUMN IF NOT EXISTS private_url TEXT;

CREATE TABLE IF NOT EXISTS posts (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES users(id),
    location_id     BIGINT REFERENCES locations(id),
    image_id        BIGINT,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    type            VARCHAR(5) NOT NULL,
    event_time      TIMESTAMP,
    create_at       TIMESTAMP DEFAULT NOW(),
    status          VARCHAR(10) DEFAULT 'ACTIVE',
    hide_post_type  VARCHAR(10) DEFAULT 'PUBLIC',
    delete_at       TIMESTAMP
);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'posts'
          AND column_name = 'image_id'
          AND data_type <> 'bigint'
    ) THEN
        ALTER TABLE posts
            ALTER COLUMN image_id TYPE BIGINT
            USING NULLIF(image_id, '')::BIGINT;
    END IF;
END
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_posts_image'
    ) THEN
        ALTER TABLE posts
            ADD CONSTRAINT fk_posts_image
            FOREIGN KEY (image_id) REFERENCES images(id);
    END IF;
END
$$;

CREATE TABLE IF NOT EXISTS match_requests (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT REFERENCES users(id),
    status      VARCHAR(10) DEFAULT 'PENDING',
    create_at   TIMESTAMP DEFAULT NOW(),
    message     TEXT
);

CREATE TABLE IF NOT EXISTS verifications (
    id               BIGSERIAL PRIMARY KEY,
    post_id          BIGINT REFERENCES posts(id),
    title            TEXT,
    important_point  INTEGER
);

CREATE TABLE IF NOT EXISTS correct_answers (
    id                BIGSERIAL PRIMARY KEY,
    verification_id   BIGINT REFERENCES verifications(id),
    answer            TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS verification_responses (
    id                BIGSERIAL PRIMARY KEY,
    claim_id          BIGINT,
    verification_id   BIGINT REFERENCES verifications(id),
    answer            TEXT,
    score             DOUBLE PRECISION
);

CREATE TABLE IF NOT EXISTS clip_embeddings (
    id          BIGSERIAL PRIMARY KEY,
    post_id     BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    image_id    BIGINT REFERENCES images(id),
    source_type VARCHAR(10) NOT NULL,
    embedding   vector(768) NOT NULL,
    created_at  TIMESTAMP DEFAULT NOW(),
    CONSTRAINT uq_clip_post_image
        UNIQUE NULLS NOT DISTINCT (post_id, image_id, source_type)
);

CREATE INDEX IF NOT EXISTS idx_clip_hnsw ON clip_embeddings
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 200);
CREATE INDEX IF NOT EXISTS idx_clip_post_id ON clip_embeddings(post_id);
CREATE INDEX IF NOT EXISTS idx_clip_source ON clip_embeddings(source_type);
CREATE INDEX IF NOT EXISTS idx_posts_type ON posts(type);
CREATE INDEX IF NOT EXISTS idx_posts_status ON posts(status);
CREATE INDEX IF NOT EXISTS idx_posts_user ON posts(user_id);
