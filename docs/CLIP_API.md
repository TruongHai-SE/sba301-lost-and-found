# CLIP Service API

The CLIP service is internal. Frontend code should call the Java backend, not
CLIP directly.

Swagger: `http://localhost:8000/docs`

## Data Prerequisite

Every embedding belongs to an existing `posts.id`. Find IDs in pgAdmin:

```sql
SELECT id, title, type, status
FROM posts
ORDER BY id;
```

## Endpoints

### Health

```text
GET /api/clip/health
```

### Index Lost-Post Text

```text
POST /api/clip/embed-and-index/text
```

```json
{
  "post_id": 1,
  "text": "brown leather wallet with student card",
  "translate": false,
  "post_type": "LOST"
}
```

`translate=true` tries to translate text to English before encoding.

### Index Found-Post Image

```text
POST /api/clip/embed-and-index/image
```

For direct Swagger testing:

```json
{
  "post_id": 3,
  "image_url": "https://example.com/public-image.jpg",
  "post_type": "FOUND"
}
```

For future backend integration:

```json
{
  "post_id": 3,
  "image_id": 5,
  "image_url": "https://res.cloudinary.com/.../image.jpg",
  "post_type": "FOUND"
}
```

`image_url` is required because CLIP downloads the image to encode it.
`image_id` is optional and links the vector to an existing `images.id`. Never
send `image_id: 0`.

### Search

```text
POST /api/clip/search
```

Search against FOUND posts:

```json
{
  "query_text": "brown leather wallet",
  "target_post_type": "FOUND",
  "top_k": 10,
  "threshold": 0
}
```

Search against LOST posts:

```json
{
  "query_image_url": "https://example.com/public-image.jpg",
  "target_post_type": "LOST",
  "top_k": 10,
  "threshold": 0
}
```

| Parameter | Meaning |
| --- | --- |
| `target_post_type` | `"FOUND"` searches in found posts. `"LOST"` searches in lost posts. `"ALL"` searches both. |
| `top_k` | Maximum number of results returned after filtering. |
| `threshold` | Minimum cosine similarity. Use `0` for inspection and `0.5` for stricter matching. |
| `query_text` | Text query. |
| `query_image_url` | Public image URL query. Send this or `query_text`, not both. |

### Delete Post Embeddings

```text
DELETE /api/clip/embeddings/{post_id}
```

This deletes every CLIP vector linked to the supplied post.

## Inspect Vectors

Run in pgAdmin after indexing:

```sql
SELECT id, post_id, image_id, source_type, vector_dims(embedding) AS dimensions
FROM clip_embeddings
ORDER BY id;
```

Each vector must have `768` dimensions.
