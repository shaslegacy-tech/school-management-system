package com.littlehome.school.student;

import static com.littlehome.school.student.ClassSectionDtos.CreateRequest;
import static com.littlehome.school.student.ClassSectionDtos.SectionResponse;

import com.littlehome.school.identity.BranchRepository;
import com.littlehome.school.identity.UserEntity;
import com.littlehome.school.learning.AcademicYearRepository;
import com.littlehome.school.learning.SchoolClassRepository;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
@RequestMapping("/api/v1/sections")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_ADMIN', 'TEACHER')")
public class ClassSectionController {
    private final ClassSectionRepository sections;
    private final BranchRepository branches;
    private final AcademicYearRepository years;
    private final SchoolClassRepository classes;

    public ClassSectionController(ClassSectionRepository sections, BranchRepository branches,
                                  AcademicYearRepository years, SchoolClassRepository classes) {
        this.sections = sections;
        this.branches = branches;
        this.years = years;
        this.classes = classes;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<SectionResponse> list(@AuthenticationPrincipal UserEntity actor, @RequestParam(required = false) UUID branchId) {
        UUID scope = branchScope(actor, branchId);
        return (scope == null ? sections.findAllByOrderBySectionNameAsc() : sections.findByBranchIdOrderBySectionNameAsc(scope))
                .stream().map(SectionResponse::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_ADMIN')")
    @Transactional
    public ResponseEntity<SectionResponse> create(@AuthenticationPrincipal UserEntity actor,
                                                   @Valid @RequestBody CreateRequest request) {
        UUID branchId = branchScope(actor, request.branchId());
        if (branchId == null || !branches.existsById(branchId)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid branch is required.");
        var year = years.findById(request.academicYearId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Academic year not found."));
        if (!branchId.equals(year.getBranchId()) || !classes.existsById(request.classId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Academic year and class must be valid for this branch.");
        }
        String sectionName = request.sectionName().trim();
        if (sections.existsByBranchIdAndAcademicYearIdAndClassIdAndSectionNameIgnoreCase(branchId, year.getId(), request.classId(), sectionName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Section already exists for this class and year.");
        }
        var section = sections.save(new ClassSectionEntity(branchId, year.getId(), request.classId(), sectionName, request.capacity()));
        return ResponseEntity.created(URI.create("/api/v1/sections/" + section.getId())).body(SectionResponse.from(section));
    }

    private UUID branchScope(UserEntity actor, UUID requestedBranchId) {
        if ("SUPER_ADMIN".equals(actor.getRoleCode())) return requestedBranchId;
        UUID ownBranch = actor.getBranchId();
        if (ownBranch == null || (requestedBranchId != null && !ownBranch.equals(requestedBranchId))) {
            throw new AccessDeniedException("Branch is outside the user's scope.");
        }
        return ownBranch;
    }
}