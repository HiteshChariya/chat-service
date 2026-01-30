package com.expense.chat_service.security;

import com.expense.chat_service.request.Principle;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Order(1)
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

    private final TokenValidation tokenValidation;
    private final ObjectMapper objectMapper;

    private static final List<String> EXCLUDE_PATHS = List.of(
            "/ws", "/ws/", "/actuator"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (HttpMethod.OPTIONS.name().equals(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String requestTokenHeader = request.getHeader("Authorization");
        if (StringUtils.isBlank(requestTokenHeader)) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Missing authorization token");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return;
        }

        if (!requestTokenHeader.startsWith("Bearer ")) {
            requestTokenHeader = "Bearer " + requestTokenHeader;
        }

        try {
            ValidationResponse validationResponse = tokenValidation.handleRequest(requestTokenHeader);
            Principle principle = validationResponse.getUser();
            if (BooleanUtils.isNotTrue(validationResponse.getTokenValidated())) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", validationResponse.getFailureMessage() != null
                        ? validationResponse.getFailureMessage() : "Invalid token");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                return;
            }

            String role = principle.getUserType() != null ? principle.getUserType() : "USER";
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    principle, null, authorities);
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        } catch (IllegalArgumentException | ExpiredJwtException e) {
            // Fall through and let chain continue; other filters or endpoint may handle
        }

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        path = path.replace(contextPath, "");
        return EXCLUDE_PATHS.stream().anyMatch(path::startsWith) || path.contains("/actuator");
    }
}
