-- ============================================
-- 公寓租房系统Demo 数据库建表脚本
-- 每次应用启动自动执行（IF NOT EXISTS 保证可重复执行）
-- ============================================

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID（主键）',
  `username`    VARCHAR(50)  NOT NULL COMMENT '账号（同时作为昵称）',
  `password`    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密存储）',
  `nickname`    VARCHAR(50)  DEFAULT NULL COMMENT '昵称（注册时默认等于账号）',
  `real_name`   VARCHAR(50)  DEFAULT NULL COMMENT '真实姓名',
  `age`         INT          DEFAULT NULL COMMENT '年龄',
  `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
  `cohabitants` VARCHAR(500) DEFAULT NULL COMMENT '同住人信息',
  `avatar`      VARCHAR(255) DEFAULT '' COMMENT '头像URL',
  `role`        VARCHAR(20)  NOT NULL DEFAULT 'user' COMMENT '角色：user普通用户 / admin管理员',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

-- --------------------------------------------
-- 兼容旧库：为已存在的 user 表补充真实信息字段
-- （MySQL 的 ADD COLUMN 不支持 IF NOT EXISTS，用 information_schema 判断后动态执行，保证幂等）
-- --------------------------------------------
SET @db_name = DATABASE();

SET @ddl = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'user' AND COLUMN_NAME = 'real_name') = 0,
  'ALTER TABLE `user` ADD COLUMN `real_name` VARCHAR(50) DEFAULT NULL COMMENT ''真实姓名'' AFTER `nickname`',
  'DO 0'));
PREPARE add_real_name FROM @ddl;
EXECUTE add_real_name;
DEALLOCATE PREPARE add_real_name;

SET @ddl = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'user' AND COLUMN_NAME = 'age') = 0,
  'ALTER TABLE `user` ADD COLUMN `age` INT DEFAULT NULL COMMENT ''年龄'' AFTER `real_name`',
  'DO 0'));
PREPARE add_age FROM @ddl;
EXECUTE add_age;
DEALLOCATE PREPARE add_age;

SET @ddl = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'user' AND COLUMN_NAME = 'cohabitants') = 0,
  'ALTER TABLE `user` ADD COLUMN `cohabitants` VARCHAR(500) DEFAULT NULL COMMENT ''同住人信息'' AFTER `phone`',
  'DO 0'));
PREPARE add_cohabitants FROM @ddl;
EXECUTE add_cohabitants;
DEALLOCATE PREPARE add_cohabitants;

-- --------------------------------------------
-- 昵称归一：昵称统一等于账号（历史数据自愈，已一致则为空更新）
-- --------------------------------------------
UPDATE `user` SET `nickname` = `username` WHERE `nickname` IS NULL OR `nickname` <> `username`;

-- --------------------------------------------
-- 管理员不存于 user 表：管理员为内置账号（application.yml 的 app.admin）
-- 此处清理历史遗留的管理员行，保证 C 端用户体系中不存在管理员
-- --------------------------------------------
DELETE FROM `user` WHERE `role` = 'admin';

-- 房源表
CREATE TABLE IF NOT EXISTS `apartment` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '房源ID（主键）',
  `title`       VARCHAR(100)  NOT NULL COMMENT '标题',
  `images`      TEXT          COMMENT '房源图片（逗号分隔URL列表）',
  `layout`      VARCHAR(50)   DEFAULT NULL COMMENT '户型',
  `area`        DECIMAL(8, 2) DEFAULT NULL COMMENT '面积（㎡）',
  `rent`        INT           DEFAULT NULL COMMENT '月租金（元）',
  `status`      VARCHAR(10)   NOT NULL DEFAULT '可租' COMMENT '状态：可租 / 已租',
  `facilities`  VARCHAR(255)  DEFAULT NULL COMMENT '配套设施（逗号分隔）',
  `address`     VARCHAR(255)  DEFAULT NULL COMMENT '位置信息',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '房源表';

-- 租房记录表
CREATE TABLE IF NOT EXISTS `booking` (
  `id`           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '记录ID（主键）',
  `user_id`      BIGINT      NOT NULL COMMENT '预约用户ID',
  `apartment_id` BIGINT      NOT NULL COMMENT '房源ID',
  `status`       VARCHAR(10) NOT NULL DEFAULT '已预约' COMMENT '记录状态：已预约 / 已成交 / 已取消',
  `create_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '预约时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_apartment_id` (`apartment_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '租房记录表';
