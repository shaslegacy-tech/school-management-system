package com.littlehome.school.student;

import static com.littlehome.school.student.StudentDtos.AdmissionRequest;
import static com.littlehome.school.student.StudentDtos.StudentResponse;
import static com.littlehome.school.student.StudentDtos.StudentUpdateRequest;

import com.littlehome.school.identity.UserEntity;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/students")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_ADMIN')")
public class StudentController {
    private final StudentService studentService;

    public StudentController(StudentService studentService) { this.studentService = studentService; }

    @GetMapping
    public Page<StudentResponse> search(
            @AuthenticationPrincipal UserEntity actor,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return studentService.search(actor, branchId, search, page, size);
    }

    @PostMapping
    public ResponseEntity<StudentResponse> admit(
            @AuthenticationPrincipal UserEntity actor,
            @Valid @RequestBody AdmissionRequest request) {
        StudentResponse student = studentService.admit(actor, request);
        return ResponseEntity.created(URI.create("/api/v1/students/" + student.id())).body(student);
    }

    @GetMapping("/{studentId}")
    public StudentResponse get(@AuthenticationPrincipal UserEntity actor, @PathVariable UUID studentId) {
        return studentService.get(actor, studentId);
    }

    @PatchMapping("/{studentId}")
    public StudentResponse update(@AuthenticationPrincipal UserEntity actor,
                                  @PathVariable UUID studentId,
                                  @Valid @RequestBody StudentUpdateRequest request) {
        return studentService.update(actor, studentId, request);
    }
}