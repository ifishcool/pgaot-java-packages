package com.pgaot.sql.api;

import com.pgaot.sql.common.config.EnvConfig;
import com.pgaot.sql.exception.SqlException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import jakarta.persistence.criteria.CriteriaQuery;

import java.util.List;

/**
 * JPA 操作模板.
 *
 * <pre>{@code
 * JpaTemplate jpa = JpaTemplate.fromEnv(UserEntity.class);
 * JpaTemplate jpa = JpaTemplate.fromEnv("MAIN", UserEntity.class);
 * }</pre>
 */
public class JpaTemplate {

    private final SessionFactory sessionFactory;

    JpaTemplate(SessionFactory sessionFactory) { this.sessionFactory = sessionFactory; }

    /** 默认数据源 */
    public static JpaTemplate fromEnv(Class<?>... entities) {
        return fromEnv("", entities);
    }

    /** 命名数据源（CODE_SQL_URL_{name}） */
    public static JpaTemplate fromEnv(String name, Class<?>... entities) {
        String suffix = (name != null && !name.isBlank()) ? "_" + name : "";
        return create(
            EnvConfig.env(EnvConfig.URL + suffix),
            EnvConfig.env(EnvConfig.USER + suffix),
            EnvConfig.env(EnvConfig.PASS + suffix),
            entities);
    }

    public static JpaTemplate create(String url, String user, String pass, Class<?>... entities) {
        var cfg = new Configuration()
                .setProperty("hibernate.connection.url", url)
                .setProperty("hibernate.connection.username", user)
                .setProperty("hibernate.connection.password", pass)
                .setProperty("hibernate.hbm2ddl.auto", "none")
                .setProperty("hibernate.show_sql", "false");
        for (Class<?> c : entities) cfg.addAnnotatedClass(c);
        return new JpaTemplate(cfg.buildSessionFactory());
    }

    public <T> List<T> findAll(Class<T> c) {
        try (var s = sessionFactory.openSession()) {
            CriteriaQuery<T> cq = s.getCriteriaBuilder().createQuery(c);
            cq.select(cq.from(c));
            return s.createQuery(cq).getResultList();
        } catch (Exception e) { throw SqlException.jpaFailed(e.getMessage()); }
    }

    public <T> T findById(Class<T> c, Object id) {
        try (var s = sessionFactory.openSession()) { return s.find(c, id); }
    }

    public void save(Object entity) {
        try (var s = sessionFactory.openSession()) {
            s.beginTransaction(); s.persist(entity); s.getTransaction().commit();
        } catch (Exception e) { throw SqlException.jpaFailed(e.getMessage()); }
    }

    public void update(Object entity) {
        try (var s = sessionFactory.openSession()) {
            s.beginTransaction(); s.merge(entity); s.getTransaction().commit();
        } catch (Exception e) { throw SqlException.jpaFailed(e.getMessage()); }
    }

    public <T> void delete(Class<T> c, Object id) {
        try (var s = sessionFactory.openSession()) {
            s.beginTransaction();
            T e = s.find(c, id);
            if (e != null) s.remove(e);
            s.getTransaction().commit();
        } catch (Exception e) { throw SqlException.jpaFailed(e.getMessage()); }
    }

    public <T> List<T> query(String hql, Class<T> resultClass, Object... params) {
        try (var s = sessionFactory.openSession()) {
            var q = s.createQuery(hql, resultClass);
            for (int i = 0; i < params.length; i++) q.setParameter(i + 1, params[i]);
            return q.getResultList();
        } catch (Exception e) { throw SqlException.jpaFailed(e.getMessage()); }
    }

    public void close() { sessionFactory.close(); }
}
