package com.distributed.documentsearch.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantInterceptor implements HandlerInterceptor {
    
    private static final String TENANT_HEADER = "X-Tenant-Id";
    private final Logger logger = LoggerFactory.getLogger(TenantInterceptor.class);
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantId = request.getHeader(TENANT_HEADER);
        logger.info("Incoming request URI={}, tenant={}", request.getRequestURI(), tenantId);
        // Allow health check without tenant
        if (request.getRequestURI().startsWith("/api/v1/health")) {
            return true;
        }

        // For search endpoint, tenant can come from query param
        if (request.getRequestURI().startsWith("/api/v1/search")) {
            tenantId = request.getParameter("tenant");
        }

        if (tenantId == null || tenantId.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }

        TenantContext.setTenantId(tenantId);
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }
}
