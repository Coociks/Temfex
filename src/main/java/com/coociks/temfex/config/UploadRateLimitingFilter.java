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
public class UploadRateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // Более строгий лимит для загрузки: 5 файлов в час
    private final Bandwidth uploadLimit = Bandwidth.builder()
            .capacity(5)
            .refillIntervally(1, Duration.ofHours(1))
            .build();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        // Применяем только к эндпоинту загрузки
        if (!requestPath.equals("/api/files/upload")) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = getClientIP(request);
        Bucket bucket = buckets.computeIfAbsent(ip, k -> Bucket.builder().addLimit(uploadLimit).build());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("""
                {
                    "error": "Too Many Requests",
                    "message": "Превышен лимит загрузок (5 файлов в час).",
                    "retryAfter": 3600
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