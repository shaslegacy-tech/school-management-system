package com.littlehome.school.student;

import static com.littlehome.school.student.StudentDtos.AdmissionRequest;
import static com.littlehome.school.student.StudentDtos.StudentResponse;
import static com.littlehome.school.student.StudentDtos.StudentUpdateRequest;

import com.littlehome.school.identity.UserEntity;
import com.littlehome.school.identity.BranchRepository;
import java.util.UUID;
import java.util.ArrayList;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StudentService {
    private final StudentRepository students;
    private final ClassSectionRepository sections;
    private final BranchRepository branches;

    public StudentService(StudentRepository students, ClassSectionRepository sections, BranchRepository branches) {
        this.students = students;
        this.sections = sections;
        this.branches = branches;
    }

    @Transactional(readOnly = true)
    public Page<StudentResponse> search(UserEntity actor, UUID requestedBranchId, String search, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be non-negative and size must be between 1 and 100.");
        }
        UUID branchScope = branchScope(actor, requestedBranchId);
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim().toLowerCase();
        Specification<StudentEntity> specification = (root, query, criteria) -> {
            var predicates = new ArrayList<Predicate>();
            if (branchScope != null) predicates.add(criteria.equal(root.get("branchId"), branchScope));
            if (normalizedSearch != null) {
                String pattern = "%" + normalizedSearch + "%";
                var fullName = criteria.lower(criteria.concat(criteria.concat(root.<String>get("firstName"), " "), root.<String>get("lastName")));
                var admissionNumber = criteria.lower(root.<String>get("admissionNumber"));
                predicates.add(criteria.or(criteria.like(fullName, pattern), criteria.like(admissionNumber, pattern)));
            }
            return criteria.and(predicates.toArray(Predicate[]::new));
        };
        return students.findAll(specification, PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(StudentResponse::from);
    }

    @Transactional
    public StudentResponse admit(UserEntity actor, AdmissionRequest request) {
        UUID branchId = branchScope(actor, request.branchId());
        if (branchId == null || !branches.existsById(branchId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid branch is required for admission.");
        }
        if (request.sectionId() != null && !sections.existsByIdAndBranchId(request.sectionId(), branchId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The selected section is not in the authorized branch.");
        }
        if (students.existsByBranchIdAndAdmissionNumberIgnoreCase(branchId, request.admissionNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Admission number already exists in this branch.");
        }
        StudentEntity student = students.save(new StudentEntity(branchId, request.sectionId(), request.admissionNumber().trim(),
                request.firstName().trim(), request.lastName().trim(), request.dateOfBirth(), request.gender(), actor.getId()));
        return StudentResponse.from(student);
    }

    @Transactional(readOnly = true)
    public StudentResponse get(UserEntity actor, UUID studentId) {
        StudentEntity student = authorizedStudent(actor, studentId);
        return StudentResponse.from(student);
    }

    @Transactional
    public StudentResponse update(UserEntity actor, UUID studentId, StudentUpdateRequest request) {
        StudentEntity student = authorizedStudent(actor, studentId);
        if (request.sectionId() != null && !sections.existsByIdAndBranchId(request.sectionId(), student.getBranchId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The selected section is not in the student's branch.");
        }
        student.updateProfile(trimOrNull(request.firstName()), trimOrNull(request.lastName()),
                request.dateOfBirth(), trimOrNull(request.gender()), request.sectionId(), actor.getId());
        return StudentResponse.from(student);
    }

    private UUID branchScope(UserEntity actor, UUID requestedBranchId) {
        if ("SUPER_ADMIN".equals(actor.getRoleCode())) {
            if (requestedBranchId == null) return null;
            return requestedBranchId;
        }
        UUID actorBranchId = actor.getBranchId();
        if (actorBranchId == null || (requestedBranchId != null && !actorBranchId.equals(requestedBranchId))) {
            throw new AccessDeniedException("Requested branch is outside the user's authorization scope.");
        }
        return actorBranchId;
    }

    private StudentEntity authorizedStudent(UserEntity actor, UUID studentId) {
        StudentEntity student = students.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found."));
        if (!"SUPER_ADMIN".equals(actor.getRoleCode()) && !student.getBranchId().equals(actor.getBranchId())) {
            throw new AccessDeniedException("Student is outside the user's branch scope.");
        }
        return student;
    }

    private String trimOrNull(String value) {
        return value == null ? null : value.trim();
    }
}