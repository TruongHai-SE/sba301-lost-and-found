# SBA301 Lost and Found System

An AI-powered, production-grade Lost & Found platform designed to match lost items with found items. The system leverages multimodal embeddings (image-to-text, text-to-image) using a local **CLIP model** and **YOLOv8** object detection to calculate similarity matches over a vector database.

---

## Key Features

* **Multimodal AI Similarity Search**: Matches lost items with found items by embedding text descriptions and images into the same vector space using OpenAI's **CLIP model** (optimized via ONNX Runtime).
* **Automated Object Detection & Cropping**: Uses **YOLOv8** to automatically identify, locate, and crop items within images, ensuring the CLIP model encodes only the relevant object, reducing background noise.
* **Vector Database Integration**: Uses **PostgreSQL 16** with the **`pgvector`** extension to perform fast high-dimensional cosine similarity searches over 768-dimensional embeddings.
* **3-Tier Layered Architecture**: A clean, scalable backend built with **Spring Boot 4.0** and **Java 21**, implementing clear separation of concerns (Controller, Service, Repository).
* **Stateless JWT & Google OAuth2 Authentication**: Safe, token-based authentication system featuring access tokens, secure HTTP-only refresh cookies, and Google Social Login.
* **Unified API Responses & Exception Handling**: Centralized exception handling that translates all validation, security, and runtime errors into clean, standardized API response JSON formats.

---

## Technology Stack

| Layer | Technologies & Frameworks |
| --- | --- |
| **Java Backend** | Java 21, Spring Boot 4.0, Spring Security (JWT), Spring Data JPA (Hibernate), Flyway Migrations, Lombok, Swagger/OpenAPI |
| **AI Microservice** | Python 3.12, FastAPI, Optimum (ONNX export), CLIP (Transformers), YOLOv8 (Ultralytics), pgvector-python |
| **Database & Cloud** | PostgreSQL 16 (with `pgvector`), Cloudinary (secure cloud image hosting), Docker Compose |

---

## System Design

The system runs as two cooperating microservices to isolate business logic from heavy AI computation:

```text
Client (Frontend)
      |
      v
Spring Boot Backend (Port 8080) -------> FastAPI CLIP Service (Port 8000)
      |                                           |
      +---------------------+---------------------+
                            |
                            v
                 PostgreSQL 16 + pgvector
```

1. **Spring Boot Backend**: Serves as the primary entry point. Handles user authentication, posts CRUD, mail notifications, database migrations via Flyway, and orchestrates calls to the CLIP service.
2. **FastAPI CLIP Service**: Exposes internal endpoints to crop images with YOLO and compute embeddings with CLIP.
3. **Vector Database**: Stored embeddings are matched inside PostgreSQL using cosine distance SQL queries.

---

## Project Organization

```text
.
├── backend/            # Spring Boot backend source (Layered: Controller -> Service -> Repository)
├── clip-service/       # Python/FastAPI AI CLIP service source
├── docs/               # Technical and system documentation
├── scripts/            # Setup and model helper scripts
├── docker-compose.yml  # Docker environment for PostgreSQL + pgvector
└── .env.example        # Environment variables template
```

---

## Technical Documentation

For details on configuration, database schemas, and running the services, please refer to the following documents:

* **[Getting Started & Development Guide](docs/DEVELOPMENT.md)**: Steps to clone, set up your local environment, install dependencies, run tests, and start daily development.
* **[System & Database Architecture](docs/ARCHITECTURE.md)**: Detailed package layout, Flyway migration rules, and the AI matching query flow.
* **[CLIP Service API Reference](docs/CLIP_API.md)**: Specifications for the internal Python CLIP microservice endpoints.
