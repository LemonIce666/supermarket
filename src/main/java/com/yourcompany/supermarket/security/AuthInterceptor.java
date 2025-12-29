package com.yourcompany.supermarket.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        AuthService.AuthSession session = authenticate(request, response);
        if (session == null) {
            return false;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequiredRole requiredRole = resolveRequiredRole(handlerMethod);
        if (requiredRole != null && !authService.hasRole(session, requiredRole.value())) {
            response.sendError(HttpStatus.FORBIDDEN.value(), "权限不足");
            return false;
        }

        return true;
    }

    private AuthService.AuthSession authenticate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String token = request.getHeader("X-Auth-Token");
        AuthService.AuthSession session = authService.resolveSession(token);
        if (session == null) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "未登录或会话失效");
        }
        return session;
    }

    private RequiredRole resolveRequiredRole(HandlerMethod handlerMethod) {
        RequiredRole roleOnMethod = handlerMethod.getMethodAnnotation(RequiredRole.class);
        if (roleOnMethod != null) {
            return roleOnMethod;
        }
        return handlerMethod.getBeanType().getAnnotation(RequiredRole.class);
    }
}
