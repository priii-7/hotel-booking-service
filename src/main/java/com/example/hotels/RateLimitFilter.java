package com.example.hotels;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Fixed-window per-IP limiter, per instance. Simple on purpose.
 * Production: shared limiter in Redis (token bucket via Lua) or at the API gateway.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final class Window {
        final long second;
        int count;
        Window(long second, int count) { this.second = second; this.count = count; }
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final int limit;

    public RateLimitFilter(@Value("${app.rate-limit-per-second}") int limit) { this.limit = limit; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        long now = System.currentTimeMillis() / 1000;
        Window w = windows.compute(req.getRemoteAddr(), (k, old) -> {
            if (old == null || old.second != now) return new Window(now, 1);
            old.count++;
            return old;
        });
        if (w.count > limit) {
            res.setStatus(429);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"rate limit exceeded\"}");
            return;
        }
        chain.doFilter(req, res);
    }
}
