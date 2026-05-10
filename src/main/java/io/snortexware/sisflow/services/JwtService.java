package io.snortexware.sisflow.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;

    @Value("${jwt.expiration-ms:3600000}")
    private long expirationMs;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UUID userId) {
        return generateToken(userId, null);
    }

    public String generateToken(UUID userId, UUID tenantId) {
        var builder = Jwts.builder()
                .subject(userId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs));
        if (tenantId != null) builder.claim("tenantId", tenantId.toString());
        return builder.signWith(key).compact();
    }

    public void validateToken(String token) {
        parseClaims(token);
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public UUID getUserIdFromToken(String token) {
        return extractUserId(token);
    }

    public UUID getTenantIdFromToken(String token) {
        String tenantId = parseClaims(token).get("tenantId", String.class);
        return tenantId != null ? UUID.fromString(tenantId) : null;
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            throw new RuntimeException("JWT validation failed: " + e.getMessage(), e);
        }
    }
}
