# 数据库说明

## 数据库

默认数据库名：

```text
lwh_system
```

初始化脚本：

```text
server/src/main/resources/sql/schema.sql
server/src/main/resources/sql/rag_qdrant_migration.sql
server/src/main/resources/sql/data.sql
```

Spring Boot 启动时会按 `application.yml` 中的 `spring.sql.init.schema-locations` 和 `data-locations` 执行。

## 表前缀

LinkwiseHub 使用 `lwh_` 作为业务表前缀。

## 核心表

基础管理：

- `lwh_base_organization`
- `lwh_base_role`
- `lwh_permission`
- `lwh_base_user`
- `lwh_base_user_role`
- `lwh_base_role_permission`
- `lwh_base_file`

AI 与知识库：

- `lwh_ai_conversation`
- `lwh_ai_message`
- `lwh_ai_conversation_summary`
- `lwh_ai_document`
- `lwh_ai_document_chunk`
- `lwh_ai_document_split_config`
- `lwh_ai_knowledge_category`

## 已排除表

厨房相关表不再初始化，包括原项目中的用户、厨房、成员、分类、菜谱、用料、步骤、日期计划和反馈表。
