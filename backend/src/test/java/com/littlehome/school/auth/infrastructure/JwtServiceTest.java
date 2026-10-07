package com.littlehome.school.auth.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.littlehome.school.identity.UserEntity;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

class JwtServiceTest {
    private static final String SECRET = Base64.getEncoder().encodeToString(
            "little-home-local-test-key-with-at-least-32-bytes".getBytes());

    @Test
    void issuesShortLivedAccessTokenForUserAndBranch() {
        JwtService service = new JwtService(SECRET, 15);
        UUID userId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UserEntity user = mock(UserEntity.class);
        when(user.getId()).thenReturn(userId);
        when(user.getBranchId()).thenReturn(branchId);
        when(user.getRoleCode()).thenReturn("BRANCH_ADMIN");
        when(user.getUsername()).thenReturn("admin@littlehome.example");

        String token = service.issueAccessToken(user);
        UserDetails principal = mock(UserDetails.class);
        when(principal.getUsername()).thenReturn("admin@littlehome.example");

        assertThat(service.isValidAccessToken(token, principal)).isTrue();
        assertThat(service.subject(token)).isEqualTo("admin@littlehome.example");
        assertThat(service.accessTokenExpiresAt()).isAfter(java.time.Instant.now());
    }
}