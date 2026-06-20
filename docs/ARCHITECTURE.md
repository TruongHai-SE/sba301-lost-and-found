# System Architecture

## Architecture Overview

The SBA301 Lost and Found system uses a microservice-oriented layout combining a Java/Spring Boot business backend, a Python/FastAPI AI CLIP service, and a PostgreSQL database with pgvector extension.

```text
Client (Frontend / Mobile)
        |
        v
Spring Boot Backend (Port 8080) -------> FastAPI CLIP Service (Port 8000)
        |                                           |
        +-------------------+-----------------------+
                            |
                            v
                 PostgreSQL 16 + pgvector (Port 5433)
```

---

## 3-Tier Layered Architecture

The Java backend follows a strict **3-Tier Layered Architecture** (Controller → Service → Repository). Dependencies flow unidirectionally downward.

### Package Layout

```text
com.sba301.lostandfound
|-- LostAndFoundApplication.java    (Main startup entry point)
|-- controller/                     (REST controllers – HTTP endpoints only)
|-- service/                        (Business logic interfaces & implementations)
|-- repository/                     (Spring Data JPA repositories for DB access)
|-- entity/                         (JPA persistence entities mapped to DB tables)
|   `-- enums/                      (Enum types: UserType, PostType, OtpPurpose …)
|-- dto/                            (Request & Response DTOs for serialization)
|-- client/                         (HTTP clients to external services: CLIP, Ollama)
|-- security/                       (JWT filter, token provider, cookie utilities)
|-- config/                         (SecurityConfig, CORS, general bean declarations)
`-- scheduler/                      (Background cron jobs: expired token cleanup)
```

### Dependency Flow

```text
[ Controller ] → [ Service ] → [ Repository ] ←→ [ Entity ]
```

- **Controller**: Validates HTTP input (`@Valid`), delegates to Service, returns `ApiResponse<T>`.
- **Service**: Implements business transactions, orchestrates cross-repository operations, calls `ClipClient` or `OllamaClient` when AI is needed.
- **Repository**: Provides typed JPA queries over database tables.
- **Entity**: Maps directly to PostgreSQL tables; schema is owned by Flyway migrations.

---

## Security Architecture

### JWT Token Strategy

| Token         | Storage Location                       | Scope / Lifetime                         |
|---------------|----------------------------------------|------------------------------------------|
| Access Token  | Response JSON body (`data.accessToken`) | Short-lived (~15 min). Frontend holds in memory or localStorage. |
| Refresh Token | HttpOnly Cookie (`refreshToken`)       | Long-lived (~7 days). Stored in DB (`refresh_tokens` table) for revocation support. |

Cookie properties for the Refresh Token:
- `HttpOnly=true` — inaccessible to JavaScript (blocks XSS token theft)
- `Secure` — set via `app.cookie.secure` in `.env` (false for local, true for production)
- `SameSite=Lax` — mitigates CSRF attacks
- `Path=/api/v1/auth` — browser only sends the cookie for auth endpoints (`/refresh`, `/logout`)

### Authorization Rules (`SecurityConfig`)

Rules are evaluated top-to-bottom (first match wins):

| Priority | Pattern                          | Rule                      | Notes                           |
|----------|----------------------------------|---------------------------|---------------------------------|
| 1        | `/api/v1/auth/password/setup`    | `authenticated()`         | Requires JWT — Google-only users setting local password |
| 1        | `/api/v1/auth/me`                | `authenticated()`         | Requires JWT — returns current user profile |
| 2        | `/api/v1/auth/**`                | `permitAll()`             | Login, register, forgot/reset password, Google OAuth |
| 3        | `/actuator/health`, `/actuator/info`, `/api/v1/system/**` | `permitAll()` | Health & monitoring endpoints |
| 4        | `/swagger-ui/**`, `/v3/api-docs/**` | `permitAll()`          | API documentation               |
| 5        | `/`, `/index.html`, `/*.css`, `/*.js`, `/favicon.ico`, `/error` | `permitAll()` | Static assets |
| 6        | `/api/v1/admin/**`               | `hasRole("ADMIN")`        | Admin management panel          |
| 7        | `anyRequest()`                   | `authenticated()`         | All other application APIs      |

Security exceptions (401/403) are serialized as `ApiResponse<Void>` via `AuthenticationEntryPoint` and `AccessDeniedHandler`.

---

## API Inventory

### Auth Controller — `/api/v1/auth`

