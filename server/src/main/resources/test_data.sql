-- LinkwiseHub测试数据脚本
-- 执行此脚本前请确保已执行 init.sql 创建了数据库表结构

USE lwh_system;

-- 清空现有数据（保留基础数据）
TRUNCATE TABLE lwh_base_user_role;
TRUNCATE TABLE lwh_base_role_permission;
TRUNCATE TABLE lwh_base_user;
TRUNCATE TABLE lwh_base_role;
TRUNCATE TABLE lwh_permission;
TRUNCATE TABLE lwh_base_organization;

-- ==================== 组织机构数据 ====================
INSERT INTO lwh_base_organization (id, name, parent_id, level, status) VALUES
(1, '集团总部', NULL, 1, 1),
(2, '技术研发中心', 1, 2, 1),
(3, '市场营销中心', 1, 2, 1),
(4, '财务管理中心', 1, 2, 1),
(5, '人力资源中心', 1, 2, 1),
(6, '行政服务中心', 1, 2, 1),
(7, '前端开发部', 2, 3, 1),
(8, '后端开发部', 2, 3, 1),
(9, '测试部', 2, 3, 1),
(10, '运维部', 2, 3, 1),
(11, '华北区销售部', 3, 3, 1),
(12, '华东区销售部', 3, 3, 1),
(13, '华南区销售部', 3, 3, 1),
(14, '会计部', 4, 3, 1),
(15, '审计部', 4, 3, 1);

-- ==================== 权限数据 ====================
INSERT INTO lwh_permission (id, name, code, description) VALUES
(1, '系统管理', 'system:manage', '系统管理权限，包含系统配置、日志管理等'),
(2, '用户管理', 'user:manage', '用户管理权限，包含用户的增删改查'),
(3, '角色管理', 'role:manage', '角色管理权限，包含角色的增删改查'),
(4, '组织机构管理', 'organization:manage', '组织机构管理权限，包含组织机构的增删改查'),
(5, '权限管理', 'permission:manage', '权限管理权限，包含权限的分配'),
(6, '数据查看', 'data:view', '数据查看权限，只能查看不能修改'),
(7, '数据导出', 'data:export', '数据导出权限，可以导出报表数据'),
(8, '审批管理', 'approval:manage', '审批管理权限，可以处理审批流程'),
(9, '公告管理', 'notice:manage', '公告管理权限，可以发布和管理公告'),
(10, '文档管理', 'document:manage', '文档管理权限，可以上传和管理文档');

-- ==================== 角色数据 ====================
INSERT INTO lwh_base_role (id, name, code, description, status) VALUES
(1, '超级管理员', 'super_admin', '系统超级管理员，拥有所有权限', 1),
(2, '系统管理员', 'system_admin', '系统管理员，负责系统日常维护', 1),
(3, '部门经理', 'dept_manager', '部门经理，管理本部门事务', 1),
(4, '人事专员', 'hr_specialist', '人事专员，负责人事相关事务', 1),
(5, '财务专员', 'finance_specialist', '财务专员，负责财务相关事务', 1),
(6, '普通员工', 'employee', '普通员工，基础权限', 1),
(7, '访客', 'guest', '访客角色，仅有查看权限', 1);

-- ==================== 角色权限关联 ====================
-- 超级管理员 - 所有权限
INSERT INTO lwh_base_role_permission (role_id, permission_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9), (1, 10);

-- 系统管理员 - 系统管理、用户管理、角色管理、数据查看
INSERT INTO lwh_base_role_permission (role_id, permission_id) VALUES
(2, 1), (2, 2), (2, 3), (2, 6);

-- 部门经理 - 用户管理、组织机构管理、数据查看、数据导出、审批管理
INSERT INTO lwh_base_role_permission (role_id, permission_id) VALUES
(3, 2), (3, 4), (3, 6), (3, 7), (3, 8);

-- 人事专员 - 用户管理、组织机构管理、数据查看
INSERT INTO lwh_base_role_permission (role_id, permission_id) VALUES
(4, 2), (4, 4), (4, 6);

-- 财务专员 - 数据查看、数据导出
INSERT INTO lwh_base_role_permission (role_id, permission_id) VALUES
(5, 6), (5, 7);

-- 普通员工 - 数据查看、公告查看
INSERT INTO lwh_base_role_permission (role_id, permission_id) VALUES
(6, 6);

-- 访客 - 仅数据查看
INSERT INTO lwh_base_role_permission (role_id, permission_id) VALUES
(7, 6);

-- ==================== 用户数据 ====================
-- 密码统一为: 123456 (BCrypt加密后的值)
INSERT INTO lwh_base_user (id, username, password, real_name, organization_id, email, phone, status) VALUES
-- 集团总部
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', 1, 'admin@oa.com', '13800000001', 1),
(2, 'zhangsan', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '张三', 1, 'zhangsan@oa.com', '13800000002', 1),
(3, 'lisi', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '李四', 1, 'lisi@oa.com', '13800000003', 1),

-- 技术研发中心
(4, 'wangwu', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '王五', 2, 'wangwu@oa.com', '13800000004', 1),
(5, 'zhaoliu', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '赵六', 2, 'zhaoliu@oa.com', '13800000005', 1),

