package com.littlehome.school.identity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class AdminUserDtos {
    private AdminUserDtos() { }

    public record CreateUserRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @NotBlank @Pattern(regexp = "BRANCH_ADMIN|TEACHER|PARENT") String role,
            @NotNull UUID branchId,
            @NotBlank @Size(min = 16, max = 128) String temporaryPassword) { }

    public record UserResponse(UUID id, String email, String firstName, String lastName, String role,
                               UUID branchId, String branchName, boolean active, Instant createdAt) {
        static UserResponse from(UserEntity user) {
            return new UserResponse(user.getId(), user.getUsername(), user.getFirstName(), user.getLastName(),
                    user.getRoleCode(), user.getBranchId(), user.getBranchName(), user.isEnabled(), user.getCreatedAt());
        }
    }
}