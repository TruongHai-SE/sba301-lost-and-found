# SBA301 Lost and Found

Mono-repo for a Lost and Found system:

- `backend/`: Spring Boot, Clean Architecture, Flyway, Swagger.
- `clip-service/`: FastAPI, CLIP ONNX, YOLO crop, pgvector search.
- `docker-compose.yml`: local PostgreSQL 16 with pgvector.

## Start Here After Clone

Prerequisites: Docker Desktop, JDK 21+, and CPython 3.12.x. Maven installation
is optional because `backend/` includes Maven Wrapper.

From the repository root:

```powershell
Copy-Item .env.example .env
python -m venv clip-service\.venv
.\clip-service\.venv\Scripts\python.exe -m pip install -r clip-service\requirements.txt
docker compose up -d postgres
```

Download local model files before starting CLIP:

```powershell
.\clip-service\.venv\Scripts\python.exe scripts\download_models.py
```

Start backend and CLIP in two separate terminals:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

```powershell
.\scripts\windows\start-clip.bat
```

| Service | URL |
| --- | --- |
| Backend Swagger | `http://localhost:8080/swagger-ui.html` |
| Backend health | `http://localhost:8080/api/system/health` |
| CLIP Swagger | `http://localhost:8000/docs` |
| CLIP health | `http://localhost:8000/api/clip/health` |
| PostgreSQL for pgAdmin | `127.0.0.1:5433` |

## Repository Layout

```text
.
|-- backend/            Spring Boot source
|-- clip-service/       Python CLIP source
|-- docs/               Team documentation
|-- scripts/
|   |-- download_models.py
|   `-- windows/        Optional CLIP startup shortcut
|-- .env.example        Shareable local configuration template
`-- docker-compose.yml  Local PostgreSQL
```

Flyway is the only schema owner. Hibernate uses `ddl-auto=validate`; it must
not create or alter tables.

## Documentation

- [Clone, run, test, and continue backend development](docs/DEVELOPMENT.md)
- [Backend Clean Architecture](docs/ARCHITECTURE.md)
- [Use the CLIP internal API](docs/CLIP_API.md)
