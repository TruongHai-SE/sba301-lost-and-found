# Development Workflow

## First-Time Setup

Install:

- Docker Desktop.
- JDK 21 or newer.
- CPython 3.12.x.
- IntelliJ IDEA if you develop the Java backend.
- pgAdmin 4 only if you want a database GUI.

From the repository root:

```powershell
Copy-Item .env.example .env
python -m venv clip-service\.venv
.\clip-service\.venv\Scripts\python.exe -m pip install -r clip-service\requirements.txt
.\clip-service\.venv\Scripts\python.exe scripts\download_models.py
```

Each developer creates their own `.env`. Never commit or send `.env`,
Cloudinary secrets, local models, or database files through Git.

## Daily Startup

Open Docker Desktop and wait until its engine is running.

Start the backend from IntelliJ IDEA by running `LostAndFoundApplication`, or
use Maven Wrapper:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Start CLIP from the repository root:

```powershell
.\scripts\windows\start-clip.bat
```

The CLIP script starts PostgreSQL if needed and uses `clip-service/.venv` when
it exists.

## Service URLs

| Service | URL |
| --- | --- |
| Backend Swagger | `http://localhost:8080/swagger-ui.html` |
| Backend health | `http://localhost:8080/api/system/health` |
| CLIP Swagger | `http://localhost:8000/docs` |
| CLIP health | `http://localhost:8000/api/clip/health` |

CLIP API details are documented in [CLIP_API.md](CLIP_API.md).

## Database

Docker PostgreSQL uses:

```text
Host:     127.0.0.1
Port:     5433
Database: lostfound
Username: postgres
Password: POSTGRES_PASSWORD from .env
```

Backend startup runs Flyway migrations. Hibernate uses `ddl-auto=validate`;
it must not create or alter tables.

For every schema change:

1. Add a new migration such as
   `backend/src/main/resources/db/migration/V2__add_claim_token.sql`.
2. Update the matching JPA persistence entity.
3. Restart backend and test through Swagger.

Do not edit a shared migration or add another schema mechanism such as
`init-db.sql`, `schema.sql`, or Hibernate `ddl-auto=update`.

## Local-Only Files

Do not push:

```text
.env
.local/
docker-data/
backend/target/
clip-service/.venv/
__pycache__/
*.pyc
*.log
.idea/
.vscode/
```

Before any Git push, test both Swagger UIs and obtain project-owner approval
before running `git add`, commit, or push.