-- 前端开发部
(6, 'sunqi', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '孙七', 7, 'sunqi@oa.com', '13800000006', 1),
(7, 'zhouba', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '周八', 7, 'zhouba@oa.com', '13800000007', 1),
(8, 'wujiu', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '吴九', 7, 'wujiu@oa.com', '13800000008', 1),

-- 后端开发部
(9, 'zhengshi', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '郑十', 8, 'zhengshi@oa.com', '13800000009', 1),
(10, 'chenxiao', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '陈晓', 8, 'chenxiao@oa.com', '13800000010', 1),

-- 测试部
(11, 'huangda', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '黄大', 9, 'huangda@oa.com', '13800000011', 1),
(12, 'liner', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '林二', 9, 'liner@oa.com', '13800000012', 1),

-- 市场营销中心
(13, 'liuqiang', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '刘强', 3, 'liuqiang@oa.com', '13800000013', 1),

-- 华东区销售部
(14, 'chenmei', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '陈美', 12, 'chenmei@oa.com', '13800000014', 1),
(15, 'zhangwei', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '张伟', 12, 'zhangwei@oa.com', '13800000015', 1),

-- 财务管理中心
(16, 'qianjing', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '钱静', 4, 'qianjing@oa.com', '13800000016', 1),

-- 会计部
(17, 'sunli', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '孙丽', 14, 'sunli@oa.com', '13800000017', 1),
(18, 'zhoufang', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '周芳', 14, 'zhoufang@oa.com', '13800000018', 1),

-- 人力资源中心
(19, 'wugang', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '吴刚', 5, 'wugang@oa.com', '13800000019', 1),

-- 行政服务中心
(20, 'zhenghua', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '郑华', 6, 'zhenghua@oa.com', '13800000020', 1);

-- ==================== 用户角色关联 ====================
-- admin - 超级管理员
INSERT INTO lwh_base_user_role (user_id, role_id) VALUES (1, 1);

-- 张三 - 系统管理员
INSERT INTO lwh_base_user_role (user_id, role_id) VALUES (2, 2);

-- 李四 - 部门经理
INSERT INTO lwh_base_user_role (user_id, role_id) VALUES (3, 3);

-- 王五 - 部门经理（技术研发中心）
INSERT INTO lwh_base_user_role (user_id, role_id) VALUES (4, 3);

-- 赵六 - 部门经理（技术研发中心副经理）
INSERT INTO lwh_base_user_role (user_id, role_id) VALUES (5, 3);

-- 孙七 - 普通员工
INSERT INTO lwh_base_user_role (user_id, role_id) VALUES (6, 6);

-- 周八 - 普通员工
INSERT INTO lwh_base_user_role (user_id, role_id) VALUES (7, 6);

-- 吴九 - 普通员工
INSERT INTO lwh_base_user_role (user_id, role_id) VALUES (8, 6);

-- 郑十 - 普通员工
INSERT INTO lwh_base_user_role (user_id, role_id) VALUES (9, 6);

-- 陈晓 - 普通员工
INSERT INTO lwh_base_user_role (user_id, role_id) VALUES (10, 6);

-- 黄大 - 部门经理（测试部）
INSERT INTO lwh_base_user_role (user_id, role_id) VALUES (11, 3);

-- 林二 - 普通员工
INSERT INTO lwh_base_user_role (user_id, role_id) VALUES (12, 6);

-- 刘强 - 部门经理（市场营销中心）
INSERT INTO lwh_base_user_role (user_id, role_id) VALUES (13, 3);

-- 陈美 - 部门经理（华东区销售部）
INSERT INTO lwh_base_user_role (user_id, role_id) VALUES (14, 3);

-- 张伟 - 普通员工
INSERT INTO lwh_base_user_role (user_id, role_id) VALUES (15, 6);

-- 钱静 - 部门经理（财务管理中心）
INSERT INTO lwh_base_user_role (user_id, role_id) VALUES (16, 3);

-- 孙丽 - 财务专员
INSERT INTO lwh_base_user_role (user_id, role_id) VALUES (17, 5);

-- 周芳 - 财务专员
INSERT INTO lwh_base_user_role (user_id, role_id) VALUES (18, 5);

-- 吴刚 - 人事专员
INSERT INTO lwh_base_user_role (user_id, role_id) VALUES (19, 4);

-- 郑华 - 部门经理（行政服务中心）
INSERT INTO lwh_base_user_role (user_id, role_id) VALUES (20, 3);

-- ==================== 数据验证查询 ====================
-- 查看组织机构数量
SELECT '组织机构数量' AS '统计项', COUNT(*) AS '数量' FROM lwh_base_organization;

-- 查看用户数量
SELECT '用户数量' AS '统计项', COUNT(*) AS '数量' FROM lwh_base_user;

-- 查看角色数量
SELECT '角色数量' AS '统计项', COUNT(*) AS '数量' FROM lwh_base_role;

-- 查看权限数量
SELECT '权限数量' AS '统计项', COUNT(*) AS '数量' FROM lwh_permission;

-- 查看用户角色分配情况
SELECT u.username AS '用户名', u.real_name AS '真实姓名', r.name AS '角色名称'
FROM lwh_base_user u
LEFT JOIN lwh_base_user_role ur ON u.id = ur.user_id
LEFT JOIN lwh_base_role r ON ur.role_id = r.id
ORDER BY u.id;
