package com.littlehome.school.identity;

import static com.littlehome.school.identity.AdminUserDtos.CreateUserRequest;
import static com.littlehome.school.identity.AdminUserDtos.UserResponse;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminUserController {
    private final UserRepository users;
    private final RoleRepository roles;
    private final BranchRepository branches;
    private final PasswordEncoder passwordEncoder;

    public AdminUserController(UserRepository users, RoleRepository roles, BranchRepository branches, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.roles = roles;
        this.branches = branches;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public Page<UserResponse> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page or size.");
        return users.findAll(PageRequest.of(page, size)).map(UserResponse::from);
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        if (users.existsByEmailIgnoreCase(request.email().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account already uses that email.");
        }
        BranchEntity branch = branches.findById(request.branchId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Branch not found."));
        RoleEntity role = roles.findByCode(request.role())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role not found."));
        UserEntity user = users.saveAndFlush(new UserEntity(branch, role, request.email().trim().toLowerCase(),
                passwordEncoder.encode(request.temporaryPassword()), request.firstName().trim(), request.lastName().trim()));
        return ResponseEntity.created(URI.create("/api/v1/admin/users/" + user.getId())).body(UserResponse.from(user));
    }
}