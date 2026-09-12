package com.symphonia.global.security.filter;

import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.global.security.writer.ErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {
    private static final Map<String, Integer> LIMIT_BY_PATH =
            Map.of(
                    "/api/v1/auth/signup", 5,
                    "/api/v1/auth/login", 5,
                    "/api/v1/auth/refresh", 10);
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final String KEY_PREFIX = "rate-limit:";

    private final StringRedisTemplate redisTemplate;
    private final ErrorResponseWriter errorResponseWriter;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        Integer limit = LIMIT_BY_PATH.get(request.getRequestURI());

        if (limit != null && isExceeded(request.getRequestURI(), request.getRemoteAddr(), limit)) {
            errorResponseWriter.send(response, AuthErrorCode.RATE_LIMIT_EXCEEDED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isExceeded(String path, String ip, int limit) {
        String key = KEY_PREFIX + path + ":" + ip;
        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1L) {
            redisTemplate.expire(key, WINDOW);
        }

        return count != null && count > limit;
    }
}
