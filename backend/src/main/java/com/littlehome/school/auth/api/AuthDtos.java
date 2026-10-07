package com.littlehome.school.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() { }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) { }
    public record RefreshRequest(@NotBlank String refreshToken) { }
    public record LogoutRequest(@NotBlank String refreshToken) { }
    public record AuthResponse(String accessToken, String refreshToken, String tokenType, Instant accessTokenExpiresAt,
                               UUID userId, String displayName, String role, UUID branchId) { }
}