| Method | Path                    | Auth Required | Description                                          |
|--------|-------------------------|---------------|------------------------------------------------------|
| POST   | `/register`             | No            | Register a new user with email/password              |
| POST   | `/login`                | No            | Login with email/password, returns Access + sets Refresh Token cookie |
| POST   | `/google`               | No            | Login / register via Google ID Token                 |
| POST   | `/refresh`              | Cookie        | Exchange Refresh Token cookie for new Access Token   |
| POST   | `/logout`               | Cookie        | Revoke Refresh Token and clear cookie                |
| POST   | `/forgot-password`      | No            | Send OTP to email for password reset                 |
| POST   | `/reset-password`       | No            | Reset password using valid OTP                       |
| POST   | `/password/setup`       | ✅ JWT        | Set a local password for a Google-only account       |
| GET    | `/me`                   | ✅ JWT        | Get current user profile (`hasPassword`, `googleAccount` flags) |

### Post Controller — `/api/v1/posts`

| Method | Path     | Auth Required | Description                         |
|--------|----------|---------------|-------------------------------------|
| POST   | `/`      | ✅ JWT        | Create a LOST post (multipart form) |
| POST   | `/found` | ✅ JWT        | Create a FOUND post (multipart form)|

### Search Controller — `/api/v1/search`

| Method | Path    | Auth Required | Description                                         |
|--------|---------|---------------|-----------------------------------------------------|
| POST   | `/`     | ✅ JWT        | Search by image (multipart). Returns blurred results.|
| POST   | `/text` | ✅ JWT        | Search by text (JSON). Returns blurred results.      |

### Verification Controller — `/api/v1/posts/{postId}`

| Method | Path                       | Auth Required | Description                                   |
|--------|----------------------------|---------------|-----------------------------------------------|
| GET    | `/{postId}/verifications`  | ✅ JWT        | Get AI-generated verification questions for a post |
| POST   | `/{postId}/claim`          | ✅ JWT        | Answer verification questions to claim a post  |

### Admin User Controller — `/api/v1/admin/users`

| Method | Path           | Auth Required    | Description                          |
|--------|----------------|------------------|--------------------------------------|
| GET    | `/`            | ✅ ADMIN only    | List all users (paginated)           |
| GET    | `/{id}`        | ✅ ADMIN only    | Get single user by ID                |
| PUT    | `/{id}`        | ✅ ADMIN only    | Update user profile                  |
| PATCH  | `/{id}/role`   | ✅ ADMIN only    | Change user role (USER ↔ ADMIN)      |
| DELETE | `/{id}`        | ✅ ADMIN only    | Delete user (guards against last admin, users with posts) |

### System Controller — `/api/v1/system`

| Method | Path      | Auth Required | Description                  |
|--------|-----------|---------------|------------------------------|
| GET    | `/health` | No            | Returns system health status |

---

## Database Schema & Migrations

Schema is managed exclusively via **Flyway**. Hibernate is set to `ddl-auto=validate` and must never create or alter tables.

Current migrations in `src/main/resources/db/migration/`:

| File | Summary |
|------|---------|
| `V1__init.sql` | Core tables: `users`, `posts`, `images`, `clip_embeddings` |
| `V2__add_claim_token.sql` | Claim token support |
| `V3__...` | OTP tokens |
| `V4__...` | Refresh tokens |
| `V5__...` | Latest schema additions |

Adding a schema change:
1. Create `Vn__description.sql` in `db/migration/`.
2. Update or create the corresponding `@Entity` class.
3. Restart the application — Flyway applies the migration automatically.

---

## AI Integration

### CLIP Service (Port 8000)

Used for semantic image/text embedding and similarity search.

1. **Lost Post Indexing**: On post creation, Spring Boot sends the description text to the CLIP service → encodes to a 768-dim `TEXT` vector → stored in `clip_embeddings`.
2. **Found Post Indexing**: The uploaded image is sent to CLIP → encoded to a 768-dim `IMAGE` vector → stored in `clip_embeddings`.
3. **Similarity Search**: Cosine similarity via `pgvector` in PostgreSQL matches lost ↔ found items.
4. **Results**: Always returned as `BlurredPostSummary` (blurred images, no contact info). Full details revealed only after successful claim verification.

### Ollama / Qwen Integration

Used for AI-generated **Verification Questions** (ownership proof). When a FOUND post is created, Ollama (running `qwen` or similar model locally) generates contextual questions based on the item image/description. These questions are stored and served via `GET /{postId}/verifications`.

---

## Response Format

All API endpoints return a unified envelope:

```json
{
  "status": 200,
  "message": "Success message",
  "data": { ... },
  "errors": null,
  "timestamp": "2024-01-15T10:00:00Z",
  "path": "/api/v1/auth/login"
}
```

Errors follow the same structure with `data: null` and `errors` containing field-level validation details.
