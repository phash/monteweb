package com.monteweb.shared.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
 * Simple in-memory rate limiting filter for auth endpoints.
 * Uses a token-bucket approach per client IP address.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter implements Filter {

    private static final int LOGIN_MAX_REQUESTS = 10;
    private static final int REGISTER_MAX_REQUESTS = 5;
    private static final int PASSWORD_RESET_MAX_REQUESTS = 5;
    private static final long WINDOW_MS = 60_000; // 1 minute

    private final Map<String, RateBucket> buckets = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) request;
        String path = httpReq.getRequestURI();

        int maxRequests = -1;
        if (path.startsWith("/api/v1/auth/login")) {
            maxRequests = LOGIN_MAX_REQUESTS;
        } else if (path.startsWith("/api/v1/auth/register")) {
            maxRequests = REGISTER_MAX_REQUESTS;
        } else if (path.startsWith("/api/v1/auth/password-reset")) {
            maxRequests = PASSWORD_RESET_MAX_REQUESTS;
        }

        if (maxRequests > 0) {
            String clientIp = getClientIp(httpReq);
            String bucketKey = clientIp + ":" + path;
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
    }
}
