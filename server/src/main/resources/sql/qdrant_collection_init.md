# Qdrant Collection 初始化说明

本项目默认使用 `spring.ai.vectorstore.qdrant.initialize-schema=true`，后端启动时会自动创建 Collection。

如果生产环境关闭自动初始化，需要提前创建：

- Collection：`lwh_document_chunks`
- 向量维度：`1024`
- 距离函数：`Cosine`
- gRPC 端口：`6334`
- HTTP 端口：`6333`

示例命令：

```bash
curl -X PUT http://localhost:6333/collections/lwh_document_chunks \
  -H 'Content-Type: application/json' \
  -d '{
    "vectors": {
      "size": 1024,
      "distance": "Cosine"
    }
  }'
```
