package com.pgaot.sql.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 共享记录 */
@Getter
@Entity
@Table(name = "ds_share", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"table_id", "from_user", "to_user"})
})
public class DsShareEntity {

    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(name = "table_id", nullable = false)
    private Long tableId;

    @Setter
    @Column(name = "from_user", nullable = false, length = 64)
    private String fromUser;

    @Setter
    @Column(name = "to_user", nullable = false, length = 64)
    private String toUser;

    @Setter
    @Column(name = "can_select", nullable = false)
    private boolean canSelect;

    @Setter
    @Column(name = "can_insert", nullable = false)
    private boolean canInsert;

    @Setter
    @Column(name = "can_update", nullable = false)
    private boolean canUpdate;

    @Setter
    @Column(name = "can_delete", nullable = false)
    private boolean canDelete;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }

}
