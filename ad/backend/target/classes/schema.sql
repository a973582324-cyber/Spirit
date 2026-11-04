-- ============================================================================
-- 广告制作公司宣传物料订单管理系统 · 数据库设计
-- 数据库：MySQL 8.0+（课程统一关系数据库）
-- 字符集：utf8mb4 （支持 emoji 和中文）
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS operation_history;
DROP TABLE IF EXISTS order_change_record;
DROP TABLE IF EXISTS modification_feedback;
DROP TABLE IF EXISTS design_version;
DROP TABLE IF EXISTS material_task;
DROP TABLE IF EXISTS material_order;
DROP TABLE IF EXISTS customer;
DROP TABLE IF EXISTS sys_user;

-- ============================================================================
-- 1. sys_user 用户表
--    三类岗位：客户经理 (CS_MANAGER) / 业务负责人 (BIZ_LEADER) / 设计制作人员 (DESIGNER)
-- ============================================================================
CREATE TABLE sys_user (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL COMMENT '登录账号',
    password      VARCHAR(100) NOT NULL COMMENT '密码（课程 demo 用明文；生产应 BCrypt）',
    real_name     VARCHAR(50)  NOT NULL COMMENT '真实姓名',
    role          VARCHAR(20)  NOT NULL COMMENT '角色：CS_MANAGER / BIZ_LEADER / DESIGNER',
    phone         VARCHAR(20)           COMMENT '联系电话',
    email         VARCHAR(100)          COMMENT '邮箱',
    enabled       TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用：1启用 0停用',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';


-- ============================================================================
-- 2. customer 客户表
--    广告公司的服务对象：奶茶店、餐饮企业、品牌方等
-- ============================================================================
CREATE TABLE customer (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    customer_name   VARCHAR(100) NOT NULL COMMENT '客户名称（如：甜野奶茶望京店）',
    contact_person  VARCHAR(50)  NOT NULL COMMENT '联系人（对接人姓名）',
    contact_phone   VARCHAR(20)  NOT NULL COMMENT '联系电话',
    contact_email   VARCHAR(100)          COMMENT '联系邮箱',
    address         VARCHAR(200)          COMMENT '门店/公司地址',
    notes           VARCHAR(500)          COMMENT '备注：客户偏好、历史合作情况等',
    create_by       BIGINT                COMMENT '由哪位客户经理录入（sys_user.id）',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_customer_name (customer_name),
    KEY idx_create_by (create_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户信息表';


-- ============================================================================
-- 3. material_order 宣传物料订单主表
--    一笔完整的广告物料订单，包含多个物料任务
-- ============================================================================
CREATE TABLE material_order (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    order_no            VARCHAR(50)  NOT NULL COMMENT '订单编号（业务生成，如 MO20261104001）',
    customer_id         BIGINT       NOT NULL COMMENT '关联客户',
    customer_contact    VARCHAR(100)          COMMENT '下单时联系人（冗余快照，避免客户改资料后订单对不上）',
    overall_deadline    DATETIME     NOT NULL COMMENT '整笔订单的交付截止时间',
    order_status        VARCHAR(30)  NOT NULL DEFAULT 'DRAFT' COMMENT '订单状态：DRAFT / CONFIRMED / IN_PROGRESS / PENDING_DELIVERY / COMPLETED / CANCELLED',
    notes               VARCHAR(1000)         COMMENT '整体备注：客户风格要求、参考链接等',
    create_by           BIGINT       NOT NULL COMMENT '创建人（客户经理 sys_user.id）',
    confirm_by          BIGINT                COMMENT '确认人（业务负责人 sys_user.id）',
    confirm_time        DATETIME              COMMENT '负责人确认时间',
    complete_time       DATETIME              COMMENT '订单完成时间（全部物料交付后客户经理操作）',
    cancel_by           BIGINT                COMMENT '取消操作人',
    cancel_time         DATETIME              COMMENT '取消时间',
    cancel_reason       VARCHAR(500)          COMMENT '取消原因',
    create_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_customer_id (customer_id),
    KEY idx_create_by (create_by),
    KEY idx_order_status (order_status),
    KEY idx_overall_deadline (overall_deadline),
    CONSTRAINT fk_order_customer   FOREIGN KEY (customer_id) REFERENCES customer(id),
    CONSTRAINT fk_order_create_by  FOREIGN KEY (create_by)   REFERENCES sys_user(id),
    CONSTRAINT fk_order_confirm_by FOREIGN KEY (confirm_by)  REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宣传物料订单主表';


-- ============================================================================
-- 4. material_task 物料任务表
--    订单拆出来的每项具体物料：海报 / 展架 / 菜单 / 宣传册 / 线上配图 等
--    每个任务独立分派给一位设计制作人员，独立流转状态
-- ============================================================================
CREATE TABLE material_task (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    order_id        BIGINT       NOT NULL COMMENT '所属订单',
    task_no         INT          NOT NULL COMMENT '任务序号（订单内从 1 开始，如 海报=1 展架=2）',
    material_type   VARCHAR(20)  NOT NULL COMMENT '物料类型：POSTER / STANDEE / MENU / BROCHURE / ONLINE_IMAGE',
    size_spec       VARCHAR(100) NOT NULL COMMENT '尺寸规格：线下如 60x90cm，线上如 1920x1080px',
    quantity        INT          NOT NULL DEFAULT 1 COMMENT '数量',
    `usage`           VARCHAR(200)          COMMENT '用途：如"新店开业户外立牌"',
    ref_notes       VARCHAR(1000)         COMMENT '参考说明：风格、色号、参考图链接等',
    task_deadline   DATETIME     NOT NULL COMMENT '该项物料的截止时间（可与整体 deadline 不同）',
    assignee_id     BIGINT                COMMENT '分派给哪位设计制作人员（sys_user.id）；PENDING_ASSIGN 时为 NULL',
    task_status     VARCHAR(30)  NOT NULL DEFAULT 'PENDING_ASSIGN' COMMENT '任务状态：PENDING_ASSIGN / ASSIGNED / IN_PROGRESS / PENDING_CLIENT_REVIEW / NEEDS_REVISION / CLIENT_CONFIRMED / CANCELLED',
    version_count   INT          NOT NULL DEFAULT 0 COMMENT '已上传的设计稿版本数',
    last_submit_time DATETIME             COMMENT '设计人员最近一次提交的时间',
    last_feedback_time DATETIME           COMMENT '客户经理最近一次录入客户意见的时间',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_task_no (order_id, task_no),
    KEY idx_assignee_id (assignee_id),
    KEY idx_task_status (task_status),
    KEY idx_task_deadline (task_deadline),
    CONSTRAINT fk_task_order    FOREIGN KEY (order_id)    REFERENCES material_order(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_assignee FOREIGN KEY (assignee_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料任务表';


-- ============================================================================
-- 5. design_version 设计稿版本记录表
--    每次上传一个新版本就 INSERT 一行，旧版本永远不覆盖
--    业务规则：上传设计稿 != 客户已确认；需由客户经理在 modification_feedback 记录客户意见
-- ============================================================================
CREATE TABLE design_version (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    task_id         BIGINT       NOT NULL COMMENT '所属物料任务',
    version_no      INT          NOT NULL COMMENT '版本号（从 1 递增，永不回退）',
    file_url        VARCHAR(500) NOT NULL COMMENT '文件访问地址',
    file_name       VARCHAR(200) NOT NULL COMMENT '原始文件名',
    file_size_bytes BIGINT                COMMENT '文件大小（字节）',
    upload_by       BIGINT       NOT NULL COMMENT '上传人（设计制作人员 sys_user.id）',
    upload_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    version_notes   VARCHAR(500)          COMMENT '设计人员对本次版本的说明（自填）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_task_version (task_id, version_no),
    KEY idx_upload_by (upload_by),
    KEY idx_upload_time (upload_time),
    CONSTRAINT fk_dv_task     FOREIGN KEY (task_id)   REFERENCES material_task(id) ON DELETE CASCADE,
    CONSTRAINT fk_dv_uploader FOREIGN KEY (upload_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设计稿版本记录表';


-- ============================================================================
-- 6. modification_feedback 客户修改意见记录表
--    客户经理记录客户对某个版本的反馈
--    业务规则：只有客户经理可以录入；意见关联到具体版本；可以多次意见（一次版本可能多轮沟通）
-- ============================================================================
CREATE TABLE modification_feedback (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    task_id             BIGINT       NOT NULL COMMENT '所属物料任务',
    target_version_no   INT          NOT NULL COMMENT '针对哪个版本的反馈（design_version.version_no）',
    feedback_content    VARCHAR(2000) NOT NULL COMMENT '客户修改意见原文',
    feedback_source     VARCHAR(100)          COMMENT '反馈渠道：微信 / 电话 / 邮件 / 现场',
    record_by           BIGINT       NOT NULL COMMENT '录入人（只能是客户经理 sys_user.id）',
    record_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed           TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '设计人员是否已处理这条意见：0未处理 1已提交新版本',
    process_time        DATETIME              COMMENT '设计人员处理时间（提交新版本时回填）',
    PRIMARY KEY (id),
    KEY idx_task_id (task_id),
    KEY idx_record_by (record_by),
    KEY idx_processed (processed),
    CONSTRAINT fk_mf_task   FOREIGN KEY (task_id)   REFERENCES material_task(id) ON DELETE CASCADE,
    CONSTRAINT fk_mf_record FOREIGN KEY (record_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户修改意见记录表';


-- ============================================================================
-- 7. order_change_record 订单变更记录表
--    业务规则：物料清单和截止时间确认后不能直接覆盖；必须留下变更记录
--    任何涉及以下场景都会产生一条：
--      - 新增物料任务
--      - 删除物料任务（CANCELLED）
--      - 修改物料任务的尺寸/数量/截止时间
--      - 修改订单整体截止时间
-- ============================================================================
CREATE TABLE order_change_record (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    order_id        BIGINT       NOT NULL COMMENT '所属订单',
    change_type     VARCHAR(30)  NOT NULL COMMENT '变更类型：TASK_ADD / TASK_REMOVE / TASK_MODIFY / TASK_DEADLINE_CHANGE / ORDER_DEADLINE_CHANGE / OTHER',
    change_target   VARCHAR(50)           COMMENT '变更对象描述：如 task#2 的尺寸 或 订单整体 deadline',
    change_before   VARCHAR(1000)         COMMENT '变更前的值（JSON 或文本快照）',
    change_after    VARCHAR(1000)         COMMENT '变更后的值',
    change_reason   VARCHAR(500)  NOT NULL COMMENT '变更原因（必填）',
    change_by       BIGINT       NOT NULL COMMENT '变更操作人',
    change_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_order_id (order_id),
    KEY idx_change_type (change_type),
    CONSTRAINT fk_ocr_order FOREIGN KEY (order_id) REFERENCES material_order(id) ON DELETE CASCADE,
    CONSTRAINT fk_ocr_by    FOREIGN KEY (change_by)  REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单变更记录表';


-- ============================================================================
-- 8. operation_history 操作历史表
--    任何关键业务操作都记一笔，保证全流程可追溯
--    业务类型 + 业务ID 多态关联，不设外键
-- ============================================================================
CREATE TABLE operation_history (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    business_type   VARCHAR(30)  NOT NULL COMMENT '业务类型：ORDER / TASK / VERSION / FEEDBACK / CHANGE',
    business_id     BIGINT       NOT NULL COMMENT '业务对象 ID（如 order.id / material_task.id）',
    operation_type  VARCHAR(30)  NOT NULL COMMENT '操作类型：CREATE / CONFIRM / ASSIGN / UPLOAD / FEEDBACK / SUBMIT_NEW_VERSION / CLIENT_CONFIRM / CANCEL / COMPLETE / CHANGE',
    operation_desc  VARCHAR(500)          COMMENT '操作说明（人可读，如"分派给 张小艺"）',
    operator_id     BIGINT       NOT NULL COMMENT '操作人',
    operator_name   VARCHAR(50)           COMMENT '操作人姓名（快照，方便直接展示）',
    operate_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_business (business_type, business_id),
    KEY idx_operator (operator_id),
    KEY idx_operate_time (operate_time),
    CONSTRAINT fk_oh_operator FOREIGN KEY (operator_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作历史表';


SET FOREIGN_KEY_CHECKS = 1;


-- ============================================================================
-- 初始化演示数据
-- ============================================================================

-- --------------------------------------------------------------------------
-- 用户账号（3 个岗位，密码均为 demo123）
-- --------------------------------------------------------------------------
INSERT INTO sys_user (username, password, real_name, role, phone, email, enabled) VALUES
('chenliang',  'demo123', '陈亮',   'CS_MANAGER',  '13800001111', 'chenliang@demo.com', 1),
('wangli',     'demo123', '王丽',   'BIZ_LEADER',  '13800002222', 'wangli@demo.com',    1),
('zhangxiaoyi','demo123', '张小艺', 'DESIGNER',    '13800003333', 'zhangxy@demo.com',   1),
('liusiyuan',  'demo123', '刘思源', 'DESIGNER',    '13800004444', 'liusy@demo.com',     1);

-- --------------------------------------------------------------------------
-- 客户
-- --------------------------------------------------------------------------
INSERT INTO customer (customer_name, contact_person, contact_phone, contact_email, address, notes, create_by) VALUES
('甜野奶茶望京店', '孙店长', '13900009999', 'sun@tianye.com', '北京市朝阳区望京SOHO B座 108', '新开的网红奶茶店，预算充足，重视视觉年轻感', 1),
('川味轩餐饮连锁', '赵经理', '13700008888', 'zhao@chuanweixuan.com', '成都市锦江区春熙路总店', '老客户，每年 4 次物料订单', 1);

-- --------------------------------------------------------------------------
-- 订单 + 物料任务（完整链路：一笔甜野奶茶店的开业订单，部分走通到待交付状态）
-- --------------------------------------------------------------------------
INSERT INTO material_order (order_no, customer_id, customer_contact, overall_deadline, order_status, notes, create_by, confirm_by, confirm_time) VALUES
('MO20261104001', 1, '孙店长 13900009999', '2026-11-07 18:00:00', 'IN_PROGRESS', '奶茶店新店开业物料，风格要求年轻、明亮、橙黄色主调', 1, 2, '2026-11-04 10:30:00');

-- 同一订单拆出来的 3 项物料任务
INSERT INTO material_task (order_id, task_no, material_type, size_spec, quantity, `usage`, ref_notes, task_deadline, assignee_id, task_status, version_count, last_submit_time) VALUES
(1, 1, 'POSTER',       '60x90cm',   2, '店内墙面海报（新品推荐 + 开业活动）', '风格参考小红书"喜茶新店开业"系列', '2026-11-06 18:00:00', 3, 'PENDING_CLIENT_REVIEW', 1, '2026-11-05 14:20:00'),
(1, 2, 'STANDEE',      '80x200cm',  1, '店门口立牌', '需预留 LOGO 位置，客户还没给 LOGO 原文件', '2026-11-06 18:00:00', 4, 'IN_PROGRESS', 0, NULL),
(1, 3, 'ONLINE_IMAGE', '1080x1920', 4, '公众号推文配图 4 张', '需与海报主视觉一致', '2026-11-05 18:00:00', 3, 'CLIENT_CONFIRMED', 2, '2026-11-05 17:00:00');

-- --------------------------------------------------------------------------
-- 设计稿版本
-- --------------------------------------------------------------------------
INSERT INTO design_version (task_id, version_no, file_url, file_name, file_size_bytes, upload_by, version_notes) VALUES
-- 任务 #1 海报：v1 刚上传，等客户意见
(1, 1, '/uploads/poster_task1_v1.png',  '甜野奶茶店_海报_v1.png',  2560000, 3, '第一版初稿，主打橙黄调，用了甜野的标准色'),
-- 任务 #3 线上配图：v1 客户提了意见，v2 已确认
(3, 1, '/uploads/online_task3_v1.png', '甜野奶茶店_公众号配图_v1_01.png', 1280000, 3, 'v1 四张配图，客户说颜色太暗'),
(3, 2, '/uploads/online_task3_v2.png', '甜野奶茶店_公众号配图_v2_01.png', 1350000, 3, 'v2 调亮整体亮度，换了更年轻的字体');

-- --------------------------------------------------------------------------
-- 客户修改意见
-- --------------------------------------------------------------------------
INSERT INTO modification_feedback (task_id, target_version_no, feedback_content, feedback_source, record_by, processed, process_time) VALUES
-- 任务 #1 海报：客户还没看，所以没意见
-- 任务 #3 线上配图：针对 v1 的意见
(3, 1, '客户说颜色整体太暗，想更亮一些；还有 logo 可以放得更大一点', '微信', 1, 1, '2026-11-05 16:30:00');

-- --------------------------------------------------------------------------
-- 订单变更记录（一笔典型的变更）
-- --------------------------------------------------------------------------
INSERT INTO order_change_record (order_id, change_type, change_target, change_before, change_after, change_reason, change_by) VALUES
(1, 'TASK_MODIFY', 'task#1 海报数量', '1 张', '2 张', '客户说新店开业当天人多，需要再做一张贴在收银台旁边', 1);

-- --------------------------------------------------------------------------
-- 操作历史（关键步骤全覆盖）
-- --------------------------------------------------------------------------
INSERT INTO operation_history (business_type, business_id, operation_type, operation_desc, operator_id, operator_name, operate_time) VALUES
-- 订单生命周期
('ORDER', 1, 'CREATE',     '创建订单 MO20261104001（甜野奶茶望京店开业）',       1, '陈亮',   '2026-11-04 09:00:00'),
('ORDER', 1, 'CONFIRM',    '业务负责人王丽审核通过物料清单和截止时间',            2, '王丽',   '2026-11-04 10:30:00'),
-- 任务分派
('TASK', 1, 'ASSIGN',      '将海报任务 #1 分派给 张小艺',                         2, '王丽',   '2026-11-04 10:35:00'),
('TASK', 2, 'ASSIGN',      '将展架任务 #2 分派给 刘思源',                         2, '王丽',   '2026-11-04 10:35:00'),
('TASK', 3, 'ASSIGN',      '将公众号配图任务 #3 分派给 张小艺',                    2, '王丽',   '2026-11-04 10:35:00'),
-- 设计稿上传
('VERSION', 1, 'UPLOAD',    '海报 task#1 上传 v1 初稿',                            3, '张小艺', '2026-11-05 14:20:00'),
('VERSION', 3, 'UPLOAD',    '公众号配图 task#3 上传 v1',                           3, '张小艺', '2026-11-05 11:00:00'),
('VERSION', 3, 'UPLOAD',    '公众号配图 task#3 上传 v2（处理客户意见）',             3, '张小艺', '2026-11-05 17:00:00'),
-- 客户意见录入
('FEEDBACK', 3, 'FEEDBACK',  '客户对 task#3 v1 提出意见：颜色太暗、logo 要更大',     1, '陈亮',   '2026-11-05 16:00:00'),
-- 客户确认
('TASK', 3, 'CLIENT_CONFIRM', '客户经理陈亮记录客户对 task#3 v2 表示确认通过',        1, '陈亮',   '2026-11-05 17:30:00'),
-- 变更记录
('CHANGE', 1, 'CHANGE',     '订单 MO20261104001 发生变更：task#1 海报数量 1→2',     1, '陈亮',   '2026-11-04 15:00:00');


