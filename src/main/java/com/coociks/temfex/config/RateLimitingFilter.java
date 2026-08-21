package com.coociks.temfex.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // Увеличили лимит: 30 запросов в минуту (было 10)
    private final Bandwidth limit = Bandwidth.builder()
            .capacity(30)
            .refillIntervally(1, Duration.ofMinutes(1))
            .build();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        
        // ИСКЛЮЧЕНИЯ: не применяем rate limiting к этим путям
        if (requestPath.matches("/swagger-ui/.*") ||
            requestPath.equals("/swagger-ui.html") ||
            requestPath.matches("/v3/api-docs/.*") ||
            requestPath.matches("/api-docs/.*") ||
            requestPath.equals("/") ||
            requestPath.equals("/index.html") ||
            requestPath.matches("/.*\\.(html|css|js|png|jpg|ico)$") ||
            requestPath.startsWith("/s/")) {  // Скачивание файлов не лимитируем
            filterChain.doFilter(request, response);
            return;
        }

        String ip = getClientIP(request);
        Bucket bucket = buckets.computeIfAbsent(ip, k -> Bucket.builder().addLimit(limit).build());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("""
                {
                    "error": "Too Many Requests",
                    "message": "Превышен лимит запросов. Попробуйте через 1 минуту.",
                    "retryAfter": 60
                }
                """);
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null) {
            return xfHeader.split(",")[0];
        }
        return request.getRemoteAddr();
    }
}