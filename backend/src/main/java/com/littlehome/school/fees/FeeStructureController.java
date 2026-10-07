package com.littlehome.school.fees;

import static com.littlehome.school.fees.FeeStructureDtos.CreateRequest;
import static com.littlehome.school.fees.FeeStructureDtos.FeeResponse;

import com.littlehome.school.identity.BranchRepository;
import com.littlehome.school.learning.AcademicYearRepository;
import com.littlehome.school.learning.SchoolClassRepository;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/fees/structures")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class FeeStructureController {
    private final FeeStructureRepository fees;
    private final BranchRepository branches;
    private final AcademicYearRepository years;
    private final SchoolClassRepository classes;

    public FeeStructureController(FeeStructureRepository fees, BranchRepository branches,
                                  AcademicYearRepository years, SchoolClassRepository classes) {
        this.fees = fees;
        this.branches = branches;
        this.years = years;
        this.classes = classes;
    }

    @GetMapping
    public List<FeeResponse> list(@RequestParam(required = false) java.util.UUID branchId) {
        return (branchId == null ? fees.findAllByOrderByFeeNameAsc() : fees.findByBranchIdOrderByFeeNameAsc(branchId))
                .stream().map(FeeResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<FeeResponse> create(@Valid @RequestBody CreateRequest request) {
        if (!branches.existsById(request.branchId()) || !years.existsById(request.academicYearId()) || !classes.existsById(request.classId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Branch, academic year, and class must exist.");
        }
        var fee = fees.save(new FeeStructureEntity(request.branchId(), request.academicYearId(), request.classId(),
                request.feeName().trim(), request.amount(), request.currency() == null ? "INR" : request.currency(), request.dueDay()));
        return ResponseEntity.created(URI.create("/api/v1/fees/structures/" + fee.getId())).body(FeeResponse.from(fee));
    }
}