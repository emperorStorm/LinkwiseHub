# LinkwiseHub AI Service

LinkwiseHub 内部 AI 能力服务。首期封装虚拟机上 MinerU 3.4.4 的异步文档解析接口，负责服务鉴权、MinIO 文件读取、解析结果标准化和结果回写，不直接访问 LinkwiseHub MySQL、Qdrant 或 Elasticsearch。

## 本地运行

```bash
cp .env.example .env
docker compose up --build
```

AI 服务默认监听 `8090`，通过 `MINERU_BASE_URL` 访问虚拟机 `10.211.55.6:8000`。生产环境必须替换 `.env` 中的 AI 服务令牌、MinerU API token 和 MinIO 凭据。

`MINERU_IMAGE`、`CADDY_IMAGE`、模型来源等变量用于记录虚拟机 MinerU 部署参数；当前 Compose 不会在本机重复启动 MinerU 或 Caddy。

## 接口

- `POST /internal/v1/document-parses`
- `GET /internal/v1/document-parses/{task_id}`
- `POST /internal/v1/document-parses/{task_id}/materialize`
- `GET /health/live`
- `GET /health/ready`

内部接口要求请求头 `X-Service-Token`，并支持使用 `X-Request-Id` 串联 Java、Python 和 MinerU 日志。
