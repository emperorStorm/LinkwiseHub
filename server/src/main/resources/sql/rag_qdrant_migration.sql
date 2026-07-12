-- Spring AI Alibaba + Qdrant RAG 索引迁移脚本
-- 需要在已有环境执行一次；本地开发环境也会随 spring.sql.init 自动尝试执行。

ALTER TABLE lwh_ai_document_chunk
    ADD COLUMN vector_id VARCHAR(120) DEFAULT NULL COMMENT 'Qdrant向量ID';

ALTER TABLE lwh_ai_document_chunk
    ADD COLUMN vector_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '向量状态：PENDING/SUCCESS/FAILED';

ALTER TABLE lwh_ai_document_chunk
    ADD COLUMN vector_error_message VARCHAR(1000) DEFAULT NULL COMMENT '向量写入错误信息';

ALTER TABLE lwh_ai_document_chunk
    ADD UNIQUE KEY uk_ai_chunk_vector_id (vector_id);

-- 智能问答消息留痕字段，保留每条AI回复实际使用的模型和耗时。
ALTER TABLE lwh_ai_message
    ADD COLUMN model VARCHAR(80) DEFAULT NULL COMMENT '本条AI消息使用的模型标识';

ALTER TABLE lwh_ai_message
    ADD COLUMN model_name VARCHAR(120) DEFAULT NULL COMMENT '本条AI消息展示的模型名称';

ALTER TABLE lwh_ai_message
    ADD COLUMN elapsed_ms BIGINT DEFAULT NULL COMMENT '本条AI回复耗时，单位毫秒';
