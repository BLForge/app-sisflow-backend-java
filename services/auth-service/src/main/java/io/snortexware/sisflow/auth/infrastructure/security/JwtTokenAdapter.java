package io.snortexware.sisflow.auth.infrastructure.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.snortexware.sisflow.auth.application.port.TokenPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenAdapter implements TokenPort {

    private final SecretKey secretKey;

    @Value("${jwt.expiration-ms:3600000}")
    private long expirationMs;

    public JwtTokenAdapter(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generateAccessToken(UUID userId, UUID tenantId) {
        var builder = Jwts.builder()
                .subject(userId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs));
        if (tenantId != null) {
            builder.claim("tenantId", tenantId.toString());
        }
        return builder.signWith(secretKey).compact();
    }
}
