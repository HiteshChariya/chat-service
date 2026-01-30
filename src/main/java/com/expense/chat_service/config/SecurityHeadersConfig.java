package com.expense.chat_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(0)
public class SecurityHeadersConfig extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        // X-Frame-Options: Prevent clickjacking
        response.setHeader("X-Frame-Options", "DENY");
        
        // X-Content-Type-Options: Prevent MIME type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");
        
        // X-XSS-Protection: Enable XSS filter
        response.setHeader("X-XSS-Protection", "1; mode=block");
        
        // Content-Security-Policy: Restrict resource loading
        response.setHeader("Content-Security-Policy", 
            "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:;");
        
        // Referrer-Policy: Control referrer information
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // Permissions-Policy: Restrict browser features
        response.setHeader("Permissions-Policy", 
            "geolocation=(), microphone=(), camera=()");
        
        // Strict-Transport-Security: Force HTTPS (only in production)
        if (request.isSecure()) {
            response.setHeader("Strict-Transport-Security", 
                "max-age=31536000; includeSubDomains; preload");
        }
        
        filterChain.doFilter(request, response);
    }
}
