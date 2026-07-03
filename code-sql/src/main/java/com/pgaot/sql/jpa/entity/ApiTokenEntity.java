package com.pgaot.sql.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** API Token 实体 */
@Getter
@Entity
@Table(name = "api_token", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id")
})
public class ApiTokenEntity {

    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Setter
    @Column(length = 128)
    private String name;

    @Setter
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Setter
    @Column(length = 12, nullable = false)
    private String prefix;

    @Setter
    @Column(columnDefinition = "TEXT", nullable = false)
    private String scopes;

    @Setter
    @Column(name = "last_used")
    private LocalDateTime lastUsed;

    @Setter
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }

}
