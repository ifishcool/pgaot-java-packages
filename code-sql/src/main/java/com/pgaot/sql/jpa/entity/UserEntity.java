package com.pgaot.sql.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

/** PG用户表 */
@Getter
@Entity
@Table(name = "pgaot_user")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 云塔用户唯一标识 */
    @Column(nullable = false, unique = true, length = 64)
    private String userId;

    /** 昵称 */
    @Column(length = 64)
    private String nickname;

    /** 头像 URL */
    @Column(length = 512)
    private String avatar;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void setId(Long id) { this.id = id; }

    public void setUserId(String userId) { this.userId = userId; }

    public void setNickname(String nickname) { this.nickname = nickname; }

    public void setAvatar(String avatar) { this.avatar = avatar; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
