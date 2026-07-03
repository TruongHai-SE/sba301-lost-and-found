-- Combined Initial Schema Migration
-- Establishes extensions schema, pgvector, and all database tables for SBA301 Lost and Found.

-- 1. Setup extensions schema and vector extension
CREATE SCHEMA IF NOT EXISTS extensions;

DO $$
DECLARE
    v_current_schema text;
BEGIN
    SELECT n.nspname INTO v_current_schema
    FROM pg_extension e 
    JOIN pg_namespace n ON e.extnamespace = n.oid 
    WHERE e.extname = 'vector';

    IF v_current_schema IS NULL THEN
        CREATE EXTENSION vector SCHEMA extensions;
    ELSIF v_current_schema <> 'extensions' THEN
        ALTER EXTENSION vector SET SCHEMA extensions;
    END IF;
END
$$;


-- 2. Core Tables
CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255),
    password    TEXT,
    type        VARCHAR(15) NOT NULL DEFAULT 'USER',
    phone       VARCHAR(10) UNIQUE,
    mail        VARCHAR(255) UNIQUE,
    social_link TEXT,
    created_at  DATE DEFAULT CURRENT_DATE
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
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS posts (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES users(id),
    location_id     BIGINT REFERENCES locations(id),
    image_id        BIGINT,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    type            VARCHAR(5) NOT NULL,
    event_time      TIMESTAMP,
    created_at      TIMESTAMP DEFAULT NOW(),
    status          VARCHAR(10) DEFAULT 'ACTIVE',
    hide_post_type  VARCHAR(10) DEFAULT 'PUBLIC',
    delete_at       TIMESTAMP,
    ai_description  TEXT,
    ai_tags         TEXT,
    ai_enriched_at  TIMESTAMP,
    CONSTRAINT fk_posts_image FOREIGN KEY (image_id) REFERENCES images(id)
);

CREATE TABLE IF NOT EXISTS match_requests (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT REFERENCES users(id),
    status      VARCHAR(10) DEFAULT 'PENDING',
    created_at  TIMESTAMP DEFAULT NOW(),
    message     TEXT
);

CREATE TABLE IF NOT EXISTS verifications (
    id               BIGSERIAL PRIMARY KEY,
    post_id          BIGINT REFERENCES posts(id),
    title            TEXT,
    important_point  INTEGER,
    question         TEXT,
    question_type    VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    question_index   INTEGER NOT NULL DEFAULT 0,
    options          TEXT
);

CREATE TABLE IF NOT EXISTS verification_answers (
    id                BIGSERIAL PRIMARY KEY,
    verification_id   BIGINT REFERENCES verifications(id),
    answer            TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS claim_attempt_answers (
    id                BIGSERIAL PRIMARY KEY,
    claim_id          BIGINT,
    verification_id   BIGINT REFERENCES verifications(id),
    answer            TEXT,
    score             DOUBLE PRECISION,
    CONSTRAINT fk_claim_attempt_answers_claim FOREIGN KEY (claim_id) REFERENCES match_requests(id)
);

CREATE TABLE IF NOT EXISTS clip_embeddings (
    id          BIGSERIAL PRIMARY KEY,
    post_id     BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    image_id    BIGINT REFERENCES images(id),
    source_type VARCHAR(10) NOT NULL,
    embedding   extensions.vector(768) NOT NULL,
    created_at  TIMESTAMP DEFAULT NOW(),
    CONSTRAINT uq_clip_post_image UNIQUE NULLS NOT DISTINCT (post_id, image_id, source_type)
);

-- 3. Authentication Tables
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       TEXT NOT NULL UNIQUE,
    expires_at  TIMESTAMP NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS otp_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    otp_code    VARCHAR(6) NOT NULL,
    purpose     VARCHAR(20) NOT NULL,
    expires_at  TIMESTAMP NOT NULL,
    used        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 4. Database Indexes
CREATE INDEX IF NOT EXISTS idx_clip_hnsw ON clip_embeddings
    USING hnsw (embedding extensions.vector_cosine_ops)
    WITH (m = 16, ef_construction = 200);

CREATE INDEX IF NOT EXISTS idx_clip_post_id ON clip_embeddings(post_id);
CREATE INDEX IF NOT EXISTS idx_clip_source ON clip_embeddings(source_type);
CREATE INDEX IF NOT EXISTS idx_posts_type ON posts(type);
CREATE INDEX IF NOT EXISTS idx_posts_status ON posts(status);
CREATE INDEX IF NOT EXISTS idx_posts_user ON posts(user_id);
CREATE INDEX IF NOT EXISTS idx_verifications_post_idx ON verifications(post_id, question_index) WHERE post_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_posts_ai_enriched_at ON posts(ai_enriched_at) WHERE ai_enriched_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_revoked ON refresh_tokens(revoked);
CREATE INDEX IF NOT EXISTS idx_otp_tokens_user ON otp_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_otp_tokens_expires_at ON otp_tokens(expires_at);
CREATE INDEX IF NOT EXISTS idx_otp_tokens_used ON otp_tokens(used);

-- 5. Security (Row Level Security - RLS)
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE locations ENABLE ROW LEVEL SECURITY;
ALTER TABLE images ENABLE ROW LEVEL SECURITY;
ALTER TABLE posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE match_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE verifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE verification_answers ENABLE ROW LEVEL SECURITY;
ALTER TABLE claim_attempt_answers ENABLE ROW LEVEL SECURITY;
ALTER TABLE clip_embeddings ENABLE ROW LEVEL SECURITY;
ALTER TABLE refresh_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE otp_tokens ENABLE ROW LEVEL SECURITY;


