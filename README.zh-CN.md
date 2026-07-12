# LinkwiseHub

[English](README.md)

智链中枢 是从本地 `saas` 项目中迁移出来的后端服务和 Web 后台管理系统。首版只做后端接口和后台控制网页，不包含移动端和桌面客户端。

## 项目入口

- [迁移分析文档](docs/migration-analysis.md)
- [开发启动说明](docs/development.md)
- [数据库说明](docs/database.md)

## 目录

```text
LinkwiseHub/
├── server/       # Spring Boot 后端服务
├── web-admin/    # Vue 3 + Vite + Ant Design Vue 后台控制网页
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

后端默认地址为 `http://localhost:8080`。Web 后台开发服务默认地址为 `http://localhost:3000`，并把 `/api` 请求代理到后端。

默认后台账号：

```text
账号：admin
密码：123456
```

## 当前能力

- 组织机构、角色、用户和文件管理。
- AI 智能问答，支持流式响应。
- 文档上传、解析、分片、稀疏索引和向量索引。
- 知识库分类和知识文档管理。
- OnlyOffice 知识文档预览和编辑。
- 基于 MinIO 的文档存储。
- Qdrant 向量库和 Elasticsearch 稀疏检索配置。
- 保留源项目中的阿里云智能外呼接口模块。

## 当前边界

- 不迁移厨房管理、菜谱管理、每日菜单、意见反馈和 `/api/suisui/action`。
- 首版不包含移动端和桌面客户端。
- 已按要求把运行配置值迁移到 `server/src/main/resources/application.yml`。
- MySQL、Redis、MinIO、OnlyOffice、Ollama、Qdrant、Elasticsearch、阿里云外呼等外部服务需要单独可用。

## 技术栈

- Java 21
- Spring Boot 3.5.x
- MyBatis
- MySQL
- Redis
- MinIO
- Spring AI、Qdrant、Elasticsearch
- Vue 3
- Vite
- Ant Design Vue
- Axios
