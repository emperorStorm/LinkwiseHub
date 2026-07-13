# 智链中枢 迁移分析

## 迁移来源

- 结构参考：`/Users/wangjun/Project/local/Coscool`
- 后端来源：`/Users/wangjun/Project/local/saas/oa-application`
- Web 后台来源：`/Users/wangjun/Project/local/saas/oa-web`
- README 结构参考：`/Users/wangjun/Project/local/sui-frame/README.md`

## 迁移范围

已迁移模块：

- 基础管理：组织机构、角色、用户。
- 文件管理：文件列表、删除和静态资源预览。
- AI 问答：会话、消息、普通问答和 SSE 流式问答。
- 文档处理：上传、解析、分片、稀疏索引、向量索引。
- 知识库：分类、文档、附件、发布状态、OnlyOffice 预览和回调。
- API 连接器：阿里云智能外呼机器人接口模块。
- AI 能力服务：新增独立 Python `ai-service`，通过 MinerU 异步任务完成 OCR、版面和结构化解析。

明确排除模块：

- 厨房管理。
- 菜谱分类、菜谱、每日菜单和意见反馈。
- 小程序兼容接口 `/api/suisui/action`。
- 厨房文件上传和厨房静态文件接口。

## 命名调整

- Java 包名：`com.example.oa` 改为 `com.linkwisehub`。
- 启动类：`OaApplication` 改为 `LinkwiseHubApplication`。
- Maven 坐标：`artifactId` 改为 `linkwisehub-server`，`groupId` 改为 `com.linkwisehub`。
- Web 包名：`oa-frontend` 改为 `linkwisehub-web-admin`。
- 数据库表前缀：`oa_` 改为 `lwh_`。
- 权限表：`permission` 改为 `lwh_permission`。

## 配置迁移

`server/src/main/resources/application.yml` 已保留源项目中的真实连接和密钥配置，包括：

- MySQL、Redis。
- OnlyOffice。
- MinIO。
- DeepSeek、百炼、MiniMax、Ollama。
- Qdrant、Elasticsearch。
- 阿里云智能外呼机器人。

数据库库名按当前项目命名为 `lwh_system`，表结构按 `lwh_` 前缀初始化。

## 风险与后续关注

- 需要先创建 `lwh_system` 数据库，否则后端无法初始化表。
- 外部依赖服务如果不可用，基础接口可以启动，但 AI、文档、OnlyOffice、向量检索和外呼能力会受影响。
- MinerU未部署时保持 `LEGACY` 解析策略，不影响现有 Java 文档解析链路。
- 源项目登录仍是前端本地模拟登录，尚未接入真实鉴权。
- 文件管理预览改为 `/uploads/{fileName}`，依赖后端 `file.upload.path` 目录中存在对应文件。
