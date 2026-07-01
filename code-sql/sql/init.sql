-- PGAOT 用户表 — 对接 code-auth 登录返回
CREATE TABLE IF NOT EXISTS pgaot_user (
    id         BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键',
    user_id    VARCHAR(64)  NOT NULL                 COMMENT '云塔用户唯一标识',
    nickname   VARCHAR(64)  DEFAULT NULL             COMMENT '昵称',
    avatar     VARCHAR(512) DEFAULT NULL             COMMENT '头像 URL',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PGAOT用户表';
