package com.pgaot.datasheet.api;

import com.pgaot.datasheet.common.model.SharePermission;
import com.pgaot.datasheet.exception.DatasheetException;
import com.pgaot.datasheet.metadata.MetadataStore;
import lombok.Getter;
import lombok.Setter;
import com.pgaot.datasheet.metadata.entity.ShareEntity;
import com.pgaot.datasheet.metadata.entity.TableEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 共享 API — 将表共享给其他用户，控制其读写权限.
 *
 * <pre>{@code
 * // 共享: 给 bob 查询+插入权限
 * engine.shares().share("alice", tableId, "bob", new SharePermission(true, true, false, false));
 *
 * // 共享: 全部权限
 * engine.shares().share("alice", tableId, "bob", SharePermission.ALL);
 *
 * // 查看表的共享列表
 * List<ShareInfo> list = engine.shares().list("alice", tableId);
 * }</pre>
 */
public class ShareApi {

    private final MetadataStore store;

    ShareApi(MetadataStore store) { this.store = store; }

    /** 共享表给指定用户 */
    public void share(String ownerId, String tableId, String toUser, SharePermission perm) {
        TableEntity t = store.getTable(parseId(tableId));
        if (t == null) throw DatasheetException.tableNotFound(tableId);
        if (!t.getOwnerId().equals(ownerId)) throw DatasheetException.notOwner();
        store.upsertShare(parseId(tableId), ownerId, toUser,
                perm.isCanSelect(), perm.isCanInsert(), perm.isCanUpdate(), perm.isCanDelete());
    }

    /** 取消共享 */
    public void unshare(String ownerId, String tableId, String toUser) {
        TableEntity t = store.getTable(parseId(tableId));
        if (t == null) throw DatasheetException.tableNotFound(tableId);
        if (!t.getOwnerId().equals(ownerId)) throw DatasheetException.notOwner();
        store.deleteShare(parseId(tableId), ownerId, toUser);
    }

    /** 查看表的共享列表 */
    public List<ShareInfo> list(String ownerId, String tableId) {
        TableEntity t = store.getTable(parseId(tableId));
        if (t == null) throw DatasheetException.tableNotFound(tableId);
        if (!t.getOwnerId().equals(ownerId)) throw DatasheetException.notOwner();
        return store.getSharesByTable(parseId(tableId)).stream().map(s -> {
            ShareInfo info = new ShareInfo();
            info.setTableId(tableId);
            info.setTableName(t.getName());
            info.setToUser(s.getToUser());
            info.setPermission(new SharePermission(s.isCanSelect(), s.isCanInsert(), s.isCanUpdate(), s.isCanDelete()));
            return info;
        }).collect(Collectors.toList());
    }

    /** 查询共享权限（null = 未共享） */
    ShareEntity getPermission(Long tableId, String userId) {
        return store.getShare(tableId, userId);
    }

    /** 收到共享: 谁共享给我的，什么权限 */
    public List<ReceivedShare> listReceived(String userId) {
        List<ReceivedShare> result = new ArrayList<>();
        for (Long tid : store.getSharedTableIds(userId)) {
            ShareEntity s = store.getShare(tid, userId);
            if (s == null) continue;
            TableEntity t = store.getTable(tid);
            if (t == null) continue;
            ReceivedShare rs = new ReceivedShare();
            rs.setTableId(String.valueOf(t.getId()));
            rs.setTableName(t.getName());
            rs.setFromUser(s.getFromUser());
            rs.setPermission(new SharePermission(s.isCanSelect(), s.isCanInsert(), s.isCanUpdate(), s.isCanDelete()));
            result.add(rs);
        }
        return result;
    }

    /** 发出共享: 我共享给谁了，什么权限 */
    public List<ShareInfo> listSent(String ownerId) {
        List<ShareInfo> result = new ArrayList<>();
        for (TableEntity t : store.listByUser(ownerId)) {
            if (!t.getOwnerId().equals(ownerId)) continue;
            for (ShareEntity s : store.getSharesByTable(t.getId())) {
                ShareInfo info = new ShareInfo();
                info.setTableId(String.valueOf(t.getId()));
                info.setTableName(t.getName());
                info.setToUser(s.getToUser());
                info.setPermission(new SharePermission(s.isCanSelect(), s.isCanInsert(), s.isCanUpdate(), s.isCanDelete()));
                result.add(info);
            }
        }
        return result;
    }

    private long parseId(String id) {
        try { return Long.parseLong(id); } catch (NumberFormatException e) { return 0L; }
    }

    @Getter @Setter
    public static class ShareInfo {
        private String tableId;
        private String tableName;
        private String toUser;
        private SharePermission permission;
    }

    @Getter @Setter
    public static class ReceivedShare {
        private String tableId;
        private String tableName;
        private String fromUser;
        private SharePermission permission;
    }
}
