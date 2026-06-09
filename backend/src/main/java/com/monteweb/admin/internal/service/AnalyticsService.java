package com.monteweb.admin.internal.service;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private final EntityManager entityManager;

    public AnalyticsService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Map<String, Object> getAnalytics() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // User stats
        stats.put("totalUsers", countQuery("SELECT COUNT(*) FROM users"));
        stats.put("activeUsers", countQuery("SELECT COUNT(*) FROM users WHERE is_active = true"));
        // User stats by role (AC: "User-Stats nach Rolle")
        stats.put("usersByRole", groupedCountQuery("SELECT role, COUNT(*) FROM users GROUP BY role"));
        stats.put("newThisWeek", countQuery(
                "SELECT COUNT(*) FROM users WHERE created_at > CURRENT_TIMESTAMP - INTERVAL '7 days'"));

        // Content stats
        stats.put("rooms", countQuery("SELECT COUNT(*) FROM rooms WHERE is_archived = false"));
        stats.put("posts", countQuery("SELECT COUNT(*) FROM feed_posts"));
        stats.put("postsThisMonth", countQuery(
                "SELECT COUNT(*) FROM feed_posts WHERE created_at >= date_trunc('month', CURRENT_TIMESTAMP)"));
        stats.put("events", countQuery("SELECT COUNT(*) FROM calendar_events"));
        stats.put("messages", countQuery("SELECT COUNT(*) FROM messages"));
        // Content stats: Files (AC: "Content-Stats: Files")
        stats.put("files", countQuery("SELECT COUNT(*) FROM room_files"));

        // Engagement stats (AC: "Engagement-Stats: Kommentare, Reaktionen, Login-Haeufigkeit")
        stats.put("comments", countQuery("SELECT COUNT(*) FROM feed_post_comments"));
        stats.put("reactions", countQuery("SELECT COUNT(*) FROM feed_reactions")
                + countQuery("SELECT COUNT(*) FROM message_reactions"));
        // Login frequency: users who logged in within the last 7 days
        stats.put("loginsThisWeek", countQuery(
                "SELECT COUNT(*) FROM users WHERE last_login_at > CURRENT_TIMESTAMP - INTERVAL '7 days'"));

        return stats;
    }

    private long countQuery(String sql) {
        Object result = entityManager.createNativeQuery(sql).getSingleResult();
        return ((Number) result).longValue();
    }

    /**
     * Runs a two-column "key, count" grouped query and returns it as a key->count map.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Long> groupedCountQuery(String sql) {
        List<Object[]> rows = entityManager.createNativeQuery(sql).getResultList();
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String key = row[0] != null ? row[0].toString() : "UNKNOWN";
            long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            result.put(key, count);
        }
        return result;
    }
}
