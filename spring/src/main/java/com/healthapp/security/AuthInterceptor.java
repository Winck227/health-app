package com.healthapp.security;

import com.healthapp.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);
    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (isPublicApi(path)) {
            log.debug("[HealthApp][auth] public api bypass: {}", path);
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        Long userId = jwtUtil.parseUserId(auth.substring(7).trim());
        UserContext.setUserId(userId);
        return true;
    }

    private boolean isPublicApi(String path) {
        if (path == null)
            return false;
        return path.endsWith("/api/foods")
                || path.contains("/api/foods/")
                || path.endsWith("/api/plan-templates")
                || path.contains("/api/plan-templates/")
                || path.endsWith("/api/auth/register")
                || path.endsWith("/api/auth/login")
                || path.endsWith("/api/admin/login");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception ex) {
        UserContext.clear();
    }
}
