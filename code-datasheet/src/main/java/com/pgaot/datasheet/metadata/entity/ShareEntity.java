package com.pgaot.datasheet.metadata.entity;

import lombok.Data;
/** 共享记录实体 */

@Data
public class ShareEntity {
    private Long id;
    private Long tableId;
    private String fromUser;
    private String toUser;
    private boolean canSelect;
    private boolean canInsert;
    private boolean canUpdate;
    private boolean canDelete;
}
