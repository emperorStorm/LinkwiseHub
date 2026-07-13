# 开发启动说明

## 环境要求

- JDK 21
- Maven 3.9+
- Node.js 18+
- Python 3.11（AI 服务）
- Docker / Docker Compose（MinerU 私有部署）
- MySQL
- Redis
- MinIO
- OnlyOffice Document Server
- Ollama、Qdrant、Elasticsearch

AI、文档和外呼相关能力依赖外部服务。只验证基础管理时，可以先保证 MySQL 和 Redis 可用。

## 后端启动

1. 创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS lwh_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 启动服务：

```bash
cd server
mvn spring-boot:run
```

3. 常用地址：

```text
健康检查：http://localhost:8080/actuator/health
接口文档：http://localhost:8080/swagger-ui.html
```

## Web 后台启动

```bash
cd web-admin
npm install
npm run dev
```

开发服务默认运行在 `http://localhost:3000`，`/api` 请求会代理到 `http://localhost:8080`。

## AI 服务与 MinerU

```bash
cd ai-service
cp .env.example .env
docker compose up --build
```

AI 服务默认地址为 `http://localhost:8090`，MinerU 虚拟机地址为 `http://10.211.55.6:8000`。确认以下接口可用后，再把 Java 解析策略切换为 `AUTO`：

```text
http://localhost:8090/health/live
http://localhost:8090/health/ready
http://10.211.55.6:8000/health
```

Java 侧至少需要配置 `AI_SERVICE_TOKEN`，且必须与 `ai-service/.env` 一致。详细配置见 [AI 服务说明](ai-service.md)。

后端不在仓库中保存真实凭据。启动前按实际环境注入以下核心变量：

```text
MYSQL_URL / MYSQL_USERNAME / MYSQL_PASSWORD
REDIS_HOST / REDIS_PORT / REDIS_PASSWORD
MINIO_ENDPOINT / MINIO_ACCESS_KEY / MINIO_SECRET_KEY / MINIO_BUCKET_NAME
ONLYOFFICE_DOCUMENT_SERVER_URL / ONLYOFFICE_PUBLIC_BACKEND_URL / ONLYOFFICE_JWT_SECRET
QDRANT_HOST / QDRANT_GRPC_PORT / QDRANT_API_KEY
ELASTICSEARCH_URIS / ELASTICSEARCH_USERNAME / ELASTICSEARCH_PASSWORD
AI_DEEPSEEK_API_KEY / AI_DASHSCOPE_API_KEY / AI_MINIMAX_API_KEY
```

## 构建命令

```bash
cd server
mvn test
mvn -DskipTests package
```

```bash
cd web-admin
npm run build
```

```bash
cd ai-service
python3.11 -m venv .venv
.venv/bin/pip install -e '.[test]'
.venv/bin/pytest
.venv/bin/ruff check .
```

## 默认账号

```text
账号：admin
密码：123456
```

当前 Web 后台登录是前端模拟登录，用于本地管理端访问控制。
