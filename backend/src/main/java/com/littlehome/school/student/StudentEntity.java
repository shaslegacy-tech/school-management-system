package com.littlehome.school.student;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "students")
public class StudentEntity {
    @Id
    private UUID id;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "section_id")
    private UUID sectionId;

    @Column(name = "admission_number", nullable = false)
    private String admissionNumber;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "gender")
    private String gender;

    @Column(name = "status", nullable = false)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected StudentEntity() { }

    public StudentEntity(UUID branchId, UUID sectionId, String admissionNumber, String firstName,
                         String lastName, LocalDate dateOfBirth, String gender, UUID createdBy) {
        this.id = UUID.randomUUID();
        this.branchId = branchId;
        this.sectionId = sectionId;
        this.admissionNumber = admissionNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.status = "ACTIVE";
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
    }

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    public UUID getId() { return id; }
    public UUID getBranchId() { return branchId; }
    public UUID getSectionId() { return sectionId; }
    public String getAdmissionNumber() { return admissionNumber; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getGender() { return gender; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void updateProfile(String firstName, String lastName, LocalDate dateOfBirth, String gender,
                              UUID sectionId, UUID updatedBy) {
        if (firstName != null) this.firstName = firstName;
        if (lastName != null) this.lastName = lastName;
        if (dateOfBirth != null) this.dateOfBirth = dateOfBirth;
        if (gender != null) this.gender = gender;
        if (sectionId != null) this.sectionId = sectionId;
        this.updatedBy = updatedBy;
    }
}