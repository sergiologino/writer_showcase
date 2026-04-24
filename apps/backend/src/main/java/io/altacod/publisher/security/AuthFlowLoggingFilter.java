package io.altacod.publisher.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Диагностика /api/auth: видно, дошёл ли запрос до контроллера (сравнить с логами AuthController)
 * и какой итоговый HTTP-статус (в т.ч. 403 с CORS/PNA до контроллера).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AuthFlowLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthFlowLoggingFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String p = request.getServletPath();
        return p == null || !p.startsWith("/api/auth");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String method = request.getMethod();
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        String pna = request.getHeader("Access-Control-Request-Private-Network");
        String acrm = request.getHeader("Access-Control-Request-Method");
        boolean hasAuth = request.getHeader(HttpHeaders.AUTHORIZATION) != null;
        // не логируем тело: только метаданные
        log.info(
                "auth request: method={} path={} origin={} acrPna={} acrMethod={} hasAuthorization={}",
                method, request.getRequestURI(), origin, pna, acrm, hasAuth
        );
        try {
            filterChain.doFilter(request, response);
        } finally {
            log.info("auth request done: method={} path={} status={}", method, request.getRequestURI(), response.getStatus());
        }
    }
}
