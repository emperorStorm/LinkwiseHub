-- AI 知识库功能迁移脚本
-- 适用于已有 lwh_ai_document 表的环境；新库可直接执行 schema.sql。

ALTER TABLE lwh_ai_document
    ADD COLUMN category_id BIGINT DEFAULT NULL COMMENT '知识库分类ID',
    ADD COLUMN title VARCHAR(120) DEFAULT NULL COMMENT '知识文档标题',
    ADD COLUMN content_html MEDIUMTEXT DEFAULT NULL COMMENT '知识文档富文本内容',
    ADD COLUMN publish_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '发布状态：DRAFT/PUBLISHED',
    ADD COLUMN source_type VARCHAR(20) DEFAULT NULL COMMENT '来源类型：CONTENT/FILE/MIXED',
    ADD INDEX idx_ai_document_category (category_id),
    ADD INDEX idx_ai_document_publish_status (publish_status),
    ADD INDEX idx_ai_document_source_type (source_type);

CREATE TABLE IF NOT EXISTS lwh_ai_knowledge_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父级分类ID，0表示根级',
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    status INT NOT NULL DEFAULT 1 COMMENT '状态：1-正常，0-删除',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_ai_knowledge_category_parent (parent_id),
    INDEX idx_ai_knowledge_category_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI知识库分类表';

INSERT IGNORE INTO lwh_ai_knowledge_category (id, parent_id, name, sort, status) VALUES
(1, 0, '默认知识库', 1, 1),
(2, 1, '制度文档', 1, 1),
(3, 1, '流程说明', 2, 1);
