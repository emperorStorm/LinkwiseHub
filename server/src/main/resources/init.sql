-- ===============================================
-- 智链中枢 - 完整数据库表结构
-- ===============================================
-- 所有表使用 CREATE TABLE IF NOT EXISTS，可安全重复执行。
-- 索引定义在表内直接声明，避免重复执行报错。
-- ===============================================

-- ==================== 基础模块 ====================

-- 1. 组织机构表
CREATE TABLE IF NOT EXISTS lwh_base_organization (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '组织机构名称',
    parent_id BIGINT DEFAULT NULL COMMENT '父组织机构ID',
    level INT DEFAULT 1 COMMENT '组织机构级别',
    status INT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织机构表';

-- 2. 角色表
CREATE TABLE IF NOT EXISTS lwh_base_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(50) NOT NULL COMMENT '角色名称',
    code VARCHAR(50) NOT NULL COMMENT '角色编码',
    description VARCHAR(200) DEFAULT NULL COMMENT '角色描述',
    status INT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 3. 权限表
CREATE TABLE IF NOT EXISTS lwh_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(50) NOT NULL COMMENT '权限名称',
    code VARCHAR(50) NOT NULL COMMENT '权限编码',
    description VARCHAR(200) DEFAULT NULL COMMENT '权限描述',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- 4. 用户表
CREATE TABLE IF NOT EXISTS lwh_base_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    organization_id BIGINT DEFAULT NULL COMMENT '所属组织机构ID',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    phone VARCHAR(20) DEFAULT NULL COMMENT '电话',
    status INT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_organization (organization_id),
    INDEX idx_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 5. 用户角色关联表
CREATE TABLE IF NOT EXISTS lwh_base_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 6. 角色权限关联表
CREATE TABLE IF NOT EXISTS lwh_base_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_role_permission (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- 7. 文件信息表
CREATE TABLE IF NOT EXISTS lwh_base_file (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_name VARCHAR(100) NOT NULL COMMENT '存储文件名(UUID)',
    file_suffix VARCHAR(20) COMMENT '文件后缀',
    file_size BIGINT COMMENT '文件大小(字节)',
    content_type VARCHAR(100) COMMENT '文件MIME类型',
    file_path VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    module VARCHAR(50) NOT NULL COMMENT '所属模块(base/ai等)',
    status INT DEFAULT 1 COMMENT '状态：1-正常，0-删除',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_file_module (module),
    INDEX idx_file_status (status),
    INDEX idx_file_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件信息表';

-- ==================== AI 智能问答模块 ====================

-- 8. AI 会话表
CREATE TABLE IF NOT EXISTS lwh_ai_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    title VARCHAR(255) DEFAULT '新对话' COMMENT '会话标题',
    user_id BIGINT COMMENT '用户ID',
    status INT DEFAULT 1 COMMENT '状态：1-正常，0-删除',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_conversation_user (user_id),
    INDEX idx_conversation_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI会话表';

-- 9. AI 消息表
CREATE TABLE IF NOT EXISTS lwh_ai_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    conversation_id BIGINT NOT NULL COMMENT '会话ID',
    role VARCHAR(20) NOT NULL COMMENT '角色：user/assistant/system',
    content TEXT NOT NULL COMMENT '消息内容',
    model VARCHAR(80) DEFAULT NULL COMMENT '本条AI消息使用的模型标识',
    model_name VARCHAR(120) DEFAULT NULL COMMENT '本条AI消息展示的模型名称',
    elapsed_ms BIGINT DEFAULT NULL COMMENT '本条AI回复耗时，单位毫秒',
    status INT DEFAULT 1 COMMENT '状态：1-正常，0-删除',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_message_conversation (conversation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI消息表';

-- 10. AI 会话摘要表
CREATE TABLE IF NOT EXISTS lwh_ai_conversation_summary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '摘要ID',
    conversation_id BIGINT NOT NULL COMMENT '会话ID',
    summary_content TEXT NOT NULL COMMENT '摘要内容',
    covered_message_id BIGINT DEFAULT 0 COMMENT '摘要已覆盖的最大消息ID',
    status INT DEFAULT 1 COMMENT '状态：1-正常，0-删除',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_summary_conversation (conversation_id),
    INDEX idx_summary_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI会话摘要表';

-- 11. AI 文档表
CREATE TABLE IF NOT EXISTS lwh_ai_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文档ID',
    category_id BIGINT DEFAULT NULL COMMENT '知识库分类ID',
    title VARCHAR(120) DEFAULT NULL COMMENT '知识文档标题',
    file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_type VARCHAR(20) NOT NULL COMMENT '文件类型',
    file_size BIGINT NOT NULL COMMENT '文件大小',
    storage_bucket VARCHAR(100) NOT NULL DEFAULT 'knowledge-base' COMMENT 'MinIO存储桶',
    storage_path VARCHAR(500) NOT NULL COMMENT 'MinIO对象Key',
    parse_status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING' COMMENT '解析状态：PROCESSING/SUCCESS/FAILED',
    chunk_count INT NOT NULL DEFAULT 0 COMMENT 'Chunk数量',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
    content_html MEDIUMTEXT DEFAULT NULL COMMENT '知识文档富文本内容',
    publish_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '发布状态：DRAFT/PUBLISHED',
    source_type VARCHAR(20) DEFAULT NULL COMMENT '来源类型：CONTENT/FILE/MIXED',
    status INT NOT NULL DEFAULT 1 COMMENT '状态：1-正常，0-删除',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_ai_document_category (category_id),
    INDEX idx_ai_document_publish_status (publish_status),
    INDEX idx_ai_document_source_type (source_type),
    INDEX idx_ai_document_status (status),
    INDEX idx_ai_document_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI文档表';

-- 11. AI 文档 Chunk 表
CREATE TABLE IF NOT EXISTS lwh_ai_document_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Chunk ID',
    document_id BIGINT NOT NULL COMMENT '文档ID',
    chunk_index INT NOT NULL COMMENT 'Chunk序号',
    content TEXT NOT NULL COMMENT 'Chunk内容',
    content_length INT NOT NULL COMMENT '内容长度',
    source_title VARCHAR(255) DEFAULT NULL COMMENT '来源标题',
    source_page INT DEFAULT NULL COMMENT '来源页码',
    source_paragraph INT DEFAULT NULL COMMENT '来源段落',
    metadata_json TEXT DEFAULT NULL COMMENT '扩展元数据',
    vector_id VARCHAR(120) DEFAULT NULL COMMENT 'Qdrant向量ID',
    vector_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '向量状态：PENDING/SUCCESS/FAILED',
    vector_error_message VARCHAR(1000) DEFAULT NULL COMMENT '向量写入错误信息',
    status INT NOT NULL DEFAULT 1 COMMENT '状态：1-正常，0-删除',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_ai_chunk_document (document_id),
    UNIQUE KEY uk_ai_chunk_vector_id (vector_id),
    UNIQUE KEY uk_ai_chunk_document_index (document_id, chunk_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI文档Chunk表';

-- 12. AI 文档分片配置表
CREATE TABLE IF NOT EXISTS lwh_ai_document_split_config (
    id BIGINT PRIMARY KEY COMMENT '配置ID，全局配置固定为1',
    target_chunk_length INT NOT NULL DEFAULT 800 COMMENT '目标Chunk长度',
    split_by_blank_line TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否优先按空行/段落切分',
    preserve_markdown_title TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否保留Markdown标题作为来源标题',
    status INT NOT NULL DEFAULT 1 COMMENT '状态：1-正常，0-删除',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI文档分片配置表';

-- AI 文档异步处理任务表
CREATE TABLE IF NOT EXISTS lwh_ai_processing_job (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    document_id BIGINT NOT NULL COMMENT '文档ID',
    task_type VARCHAR(40) NOT NULL DEFAULT 'DOCUMENT_PARSE' COMMENT '任务类型',
    parse_engine VARCHAR(20) NOT NULL COMMENT '解析引擎：LEGACY/MINERU/AUTO',
    provider_task_id VARCHAR(120) DEFAULT NULL COMMENT 'MinerU任务ID',
    status VARCHAR(30) NOT NULL COMMENT '任务状态',
    progress INT NOT NULL DEFAULT 0 COMMENT '进度百分比',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    result_bucket VARCHAR(100) DEFAULT NULL COMMENT '结果存储桶',
    manifest_path VARCHAR(500) DEFAULT NULL COMMENT '结果清单路径',
    markdown_path VARCHAR(500) DEFAULT NULL COMMENT 'Markdown路径',
    blocks_path VARCHAR(500) DEFAULT NULL COMMENT '标准化块路径',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
    next_poll_time TIMESTAMP NULL DEFAULT NULL COMMENT '下次轮询时间',
    locked_until TIMESTAMP NULL DEFAULT NULL COMMENT '任务锁过期时间',
    worker_id VARCHAR(80) DEFAULT NULL COMMENT '处理实例ID',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    started_at TIMESTAMP NULL DEFAULT NULL COMMENT '开始时间',
    finished_at TIMESTAMP NULL DEFAULT NULL COMMENT '结束时间',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_ai_job_document (document_id),
    INDEX idx_ai_job_poll (status, next_poll_time),
    INDEX idx_ai_job_provider_task (provider_task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI文档处理任务表';

-- 13. AI 知识库分类表
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
