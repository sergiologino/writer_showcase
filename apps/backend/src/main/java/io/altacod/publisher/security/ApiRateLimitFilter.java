package io.altacod.publisher.security;

import io.altacod.publisher.config.PublisherRateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Фиксированное окно по минутам: лимит запросов на IP для auth-эндпоинтов и для остального API.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private final PublisherRateLimitProperties props;
    private final Map<String, Window> authWindows = new ConcurrentHashMap<>();
    private final Map<String, Window> apiWindows = new ConcurrentHashMap<>();

    public ApiRateLimitFilter(PublisherRateLimitProperties props) {
        this.props = props;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!props.isEnabled()) {
            return true;
        }
        String uri = request.getRequestURI();
        return !uri.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String ip = clientIp(request);
        String uri = request.getRequestURI();
        boolean authHeavy = uri.startsWith("/api/auth/login")
                || uri.startsWith("/api/auth/register")
                || uri.startsWith("/api/auth/refresh");
        int limit = authHeavy ? props.getAuthPerMinute() : props.getApiPerMinute();
        Map<String, Window> map = authHeavy ? authWindows : apiWindows;
        Window w = map.computeIfAbsent(ip, k -> new Window());
        if (!w.tryAcquire(limit)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private static final class Window {
        private long minuteEpoch;
        private int count;

        synchronized boolean tryAcquire(int limit) {
            long m = System.currentTimeMillis() / 60_000L;
            if (m != minuteEpoch) {
                minuteEpoch = m;
                count = 0;
            }
            if (count >= limit) {
                return false;
            }
            count++;
            return true;
        }
    }
}
