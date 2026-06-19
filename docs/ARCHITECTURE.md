# System Architecture

## Architecture Overview

The SBA301 Lost and Found system uses a microservice-oriented layout combining a Java/Spring Boot business backend, a Python/FastAPI AI CLIP service, and a PostgreSQL database with pgvector extensions.

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

---

## 3-Tier Layered Architecture

The Java backend follows a standard, highly organized **3-Tier Layered Architecture** (Controller-Service-Repository). This pattern enforces separation of concerns, simplifies testing, and keeps dependencies clean.

### Package Layout

```text
com.sba301.lostandfound
|-- LostAndFoundApplication.java   (Main startup entry point)
|-- controller/                     (REST controllers, API endpoints)
|-- service/                        (Business logic interfaces & implementations)
|-- repository/                     (Spring Data JPA repositories for DB access)
|-- entity/                         (JPA persistence entities mapped to database tables)
|-- dto/                            (Data Transfer Objects for request/response serialization)
|-- client/                         (Feign/RestClient integrations to external services like CLIP)
|-- security/                       (JWT filters, Token providers, Cookie extractors)
|-- config/                         (Security configuration, CORS, general bean declarations)
`-- scheduler/                      (Background cron jobs, e.g., expired token cleanup)
```

### Dependency Flow

Dependencies flow unidirectional from the presentation layer downwards to the database access layer:

```text
[ Controller (presentation) ]
             |
             v
  [ Service (business) ]
             |
             v
[ Repository (persistence) ] <---> [ Entity ]
```

* **Controller Layer**: Handles incoming HTTP requests, validates input parameters, and returns standardized unified responses (`ApiResponse<T>`).
* **Service Layer**: Implements business transactions, orchestrates operations between multiple repositories, and calls the external `ClipClient` when AI vectorization is needed.
* **Repository Layer**: Provides abstraction over database queries using Spring Data JPA.
* **Entity Layer**: Represents the database schema models.

---

## Database Schema & Migrations

Database structure is managed explicitly through **Flyway**:

1. **Flyway Migration Owner**: All database tables, extensions (like `vector`), and constraints are created via versioned SQL scripts in `src/main/resources/db/migration/`.
2. **JPA Validation**: Hibernate is configured with `ddl-auto=validate`. Hibernate is strictly forbidden from creating or modifying tables at runtime to avoid schema drift.
3. **Adding Schema Changes**:
   * Add a new SQL file under `db/migration/` (e.g., `V4__add_new_table.sql`).
   * Create/update the matching JPA `@Entity` class in `com.sba301.lostandfound.entity`.
   * Restart the application to apply the migration automatically.

---

## AI CLIP Search Flow

1. **Lost Item Text Indexing**: When a user posts a lost item, Spring Boot extracts the description text, calls the FastAPI CLIP service to encode it into a `TEXT` embedding, and stores the 768-dimensional vector in `clip_embeddings`.
2. **Found Item Image Indexing**: When a found item image is posted, the image is cropped via YOLOv8, encoded to an `IMAGE` embedding by the CLIP service, and stored in PostgreSQL.
3. **Similarity Search**: Cosine similarity matches are computed inside PostgreSQL using `pgvector` to identify and link corresponding lost and found items.
