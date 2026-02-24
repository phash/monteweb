package com.monteweb.admin.internal.service;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AnalyticsService {

    private final EntityManager entityManager;

    public AnalyticsService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Map<String, Object> getAnalytics() {
        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("totalUsers", countQuery("SELECT COUNT(*) FROM users"));
        stats.put("activeUsers", countQuery("SELECT COUNT(*) FROM users WHERE active = true"));
        stats.put("rooms", countQuery("SELECT COUNT(*) FROM rooms WHERE is_archived = false"));
        stats.put("posts", countQuery("SELECT COUNT(*) FROM feed_posts"));
        stats.put("events", countQuery("SELECT COUNT(*) FROM calendar_events"));
        stats.put("messages", countQuery("SELECT COUNT(*) FROM messages"));
        stats.put("postsThisMonth", countQuery(
                "SELECT COUNT(*) FROM feed_posts WHERE created_at >= date_trunc('month', CURRENT_TIMESTAMP)"));
        stats.put("newThisWeek", countQuery(
                "SELECT COUNT(*) FROM users WHERE created_at > CURRENT_TIMESTAMP - INTERVAL '7 days'"));

        return stats;
    }

    private long countQuery(String sql) {
        Object result = entityManager.createNativeQuery(sql).getSingleResult();
        return ((Number) result).longValue();
    }
}
