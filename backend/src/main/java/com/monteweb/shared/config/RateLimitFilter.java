package com.monteweb.shared.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple in-memory rate limiting filter for security-sensitive endpoints.
 * Uses a token-bucket approach per client IP address.
 * Includes periodic cleanup to prevent unbounded memory growth.
 * Can be disabled via monteweb.rate-limit.enabled=false (e.g. in tests).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnProperty(name = "monteweb.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitFilter implements Filter {

    private static final int LOGIN_MAX_REQUESTS = 10;
    private static final int REGISTER_MAX_REQUESTS = 5;
    private static final int PASSWORD_RESET_MAX_REQUESTS = 5;
    private static final int TWO_FA_MAX_REQUESTS = 5;
    private static final int ERROR_REPORT_MAX_REQUESTS = 10;
    private static final int FILE_UPLOAD_MAX_REQUESTS = 10;
    private static final int SEARCH_MAX_REQUESTS = 30;
    private static final int FORM_SUBMIT_MAX_REQUESTS = 10;
    private static final int JOB_APPLY_MAX_REQUESTS = 10;
    private static final long WINDOW_MS = 60_000; // 1 minute
    private static final int MAX_BUCKETS = 10_000; // Hard cap to prevent memory exhaustion

    private final Map<String, RateBucket> buckets = new ConcurrentHashMap<>();

    /**
     * Periodically clean up expired rate-limit buckets to prevent unbounded memory growth.
     * Runs every 5 minutes.
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 300_000)
    public void cleanupExpiredBuckets() {
        long now = System.currentTimeMillis();
        buckets.entrySet().removeIf(entry -> now - entry.getValue().getWindowStart() > WINDOW_MS * 2);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) request;
        String path = httpReq.getRequestURI();

        int maxRequests = -1;
        String method = httpReq.getMethod();
        if (path.startsWith("/api/v1/auth/login")) {
            maxRequests = LOGIN_MAX_REQUESTS;
        } else if (path.startsWith("/api/v1/auth/register")) {
            maxRequests = REGISTER_MAX_REQUESTS;
        } else if (path.startsWith("/api/v1/auth/password-reset")) {
            maxRequests = PASSWORD_RESET_MAX_REQUESTS;
        } else if (path.startsWith("/api/v1/auth/2fa")) {
            maxRequests = TWO_FA_MAX_REQUESTS;
        } else if (path.startsWith("/api/v1/error-reports")) {
            maxRequests = ERROR_REPORT_MAX_REQUESTS;
        } else if ("POST".equals(method) && (path.contains("/files") || path.contains("/fotobox") || path.contains("/avatar"))) {
            maxRequests = FILE_UPLOAD_MAX_REQUESTS;
        } else if (path.startsWith("/api/v1/search")) {
            maxRequests = SEARCH_MAX_REQUESTS;
        } else if ("POST".equals(method) && path.contains("/forms") && path.contains("/respond")) {
            maxRequests = FORM_SUBMIT_MAX_REQUESTS;
        } else if ("POST".equals(method) && path.contains("/jobs") && path.contains("/apply")) {
            maxRequests = JOB_APPLY_MAX_REQUESTS;
        }

        if (maxRequests > 0) {
            String clientIp = getClientIp(httpReq);
            // Normalize bucket key to prevent per-path-parameter buckets
            String normalizedPath = normalizePath(path);
            String bucketKey = clientIp + ":" + normalizedPath;

            // Prevent memory exhaustion from IP spoofing attacks
            if (buckets.size() >= MAX_BUCKETS) {
                cleanupExpiredBuckets();
            }

            RateBucket bucket = buckets.computeIfAbsent(bucketKey, k -> new RateBucket());

            if (!bucket.tryConsume(maxRequests)) {
                HttpServletResponse httpRes = (HttpServletResponse) response;
                httpRes.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                httpRes.setContentType("application/json");
                httpRes.getWriter().write("{\"error\":\"Too many requests. Please try again later.\",\"success\":false}");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * Normalize the path to a category so that /api/v1/rooms/{uuid}/files and /api/v1/rooms/{other-uuid}/files
     * share the same rate limit bucket.
     */
    private String normalizePath(String path) {
        // Replace UUIDs in path with placeholder
        return path.replaceAll("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", "{id}");
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Simple sliding window counter.
     */
    private static class RateBucket {
        private final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());
        private final AtomicInteger count = new AtomicInteger(0);

        synchronized boolean tryConsume(int maxRequests) {
            long now = System.currentTimeMillis();
            long start = windowStart.get();

            if (now - start > WINDOW_MS) {
                windowStart.set(now);
                count.set(1);
                return true;
            }

            return count.incrementAndGet() <= maxRequests;
        }

        long getWindowStart() {
            return windowStart.get();
        }
    }
}
