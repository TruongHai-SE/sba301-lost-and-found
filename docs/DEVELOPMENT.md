# Development Workflow

## Prerequisites

Install the following tools before starting:

| Tool | Version | Purpose |
|------|---------|---------|
| Docker Desktop | Latest | Runs PostgreSQL container |
| JDK | 21 or newer | Backend compilation & runtime |
| CPython | 3.12.x | CLIP service runtime |
| IntelliJ IDEA | Any | Recommended Java IDE |
| pgAdmin 4 | Optional | Database GUI |

---

## First-Time Setup

From the **repository root**:

```powershell
# 1. Create your local environment file
Copy-Item .env.example .env

# 2. Create the Python virtual environment for the CLIP service
python -m venv clip-service\.venv
.\.venv\Scripts\python.exe -m pip install -r clip-service\requirements.txt

# 3. Download the CLIP + YOLO models (requires internet, ~1 GB)
.\clip-service\.venv\Scripts\python.exe scripts\download_models.py
```

> Each developer maintains their own `.env`. **Never commit** `.env`, Cloudinary secrets, model weights, or `docker-data/` to Git.

---

## Daily Startup

### 1. Start Docker Desktop

Wait until the Docker engine is fully running before proceeding.

### 2. Start the Backend

From the `backend/` directory:

```powershell
.\mvnw.cmd spring-boot:run
```

Or open `LostAndFoundApplication.java` in IntelliJ and click the green ▶ Run button.

On startup, Flyway automatically applies any pending database migrations.

### 3. Start the CLIP Service

From the **repository root**:

```powershell
.\scripts\windows\start-clip.bat
```

The script starts the PostgreSQL container (if not already running) and launches FastAPI using the `clip-service/.venv` virtual environment.

---

## Service URLs

| Service | URL |
|---------|-----|
| Backend Swagger UI | `http://localhost:8080/swagger-ui.html` |
| Backend Health | `http://localhost:8080/api/v1/system/health` |
| CLIP Swagger UI | `http://localhost:8000/docs` |
| CLIP Health | `http://localhost:8000/api/v1/health` |

> Full CLIP API details: [CLIP_API.md](CLIP_API.md)

---

## Environment Variables (`.env`)

Key variables required in your `.env` file:

| Variable | Description |
|----------|-------------|
| `POSTGRES_PASSWORD` | PostgreSQL password |
| `JWT_SECRET` | Secret key for signing JWTs (min 256-bit) |
| `JWT_ACCESS_TOKEN_EXPIRATION` | Access token TTL in milliseconds (e.g., `900000` = 15 min) |
| `JWT_REFRESH_TOKEN_EXPIRATION` | Refresh token TTL in milliseconds (e.g., `604800000` = 7 days) |
| `GOOGLE_CLIENT_ID` | Google OAuth Client ID for Google Login |
| `CLOUDINARY_CLOUD_NAME` | Cloudinary cloud name for image uploads |
| `CLOUDINARY_API_KEY` | Cloudinary API key |
| `CLOUDINARY_API_SECRET` | Cloudinary API secret |
| `MAIL_USERNAME` | Gmail/SMTP username for sending OTP emails |
| `MAIL_PASSWORD` | Gmail App Password |
| `APP_CORS_ALLOWED_ORIGINS` | Comma-separated frontend origins (or `*` for open CORS) |
| `APP_COOKIE_SECURE` | `false` locally, `true` in production (HTTPS only cookie) |

---

## Database

Docker PostgreSQL connection details:

```
Host:     127.0.0.1
Port:     5433
Database: lostfound
Username: postgres
Password: value of POSTGRES_PASSWORD in .env
```

**Schema management rules:**
- All schema changes must be done via **Flyway migration files**.
- Hibernate uses `ddl-auto=validate` — it must never create or alter tables.
- **Never** edit an existing migration file that has already been applied.
- **Never** add alternative schema mechanisms (`init-db.sql`, `schema.sql`, `ddl-auto=update`).

**Adding a schema change:**
1. Create `Vn__description.sql` in `backend/src/main/resources/db/migration/`.
2. Update or create the matching `@Entity` class.
3. Restart the backend — Flyway applies the migration automatically.

---

## Authentication Flow

### Login (email/password or Google)

1. Client sends credentials to `POST /api/v1/auth/login` or `POST /api/v1/auth/google`.
2. Server returns **Access Token** in the JSON response body (`data.accessToken`).
3. Server sets **Refresh Token** as an `HttpOnly` cookie (`refreshToken`, Path=`/api/v1/auth`).
4. Frontend stores the Access Token in memory (recommended) or localStorage.
5. For all protected API calls, attach: `Authorization: Bearer <accessToken>`.

### Token Refresh (Silent Refresh)

1. When the Access Token expires, call `POST /api/v1/auth/refresh`.
2. Browser automatically includes the `refreshToken` cookie (same-origin only, due to `Path=/api/v1/auth`).
3. Server validates the Refresh Token in DB, issues a new Access Token in the response body.

### Setup Password (Google-only accounts)

Google-only users (`hasPassword: false` in `GET /api/v1/auth/me`) can create a local password:
- Call `POST /api/v1/auth/password/setup` with a valid Access Token.
- Returns `409 Conflict` if a local password is already set.
- After setup, the user can log in with both Google and email/password.

---

## Local-Only Files (Never Push to Git)

```text
.env
.local/
docker-data/
backend/target/
clip-service/.venv/
clip-service/models/
__pycache__/
*.pyc
*.log
.idea/
.vscode/
```

---

## Before Any Git Push

1. Start both backend and CLIP service.
2. Verify all Swagger UI endpoints behave correctly.
3. Run unit tests: `cd backend && mvn test`.
4. Obtain project-owner approval before running `git add`, `git commit`, or `git push`.
