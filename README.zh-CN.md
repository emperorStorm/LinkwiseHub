# 智链中枢

[English](README.md)

智链中枢 是从本地 `saas` 项目中迁移出来的后端服务和 Web 后台管理系统。首版只做后端接口和后台控制网页，不包含移动端和桌面客户端。

## 项目入口

- [迁移分析文档](docs/migration-analysis.md)
- [开发启动说明](docs/development.md)
- [数据库说明](docs/database.md)
- [AI 服务与 MinerU 接入说明](docs/ai-service.md)
- [AI 服务架构图](docs/architecture/ai-service-architecture.drawio.png)

## 目录

```text
智链中枢/
├── server/       # Spring Boot 后端服务
├── web-admin/    # Vue 3 + Vite + Ant Design Vue 后台控制网页
├── ai-service/   # Python 3.11 内部 AI 能力服务
├── docs/         # 迁移、开发和数据库说明
└── assets/       # 公共项目资源
```

## 本地启动

启动后端服务：

```bash
cd server
mvn spring-boot:run
```

启动 Web 后台：

```bash
cd web-admin
npm install
npm run dev
```

配置 `ai-service/.env` 后可启动 MinerU 与 AI 服务：

```bash
cd ai-service
cp .env.example .env
docker compose up --build
```

后端默认地址为 `http://localhost:8080`。Web 后台开发服务默认地址为 `http://localhost:3000`，并把 `/api` 请求代理到后端。

默认后台账号：

```text
账号：admin
密码：123456
```

## 当前能力

- 组织机构、角色、用户和文件管理。
- AI 智能问答，支持流式响应。
- 文档上传、MinerU 异步 OCR/版面解析、分片、稀疏索引和向量索引。
- 知识库分类和知识文档管理。
- OnlyOffice 知识文档预览和编辑。
- 基于 MinIO 的文档存储。
- Qdrant 向量库和 Elasticsearch 稀疏检索配置。
- 保留源项目中的阿里云智能外呼接口模块。

## 当前边界

- 不迁移厨房管理、菜谱管理、每日菜单、意见反馈和 `/api/suisui/action`。
- 首版不包含移动端和桌面客户端。
- 开发环境的中间件默认连接信息已同步 OA 配置，仍可通过环境变量覆盖；外部模型密钥按环境变量注入。
- MySQL、Redis、MinIO、OnlyOffice、Ollama、Qdrant、Elasticsearch、阿里云外呼等外部服务需要单独可用。
- 新上传文档默认使用 `AUTO`：PDF、现代 Office 文档和图片使用 MinerU；TXT、Markdown 和旧版 Office 文档使用 Java 兼容解析。启用上传前需完成 AI 服务、MinIO 和共享服务令牌配置。

## 技术栈

- Java 21
- Spring Boot 3.5.x
- MyBatis
- MySQL
- Redis
- MinIO
- Spring AI、Qdrant、Elasticsearch
- Python 3.11、FastAPI、MinerU 3.4.4
- Vue 3
- Vite
- Ant Design Vue
- Axios
