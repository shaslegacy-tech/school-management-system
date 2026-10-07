package com.littlehome.school.learning;

import static com.littlehome.school.learning.AcademicYearDtos.AcademicYearResponse;
import static com.littlehome.school.learning.AcademicYearDtos.CreateRequest;

import com.littlehome.school.identity.BranchRepository;
import com.littlehome.school.identity.UserEntity;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/academic-years")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_ADMIN')")
public class AcademicYearController {
    private final AcademicYearRepository years;
    private final BranchRepository branches;

    public AcademicYearController(AcademicYearRepository years, BranchRepository branches) {
        this.years = years;
        this.branches = branches;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<AcademicYearResponse> list(@AuthenticationPrincipal UserEntity actor, @RequestParam(required = false) UUID branchId) {
        UUID scope = branchScope(actor, branchId);
        return (scope == null ? years.findAllByOrderByStartsOnDesc() : years.findByBranchIdOrderByStartsOnDesc(scope))
                .stream().map(AcademicYearResponse::from).toList();
    }

    @PostMapping
    @Transactional
    public ResponseEntity<AcademicYearResponse> create(@AuthenticationPrincipal UserEntity actor, @Valid @RequestBody CreateRequest request) {
        UUID branchId = branchScope(actor, request.branchId());
        if (branchId == null || !branches.existsById(branchId)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid branch is required.");
        if (!request.endsOn().isAfter(request.startsOn())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date must be after start date.");
        if (years.existsByBranchIdAndNameIgnoreCase(branchId, request.name().trim())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Academic year already exists for this branch.");
        if (request.current()) years.findByBranchIdOrderByStartsOnDesc(branchId).forEach(year -> year.setCurrent(false));
        var year = years.save(new AcademicYearEntity(branchId, request.name().trim(), request.startsOn(), request.endsOn(), request.current()));
        return ResponseEntity.created(URI.create("/api/v1/academic-years/" + year.getId())).body(AcademicYearResponse.from(year));
    }

    private UUID branchScope(UserEntity actor, UUID requestedBranchId) {
        if ("SUPER_ADMIN".equals(actor.getRoleCode())) return requestedBranchId;
        UUID ownBranch = actor.getBranchId();
        if (ownBranch == null || (requestedBranchId != null && !ownBranch.equals(requestedBranchId))) {
            throw new org.springframework.security.access.AccessDeniedException("Branch is outside the user's scope.");
        }
        return ownBranch;
    }
}