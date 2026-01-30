package com.expense.chat_service.security;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
public class TokenGenerator {

    @Value("${jwt.secret:${JWT_SECRET:5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437}}")
    private String secret;

    @Value("${jwt.expiration:86400}")
    private long jwtTokenValidity;

    @Value("${jwt.audience:31b4428c-275e-11ec-9621-0242ac130002}")
    private String audience;

    public Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
