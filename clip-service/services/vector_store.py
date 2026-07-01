from typing import Optional

import numpy as np
import psycopg2
import psycopg2.extras

from config import settings


class VectorStore:
    """pgvector CRUD for the clip_embeddings table."""

    def __init__(self, dsn: str | None = None):
        self.dsn = dsn or settings.database_url
        self._conn: Optional[psycopg2.extensions.connection] = None

    @property
    def conn(self):
        if self._conn is None or self._conn.closed:
            self._conn = psycopg2.connect(self.dsn)
            self._conn.autocommit = False
        return self._conn

    # Upsert
    def upsert(
        self,
        post_id: int,
        embedding: np.ndarray,
        source_type: str,
        image_id: int | None = None,
    ) -> int:
        """Insert or update a CLIP embedding and return its ID."""
        vec_str = "[" + ",".join(f"{value:.8f}" for value in embedding) + "]"
        with self.conn:
            with self.conn.cursor() as cursor:
                cursor.execute(
                    """
                    INSERT INTO clip_embeddings (post_id, image_id, source_type, embedding)
                    VALUES (%s, %s, %s, %s::vector)
                    ON CONFLICT (post_id, image_id, source_type)
                    DO UPDATE SET embedding = EXCLUDED.embedding,
                                  created_at = NOW()
                    RETURNING id
                    """,
                    (post_id, image_id, source_type, vec_str),
                )
                row = cursor.fetchone()
                return row[0]

    # Search
    def search(
        self,
        query_vec: np.ndarray,
        target_post_type: str,
        top_k: int = 10,
        threshold: float = 0.5,
    ) -> list[dict]:
        """
        Run cosine similarity search against active posts using HNSW CTE.
        
        Filter by target_post_type ('LOST', 'FOUND', or 'ALL').
        """
        vec_str = "[" + ",".join(f"{value:.8f}" for value in query_vec) + "]"
        with self.conn:
            with self.conn.cursor(
                cursor_factory=psycopg2.extras.RealDictCursor
            ) as cursor:
                cursor.execute(
                    """
                    WITH nearest_embeddings AS (
                        SELECT ce.id AS emb_id, ce.post_id,
                               1 - (ce.embedding <=> %s::vector) AS score
                        FROM clip_embeddings ce
                        JOIN posts p ON p.id = ce.post_id
                        WHERE p.status = 'ACTIVE'
                          AND (%s = 'ALL' OR p.type = %s)
                          AND 1 - (ce.embedding <=> %s::vector) >= %s
                        ORDER BY ce.embedding <=> %s::vector
                        LIMIT %s
                    ),
                    candidate_posts AS (
                        SELECT DISTINCT post_id FROM nearest_embeddings
                    )
                    SELECT ce.id, ce.post_id, ce.image_id, ce.source_type, p.type as post_type,
                           p.title, p.description,
                           COALESCE(ne.score, 1 - (ce.embedding <=> %s::vector)) AS score
                    FROM clip_embeddings ce
                    JOIN posts p ON p.id = ce.post_id
                    JOIN candidate_posts cp ON ce.post_id = cp.post_id
                    LEFT JOIN nearest_embeddings ne ON ce.id = ne.emb_id
                    """,
                    (vec_str, target_post_type, target_post_type, vec_str, threshold, vec_str, top_k, vec_str),
                )
                return [dict(row) for row in cursor.fetchall()]

    # Delete
    def delete_by_post(self, post_id: int) -> int:
        """Delete all embeddings for a post and return the deleted count."""
        with self.conn:
            with self.conn.cursor() as cursor:
                cursor.execute(
                    "DELETE FROM clip_embeddings WHERE post_id = %s", (post_id,)
                )
                return cursor.rowcount

    # Cleanup
    def close(self):
        if self._conn and not self._conn.closed:
            self._conn.close()
