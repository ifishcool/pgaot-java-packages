CREATE TABLE IF NOT EXISTS ds_table (
    id           BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键',
    name         VARCHAR(64)  NOT NULL                 COMMENT '逻辑表名',
    title        VARCHAR(128) DEFAULT NULL             COMMENT '显示名称',
    owner_id     VARCHAR(64)  NOT NULL                 COMMENT '创建者',
    description  TEXT         DEFAULT NULL             COMMENT '描述',
    mode         VARCHAR(16)  NOT NULL DEFAULT 'READ_WRITE' COMMENT 'READ_ONLY/WRITE_ONLY/READ_WRITE',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_owner_table (owner_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ds_share (
    id          BIGINT      NOT NULL AUTO_INCREMENT  COMMENT '主键',
    table_id    BIGINT      NOT NULL                 COMMENT '表 ID',
    from_user   VARCHAR(64) NOT NULL                 COMMENT '共享者',
    to_user     VARCHAR(64) NOT NULL                 COMMENT '被共享者',
    can_select  BOOLEAN     NOT NULL DEFAULT TRUE,
    can_insert  BOOLEAN     NOT NULL DEFAULT FALSE,
    can_update  BOOLEAN     NOT NULL DEFAULT FALSE,
    can_delete  BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_share (table_id, from_user, to_user)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
