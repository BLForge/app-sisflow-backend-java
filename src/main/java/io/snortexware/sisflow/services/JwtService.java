package io.snortexware.sisflow.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;
import java.util.Base64;
import java.util.UUID;
import org.json.JSONObject;

@Service
public class JwtService {

    @Value("${supabase.jwks.url}")
    private String jwksUrl;

    private volatile ECPublicKey cachedKey;

    private synchronized ECPublicKey getPublicKey() throws Exception {
        if (cachedKey != null) return cachedKey;

        URL url = new URL(jwksUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestMethod("GET");

        String response = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        JSONObject jwks = new JSONObject(response);
        JSONObject key = jwks.getJSONArray("keys").getJSONObject(0);

        byte[] xBytes = Base64.getUrlDecoder().decode(key.getString("x"));
        byte[] yBytes = Base64.getUrlDecoder().decode(key.getString("y"));

        BigInteger x = new BigInteger(1, xBytes);
        BigInteger y = new BigInteger(1, yBytes);

        BigInteger p = new BigInteger("FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFF", 16);
        BigInteger a = new BigInteger("FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFC", 16);
        BigInteger b = new BigInteger("5AC635D8AA3A93E7B3EBBD55769886BC651D06B0CC53B0F63BCE3C3E27D2604B", 16);
        BigInteger n = new BigInteger("FFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551", 16);
        ECFieldFp field = new ECFieldFp(p);
        EllipticCurve curve = new EllipticCurve(field, a, b);
        ECPoint g = new ECPoint(
            new BigInteger("6B17D1F2E12C4247F8BCE6E563A440F277037D812DEB33A0F4A13945D898C296", 16),
            new BigInteger("4FE342E2FE1A7F9B8EE7EB4A7C0F9E162BCE33576B315ECECBB6406837BF51F5", 16)
        );
        ECParameterSpec spec = new ECParameterSpec(curve, g, n, 1);
        ECPublicKeySpec pubKeySpec = new ECPublicKeySpec(new ECPoint(x, y), spec);

        cachedKey = (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(pubKeySpec);
        return cachedKey;
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getPublicKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            throw new RuntimeException("JWT validation failed: " + e.getMessage(), e);
        }
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
        Claims claims = parseClaims(token);
        Object tenantId = claims.get("tenant_id");
        if (tenantId != null) {
            return UUID.fromString(tenantId.toString());
        }
        // Fallback: try to get from custom claims
        Object customTenant = claims.get("custom_claims");
        if (customTenant instanceof java.util.Map) {
            Object tenantFromCustom = ((java.util.Map<?, ?>) customTenant).get("tenant_id");
            if (tenantFromCustom != null) {
                return UUID.fromString(tenantFromCustom.toString());
            }
        }
        return null;
    }
}
