package com.littlehome.school.auth.application;

import com.littlehome.school.auth.api.AuthDtos.AuthResponse;
import com.littlehome.school.auth.api.AuthDtos.LoginRequest;
import com.littlehome.school.auth.domain.RefreshTokenEntity;
import com.littlehome.school.auth.domain.RefreshTokenRepository;
import com.littlehome.school.auth.infrastructure.JwtService;
import com.littlehome.school.identity.UserEntity;
import com.littlehome.school.identity.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final JwtService jwtService;
    private final long refreshTokenDays;
    private final SecureRandom random = new SecureRandom();

    public AuthService(AuthenticationManager authenticationManager, UserRepository users,
                       RefreshTokenRepository refreshTokens, JwtService jwtService,
                       @Value("${app.security.refresh-token-days}") long refreshTokenDays) {
        this.authenticationManager = authenticationManager;
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.jwtService = jwtService;
        this.refreshTokenDays = refreshTokenDays;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
        UserEntity user = users.findByEmailIgnoreCase(request.email()).orElseThrow();
        return issuePair(user, null);
    }

    @Transactional
    public AuthResponse refresh(String rawToken) {
        RefreshTokenEntity current = refreshTokens.findByTokenHashForUpdate(hash(rawToken))
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if (current.isRevoked() || current.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token expired or revoked");
        }
        AuthResponse response = issuePair(current.getUser(), current);
        return response;
    }

    @Transactional
    public void logout(String rawToken) {
        refreshTokens.findByTokenHash(hash(rawToken)).ifPresent(token -> token.revoke(Instant.now(), null));
    }

    private AuthResponse issuePair(UserEntity user, RefreshTokenEntity current) {
        String rawRefreshToken = newRefreshToken();
        RefreshTokenEntity replacement = refreshTokens.save(new RefreshTokenEntity(
                user, hash(rawRefreshToken), Instant.now().plusSeconds(refreshTokenDays * 86_400)));
        if (current != null) current.revoke(Instant.now(), replacement.getId());
        Instant accessExpiresAt = jwtService.accessTokenExpiresAt();
        return new AuthResponse(jwtService.issueAccessToken(user), rawRefreshToken, "Bearer", accessExpiresAt,
                user.getId(), user.getDisplayName(), user.getRoleCode(), user.getBranchId());
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}