package com.littlehome.school.branch;

import com.littlehome.school.identity.BranchEntity;
import java.util.UUID;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class BranchDtos {
    private BranchDtos() { }

    public record BranchRequest(
            @NotBlank @Size(max = 32) @Pattern(regexp = "[A-Za-z0-9-]+") String code,
            @NotBlank @Size(max = 160) String name,
            @Size(max = 32) String phone,
            @Email @Size(max = 254) String email,
            @Size(max = 100) String city) { }

    public record BranchUpdateRequest(
            @Size(max = 160) String name,
            @Size(max = 32) String phone,
            @Email @Size(max = 254) String email,
            @Size(max = 100) String city,
            @Pattern(regexp = "ACTIVE|INACTIVE") String status) { }

    public record BranchResponse(UUID id, String code, String name, String phone, String email, String city, String status) {
        static BranchResponse from(BranchEntity branch) {
            return new BranchResponse(branch.getId(), branch.getCode(), branch.getName(), branch.getPhone(), branch.getEmail(), branch.getCity(), branch.getStatus());
        }
    }
}