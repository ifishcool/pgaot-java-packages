package com.pgaot.sql;

import com.pgaot.sql.api.JpaTemplate;
import com.pgaot.sql.jpa.entity.UserEntity;

public class JpaDemoTest {

    public static void main(String[] args) {
        JpaTemplate jpa = JpaTemplate.fromEnv(UserEntity.class);

        // 模拟 code-auth 登录成功后的数据落库
        UserEntity u = new UserEntity();
        u.setUserId("user_10001");
        u.setNickname("测试用户");
        u.setAvatar("https://cdn.example.com/avatar/10001.png");
        jpa.save(u);
        System.out.println("新增: id=" + u.getId() + ", userId=" + u.getUserId());

        // 查询
        jpa.findAll(UserEntity.class).stream().limit(5)
                .forEach(row -> System.out.println(
                        "  id=" + row.getId()
                        + ", userId=" + row.getUserId()
                        + ", nickname=" + row.getNickname()));

        // 更新昵称
        u.setNickname("新昵称");
        u.setUpdatedAt(java.time.LocalDateTime.now());
        jpa.update(u);
        System.out.println("已更新: " + jpa.findById(UserEntity.class, u.getId()).getNickname());

        // 删除
        jpa.delete(UserEntity.class, u.getId());
        System.out.println("已删除");

        jpa.close();
    }
}
