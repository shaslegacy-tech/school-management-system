package com.littlehome.school.attendance;

import static com.littlehome.school.attendance.AttendanceDtos.AttendanceResponse;
import static com.littlehome.school.attendance.AttendanceDtos.RegisterRequest;

import com.littlehome.school.identity.UserEntity;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/attendance")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_ADMIN', 'TEACHER')")
public class AttendanceController {
    private final AttendanceService service;

    public AttendanceController(AttendanceService service) { this.service = service; }

    @GetMapping
    public List<AttendanceResponse> list(@AuthenticationPrincipal UserEntity actor,
                                         @RequestParam(required = false) UUID branchId,
                                         @RequestParam(required = false) UUID sectionId,
                                         @RequestParam LocalDate date) {
        return service.list(actor, branchId, sectionId, date);
    }

    @PostMapping
    public List<AttendanceResponse> mark(@AuthenticationPrincipal UserEntity actor,
                                         @RequestParam(required = false) UUID branchId,
                                         @Valid @RequestBody RegisterRequest request) {
        return service.mark(actor, branchId, request);
    }
}