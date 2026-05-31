# Architecture

## Components

```text
Client
  |
  v
Spring Boot backend -----> FastAPI CLIP service
  |                             |
  +-------------+---------------+
                |
                v
       PostgreSQL 16 + pgvector
```

The Spring Boot backend owns business APIs and Flyway migrations. The CLIP
service encodes text and images and stores vectors in `clip_embeddings`.

## Backend Clean Architecture

The application class stays at the root package so Spring Boot scans the
project without scanning unrelated libraries:

```text
com.sba301.lostandfound
|-- LostAndFoundApplication.java
|-- domain/
|   `-- model/
|-- application/
|   |-- port/
|   |   |-- in/
|   |   `-- out/
|   |-- result/
|   `-- service/
|-- infrastructure/
|   |-- config/
|   |-- integration/
|   `-- persistence/
|       |-- entity/
|       `-- repository/
`-- presentation/
    `-- rest/
```

Dependency direction:

```text
presentation ------> application <------ infrastructure
                           |
                           v
                        domain
```

Rules:

- `domain/`: pure business types. No Spring, JPA, HTTP, or database imports.
- `application/`: use cases and ports. No Spring, JPA, HTTP, or database
  imports.
- `infrastructure/`: framework adapters such as JPA entities, Spring Data
  repositories, JDBC checks, FastAPI HTTP clients, and Spring configuration.
- `presentation/`: REST controllers and HTTP request/response DTOs. Controllers
  call input ports, not infrastructure classes.

## Adding a Feature

Example for post creation:

```text
domain/model/Post.java
application/port/in/CreatePostUseCase.java
application/port/out/SavePostPort.java
application/service/CreatePostService.java
infrastructure/persistence/entity/PostJpaEntity.java
infrastructure/persistence/repository/PostJpaRepository.java
infrastructure/persistence/adapter/PostPersistenceAdapter.java
presentation/rest/post/CreatePostRequest.java
presentation/rest/post/PostController.java
```

Do not expose JPA entities from controllers. Map persistence entities to domain
models inside an infrastructure adapter.

## Database Ownership

There is one schema mechanism:

1. Docker starts the `pgvector/pgvector:pg16` PostgreSQL image.
2. Backend startup runs Flyway migrations from `classpath:db/migration`.
3. Migration `V1__create_initial_schema.sql` enables the `vector` extension and
   creates the application schema.
4. Hibernate validates mappings using `ddl-auto=validate`.

Do not add a second schema initializer such as root `init-db.sql`,
`schema.sql`, or Hibernate `ddl-auto=update`.

## CLIP Flow

- Lost post text: encode text, store a `TEXT` vector, search `IMAGE` vectors.
- Found post image: crop with YOLO, encode image, store an `IMAGE` vector,
  search `TEXT` vectors.
- PostgreSQL stores vectors as `vector(768)` and searches cosine similarity.
