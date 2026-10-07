package com.littlehome.school.student;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class StudentDtos {
    private StudentDtos() { }

    public record AdmissionRequest(
            UUID branchId,
            UUID sectionId,
            @NotBlank @Size(max = 40) String admissionNumber,
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @NotNull @Past LocalDate dateOfBirth,
            @Size(max = 24) String gender) { }

    public record StudentUpdateRequest(
            @Size(max = 80) String firstName,
            @Size(max = 80) String lastName,
            @Past LocalDate dateOfBirth,
            @Size(max = 24) String gender,
            UUID sectionId) { }

    public record StudentResponse(
            UUID id, UUID branchId, UUID sectionId, String admissionNumber,
            String firstName, String lastName, LocalDate dateOfBirth,
            String gender, String status, Instant createdAt) {
        static StudentResponse from(StudentEntity student) {
            return new StudentResponse(student.getId(), student.getBranchId(), student.getSectionId(),
                    student.getAdmissionNumber(), student.getFirstName(), student.getLastName(),
                    student.getDateOfBirth(), student.getGender(), student.getStatus(), student.getCreatedAt());
        }
    }
}