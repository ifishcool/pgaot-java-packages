package com.pgaot.sql.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** PGAOT 用户表 */
@Setter
@Getter
@Entity
@Table(name = "pgaot_user")
public class UserEntity {

    /** 云塔用户唯一标识（主键） */
    @Id
    @Column(length = 64)
    private String userId;

    @Column(length = 128)
    private String nickname;

    @Column(length = 512)
    private String avatar;

    @Column(length = 128)
    private String email;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }

}
