package com.pgaot.sql;

import com.pgaot.sql.api.JpaTemplate;
import com.pgaot.sql.jpa.entity.UserEntity;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JpaDemoTest {

    private static JpaTemplate jpa;
    private static final String TEST_USER = "user_junit_demo";

    @BeforeAll
    static void requireDb() {
        assumeTrue(EnvLoader.hasDb(), "跳过：需要数据库");
        jpa = JpaTemplate.fromEnv(UserEntity.class);
    }

    @AfterAll
    static void cleanup() {
        if (jpa != null) {
            try { jpa.delete(UserEntity.class, TEST_USER); } catch (Exception ignored) {}
            jpa.close();
        }
    }

    @Test @Order(1)
    void shouldSaveAndFindById() {
        UserEntity u = new UserEntity();
        u.setUserId(TEST_USER);
        u.setNickname("测试用户");
        u.setAvatar("https://cdn.example.com/avatar/10001.png");
        jpa.save(u);

        UserEntity found = jpa.findById(UserEntity.class, TEST_USER);
        assertNotNull(found);
        assertEquals("测试用户", found.getNickname());
    }

    @Test @Order(2)
    void shouldUpdateNickname() {
        UserEntity u = jpa.findById(UserEntity.class, TEST_USER);
        assertNotNull(u);
        u.setNickname("新昵称");
        jpa.update(u);

        UserEntity updated = jpa.findById(UserEntity.class, TEST_USER);
        assertEquals("新昵称", updated.getNickname());
    }

    @Test
    void shouldFindAll() {
        var list = jpa.findAll(UserEntity.class);
        assertNotNull(list);
    }
}
