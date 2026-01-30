package com.expense.chat_service.security;

import com.expense.chat_service.request.Principle;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.function.Function;

@Component
public class TokenValidation {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TokenGenerator tokenGenerator;

    public ValidationResponse handleRequest(String authorizationHeader) {
        ValidationResponse validationResponse = new ValidationResponse();
        try {
            validationResponse.setTokenValidated(false);
            String authToken = authorizationHeader != null && authorizationHeader.startsWith("Bearer ")
                    ? authorizationHeader.substring(7).trim()
                    : (authorizationHeader != null ? authorizationHeader.trim() : null);
            if (authToken == null || authToken.isEmpty()) {
                return validationResponse;
            }
            String loginKey = extractUsername(authToken);
            if (loginKey != null && validateToken(authToken, loginKey)) {
                Principle user = getPrincipleFromClaims(authToken);
                validationResponse.setTokenValidated(true);
                validationResponse.setUser(user);
                return validationResponse;
            }
            validationResponse.setTokenValidated(false);
            return validationResponse;
        } catch (ExpiredJwtException e) {
            validationResponse.setTokenValidated(false);
            validationResponse.setFailureMessage("Token Expired!");
            return validationResponse;
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(tokenGenerator.getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, String loginKey) {
        final String username = extractUsername(token);
        return username.equals(loginKey) && !isTokenExpired(token);
    }

    private Principle getPrincipleFromClaims(String token) {
        final Claims claims = extractAllClaims(token);
        Object userClaim = claims.get("user");
        if (userClaim instanceof LinkedHashMap) {
            return objectMapper.convertValue(userClaim, Principle.class);
        }
        return null;
    }
}
