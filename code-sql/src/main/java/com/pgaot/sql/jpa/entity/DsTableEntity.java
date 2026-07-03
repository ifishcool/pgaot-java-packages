package com.pgaot.sql.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 数据表元数据 */
@Getter
@Entity
@Table(name = "ds_table", indexes = {
    @Index(name = "idx_owner_id", columnList = "owner_id")
})
public class DsTableEntity {

    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false, length = 64)
    private String name;

    @Setter
    @Column(length = 128)
    private String title;

    @Setter
    @Column(name = "owner_id", nullable = false, length = 64)
    private String ownerId;

    @Setter
    @Column(columnDefinition = "TEXT")
    private String description;

    @Setter
    @Column(length = 16, nullable = false)
    private String mode = "ALL";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Setter
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }

}
