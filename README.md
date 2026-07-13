# 智链中枢

[中文说明](README.zh-CN.md)

智链中枢 is a backend service and web admin system migrated from the non-kitchen modules of the local `saas` project. The first version focuses on server-side APIs and an admin web console. Mobile clients and desktop clients are intentionally not included.

## Project Entry Points

- [Migration analysis](docs/migration-analysis.md)
- [Development guide](docs/development.md)
- [Database notes](docs/database.md)
- [AI service and MinerU integration](docs/ai-service.md)
- [AI service architecture](docs/architecture/ai-service-architecture.drawio.png)

## Structure

```text
智链中枢/
├── server/       # Spring Boot backend service
├── web-admin/    # Vue 3 + Vite + Ant Design Vue admin console
├── ai-service/   # Python 3.11 internal AI capability service
├── docs/         # Migration, development, and database notes
└── assets/       # Shared project assets
```

## Local Development

Start the backend service:

```bash
cd server
mvn spring-boot:run
```

Start the web admin console:

```bash
cd web-admin
npm install
npm run dev
```

Start the optional MinerU stack after configuring `ai-service/.env`:

```bash
cd ai-service
cp .env.example .env
docker compose up --build
```

The backend listens on `http://localhost:8080` by default. The web admin dev server listens on `http://localhost:3000` and proxies `/api` requests to the backend.

Default admin login:

```text
Account: admin
Password: 123456
```

## Features

- Base organization, role, user, and file management.
- AI chat with streaming response support.
- Document upload, asynchronous MinerU OCR/layout parsing, chunking, sparse indexing, and vector indexing.
- Knowledge base category and document management.
- OnlyOffice preview and editing integration for knowledge documents.
- MinIO-backed document storage.
- Qdrant vector store and Elasticsearch sparse search configuration.
- Aliyun outbound bot API connector retained from the source project.

## Current Boundaries

- Kitchen management, recipe management, date plans, feedback pages, and `/api/suisui/action` are excluded.
- Mobile and desktop clients are not included in the first version.
- Runtime credentials and service addresses are injected through environment variables; no production secrets belong in the repository.
- External services such as MySQL, Redis, MinIO, OnlyOffice, Ollama, Qdrant, Elasticsearch, and Aliyun outbound bot must be available separately.
- The default parser remains `LEGACY`; set `AI_DOCUMENT_PARSE_STRATEGY=AUTO` only after the AI service and MinerU health checks pass.

## Technology Stack

- Java 21
- Spring Boot 3.5.x
- MyBatis
- MySQL
- Redis
- MinIO
- Spring AI, Qdrant, Elasticsearch
- Python 3.11, FastAPI, MinerU 3.4.4
- Vue 3
- Vite
- Ant Design Vue
- Axios
