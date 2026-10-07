package com.littlehome.school.auth.infrastructure;

import com.littlehome.school.identity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey key;
    private final long accessTokenMinutes;

    public JwtService(@Value("${app.security.jwt-secret-base64}") String secret,
                      @Value("${app.security.access-token-minutes}") long accessTokenMinutes) {
        this.key = Keys.hmacShaKeyFor(decodeSecret(secret));
        this.accessTokenMinutes = accessTokenMinutes;
    }

    public String issueAccessToken(UserEntity user) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(user.getUsername())
                .claim("uid", user.getId().toString())
                .claim("role", user.getRoleCode())
                .claim("token_type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenMinutes * 60)));
        UUID branchId = user.getBranchId();
        if (branchId != null) builder.claim("branch_id", branchId.toString());
        return builder.signWith(key).compact();
    }

    public Instant accessTokenExpiresAt() {
        return Instant.now().plusSeconds(accessTokenMinutes * 60);
    }

    public String subject(String token) { return claims(token).getSubject(); }

    public boolean isValidAccessToken(String token, UserDetails user) {
        Claims claims = claims(token);
        return user.getUsername().equalsIgnoreCase(claims.getSubject())
                && "access".equals(claims.get("token_type", String.class))
                && claims.getExpiration().after(new Date());
    }

    private Claims claims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    private byte[] decodeSecret(String secret) {
        String normalized = secret.trim().replace('-', '+').replace('_', '/');
        return Base64.getDecoder().decode(normalized);
    }
}