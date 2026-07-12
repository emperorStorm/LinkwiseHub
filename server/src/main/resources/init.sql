-- 创建数据库
CREATE DATABASE IF NOT EXISTS lwh_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE lwh_system;

-- 创建组织机构表
CREATE TABLE IF NOT EXISTS lwh_base_organization (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '组织机构名称',
    parent_id BIGINT DEFAULT NULL COMMENT '父组织机构ID',
    level INT DEFAULT 1 COMMENT '组织机构级别',
    status INT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织机构表';

-- 创建角色表
CREATE TABLE IF NOT EXISTS lwh_base_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL COMMENT '角色名称',
    code VARCHAR(50) NOT NULL COMMENT '角色编码',
    description VARCHAR(200) DEFAULT NULL COMMENT '角色描述',
    status INT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 创建用户表
CREATE TABLE IF NOT EXISTS lwh_base_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    organization_id BIGINT DEFAULT NULL COMMENT '所属组织机构ID',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    phone VARCHAR(20) DEFAULT NULL COMMENT '电话',
    status INT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 创建权限表
CREATE TABLE IF NOT EXISTS lwh_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL COMMENT '权限名称',
    code VARCHAR(50) NOT NULL COMMENT '权限编码',
    description VARCHAR(200) DEFAULT NULL COMMENT '权限描述',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- 创建用户角色关联表
CREATE TABLE IF NOT EXISTS lwh_base_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 创建角色权限关联表
CREATE TABLE IF NOT EXISTS lwh_base_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_role_permission (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- 插入基础数据
-- 组织机构
INSERT INTO lwh_base_organization (id, name, parent_id, level, status) VALUES
(1, '总公司', NULL, 1, 1),
(2, '技术部', 1, 2, 1),
(3, '市场部', 1, 2, 1),
(4, '财务部', 1, 2, 1);

-- 角色
INSERT INTO lwh_base_role (id, name, code, description, status) VALUES
(1, '超级管理员', 'super_admin', '系统超级管理员', 1),
(2, '技术主管', 'tech_manager', '技术部门主管', 1),
(3, '市场主管', 'market_manager', '市场部门主管', 1),
(4, '财务主管', 'finance_manager', '财务部门主管', 1),
(5, '普通员工', 'employee', '普通员工', 1);

-- 权限
INSERT INTO lwh_permission (id, name, code, description) VALUES
(1, '系统管理', 'system:manage', '系统管理权限'),
(2, '用户管理', 'user:manage', '用户管理权限'),
(3, '角色管理', 'role:manage', '角色管理权限'),
(4, '组织机构管理', 'organization:manage', '组织机构管理权限'),
(5, '查看数据', 'data:view', '查看数据权限');

-- 角色权限关联
INSERT INTO lwh_base_role_permission (role_id, permission_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5),
(2, 2), (2, 5),
(3, 2), (3, 5),
(4, 2), (4, 5),
(5, 5);

-- 用户（密码：123456，使用 BCrypt 加密）
INSERT INTO lwh_base_user (id, username, password, real_name, organization_id, email, phone, status) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '管理员', 1, 'admin@example.com', '13800138000', 1),
(2, 'tech_lead', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '技术主管', 2, 'tech_lead@example.com', '13800138001', 1),
(3, 'market_lead', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '市场主管', 3, 'market_lead@example.com', '13800138002', 1),
(4, 'finance_lead', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '财务主管', 4, 'finance_lead@example.com', '13800138003', 1),
(5, 'employee', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '普通员工', 2, 'employee@example.com', '13800138004', 1);

-- 用户角色关联
INSERT INTO lwh_base_user_role (user_id, role_id) VALUES
(1, 1),
(2, 2),
(3, 3),
(4, 4),
(5, 5);
