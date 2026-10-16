package com.pandora6ix.backend.config;

import com.pandora6ix.backend.service.IdentityService;

import jakarta.servlet.http.*;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class BusinessAuthentication implements HandlerInterceptor {
    private final IdentityService identities;

    public BusinessAuthentication(IdentityService identities) {
        this.identities = identities;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod) || !request.getRequestURI().startsWith("/api/v1/"))
            return true;
        String path = request.getRequestURI();
        if (path.equals("/api/v1/auth/login")) return true;
        if (path.equals("/api/v1/admin/enterprise-initializations")
                && request.getHeader("Authorization") == null) return true;
        request.setAttribute(IdentityService.ACTOR_ATTRIBUTE, identities.authenticate(request));
        return true;
    }
}
