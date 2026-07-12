# 开发启动说明

## 环境要求

- JDK 21
- Maven 3.9+
- Node.js 18+
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

## 默认账号

```text
账号：admin
密码：123456
```

当前 Web 后台登录是前端模拟登录，用于本地管理端访问控制。
