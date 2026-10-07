package com.littlehome.school.auth.api;

import com.littlehome.school.auth.api.AuthDtos.AuthResponse;
import com.littlehome.school.auth.api.AuthDtos.LoginRequest;
import com.littlehome.school.auth.api.AuthDtos.LogoutRequest;
import com.littlehome.school.auth.api.AuthDtos.RefreshRequest;
import com.littlehome.school.auth.application.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) { return authService.login(request); }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) { return authService.refresh(request.refreshToken()); }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest request) { authService.logout(request.refreshToken()); }
}