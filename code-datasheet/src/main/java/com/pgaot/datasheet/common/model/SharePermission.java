package com.pgaot.datasheet.common.model;

import lombok.Data;

/** 共享权限 */
@Data
public class SharePermission {
    private boolean canSelect;
    private boolean canInsert;
    private boolean canUpdate;
    private boolean canDelete;

    public SharePermission() {}

    public SharePermission(boolean canSelect, boolean canInsert, boolean canUpdate, boolean canDelete) {
        this.canSelect = canSelect;
        this.canInsert = canInsert;
        this.canUpdate = canUpdate;
        this.canDelete = canDelete;
    }

    /** 全部权限 */
    public static final SharePermission ALL = new SharePermission(true, true, true, true);
    /** 只读 */
    public static final SharePermission SELECT_ONLY = new SharePermission(true, false, false, false);
}
