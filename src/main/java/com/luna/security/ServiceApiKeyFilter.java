package com.luna.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter for authenticating service-to-service API calls using API keys.
 * This allows microservices to communicate securely without JWT tokens.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceApiKeyFilter extends OncePerRequestFilter {
    
    private static final String API_KEY_HEADER = "X-Service-Api-Key";
    
    @Value("${app.service.api-key:}")
    private String serviceApiKey;
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        
        // Only check API key for /internal/** endpoints
        if (requestPath.startsWith("/internal/")) {
            String apiKey = request.getHeader(API_KEY_HEADER);
            
            if (apiKey == null || apiKey.isEmpty()) {
                log.warn("Missing API key for internal endpoint: {}", requestPath);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Missing service API key\"}");
                return;
            }
            
            if (!isValidApiKey(apiKey)) {
                log.warn("Invalid API key for internal endpoint: {}", requestPath);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Invalid service API key\"}");
                return;
            }
            
            log.debug("Valid API key for internal endpoint: {}", requestPath);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private boolean isValidApiKey(String apiKey) {
        // If no service API key is configured, reject all requests
        if (serviceApiKey == null || serviceApiKey.isEmpty()) {
            log.error("Service API key not configured in application properties");
            return false;
        }
        
        return serviceApiKey.equals(apiKey);
    }
}
