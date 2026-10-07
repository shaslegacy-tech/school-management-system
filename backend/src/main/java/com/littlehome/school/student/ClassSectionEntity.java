package com.littlehome.school.student;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

@Entity
@Table(name = "class_sections")
public class ClassSectionEntity {
    @Id
    private UUID id;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(name = "section_name", nullable = false)
    private String sectionName;

    @Column(nullable = false)
    private short capacity;

    @Column(name = "homeroom_teacher_id")
    private UUID homeroomTeacherId;

    @Version
    private long version;

    protected ClassSectionEntity() { }

    public ClassSectionEntity(UUID branchId, UUID academicYearId, UUID classId, String sectionName, short capacity) {
        this.id = UUID.randomUUID();
        this.branchId = branchId;
        this.academicYearId = academicYearId;
        this.classId = classId;
        this.sectionName = sectionName;
        this.capacity = capacity;
    }

    public UUID getId() { return id; }
    public UUID getBranchId() { return branchId; }
    public UUID getAcademicYearId() { return academicYearId; }
    public UUID getClassId() { return classId; }
    public String getSectionName() { return sectionName; }
    public short getCapacity() { return capacity; }
    public UUID getHomeroomTeacherId() { return homeroomTeacherId; }
}