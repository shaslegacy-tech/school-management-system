package com.littlehome.school.branch;

import com.littlehome.school.branch.BranchDtos.BranchResponse;
import com.littlehome.school.identity.BranchRepository;
import com.littlehome.school.identity.UserEntity;
import com.littlehome.school.branch.BranchDtos.BranchRequest;
import com.littlehome.school.branch.BranchDtos.BranchUpdateRequest;
import java.util.List;
import java.net.URI;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/branches")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_ADMIN')")
public class BranchController {
    private final BranchRepository branches;

    public BranchController(BranchRepository branches) { this.branches = branches; }

    @GetMapping
    @Transactional(readOnly = true)
    public List<BranchResponse> list(@AuthenticationPrincipal UserEntity actor) {
        if ("SUPER_ADMIN".equals(actor.getRoleCode())) {
            return branches.findAll().stream().map(BranchResponse::from).toList();
        }
        if (actor.getBranchId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No branch is assigned to this account.");
        }
        return branches.findById(actor.getBranchId()).stream().map(BranchResponse::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<BranchResponse> create(@Valid @RequestBody BranchRequest request) {
        if (branches.existsByCodeIgnoreCase(request.code().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Branch code already exists.");
        }
        var branch = branches.save(new com.littlehome.school.identity.BranchEntity(
                request.code().trim().toUpperCase(), request.name().trim(), request.phone(), request.email(), request.city()));
        return ResponseEntity.created(URI.create("/api/v1/branches/" + branch.getId())).body(BranchResponse.from(branch));
    }

    @PatchMapping("/{branchId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public BranchResponse update(@PathVariable UUID branchId, @Valid @RequestBody BranchUpdateRequest request) {
        var branch = branches.findById(branchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found."));
        branch.update(trimOrNull(request.name()), request.phone(), request.email(), request.city(), request.status());
        return BranchResponse.from(branch);
    }

    private String trimOrNull(String value) { return value == null ? null : value.trim(); }
}