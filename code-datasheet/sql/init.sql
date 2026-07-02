CREATE TABLE IF NOT EXISTS ds_table (
    id           BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键',
    name         VARCHAR(64)  NOT NULL                 COMMENT '表名（用户可见）',
    title        VARCHAR(128) DEFAULT NULL             COMMENT '显示名称',
    owner_id     VARCHAR(64)  NOT NULL                 COMMENT '创建者',
    description  TEXT         DEFAULT NULL             COMMENT '描述',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_owner_table (owner_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ds_column (
    id          BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键',
    table_id    BIGINT       NOT NULL                 COMMENT '所属表',
    name        VARCHAR(64)  NOT NULL                 COMMENT '列名',
    type        VARCHAR(32)  NOT NULL                 COMMENT 'STRING/NUMBER/DATE/BOOLEAN',
    required    BOOLEAN      NOT NULL DEFAULT FALSE   COMMENT '是否必填',
    sort_order  INT          NOT NULL DEFAULT 0       COMMENT '排序',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_table_column (table_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
