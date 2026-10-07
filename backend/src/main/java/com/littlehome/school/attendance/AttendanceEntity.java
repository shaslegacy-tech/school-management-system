package com.littlehome.school.attendance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "attendance")
public class AttendanceEntity {
    @Id
    private UUID id;
    @Column(name = "student_id", nullable = false)
    private UUID studentId;
    @Column(name = "branch_id", nullable = false)
    private UUID branchId;
    @Column(name = "section_id")
    private UUID sectionId;
    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;
    @Column(nullable = false)
    private String status;
    @Column(name = "marked_by", nullable = false)
    private UUID markedBy;
    @Column(name = "marked_at", nullable = false)
    private Instant markedAt;
    private String remarks;
    @Version
    private long version;

    protected AttendanceEntity() { }

    public AttendanceEntity(UUID studentId, UUID branchId, UUID sectionId, LocalDate date,
                            String status, UUID markedBy, String remarks) {
        this.id = UUID.randomUUID();
        this.studentId = studentId;
        this.branchId = branchId;
        this.sectionId = sectionId;
        this.attendanceDate = date;
        this.status = status;
        this.markedBy = markedBy;
        this.markedAt = Instant.now();
        this.remarks = remarks;
    }

    public void update(String status, UUID markedBy, String remarks) {
        this.status = status;
        this.markedBy = markedBy;
        this.markedAt = Instant.now();
        this.remarks = remarks;
    }

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public UUID getBranchId() { return branchId; }
    public UUID getSectionId() { return sectionId; }
    public LocalDate getAttendanceDate() { return attendanceDate; }
    public String getStatus() { return status; }
    public UUID getMarkedBy() { return markedBy; }
    public Instant getMarkedAt() { return markedAt; }
    public String getRemarks() { return remarks; }
}