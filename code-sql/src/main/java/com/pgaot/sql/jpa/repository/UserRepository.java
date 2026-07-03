package com.pgaot.sql.jpa.repository;

import com.pgaot.sql.api.JpaTemplate;
import com.pgaot.sql.jpa.entity.UserEntity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户仓储.
 *
 * <pre>{@code
 * UserRepository repo = new UserRepository(JpaTemplate.fromEnv(UserEntity.class));
 * repo.upsert("alice", "Alice", "https://...");
 * UserEntity u = repo.findById("alice");
 * }</pre>
 */
public class UserRepository {

    private final JpaTemplate jpa;

    public UserRepository(JpaTemplate jpa) { this.jpa = jpa; }

    /** 保存或更新（按 userId 判断） */
    public void upsert(String userId, String nickname, String avatar) {
        UserEntity existing = jpa.findById(UserEntity.class, userId);
        if (existing != null) {
            existing.setNickname(nickname);
            existing.setAvatar(avatar);
            existing.setUpdatedAt(LocalDateTime.now());
            jpa.update(existing);
        } else {
            UserEntity u = new UserEntity();
            u.setUserId(userId);
            u.setNickname(nickname);
            u.setAvatar(avatar);
            jpa.save(u);
        }
    }

    /** 按 userId 查询 */
    public UserEntity findById(String userId) {
        return jpa.findById(UserEntity.class, userId);
    }

    /** 昵称模糊搜索 */
    public List<UserEntity> search(String keyword) {
        return jpa.query("FROM UserEntity WHERE nickname LIKE ?1", UserEntity.class,
                "%" + keyword + "%");
    }
}
