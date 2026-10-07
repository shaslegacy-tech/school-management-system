package com.littlehome.school.learning;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "academic_years")
public class AcademicYearEntity {
    @Id
    private UUID id;
    @Column(name = "branch_id", nullable = false)
    private UUID branchId;
    @Column(nullable = false)
    private String name;
    @Column(name = "starts_on", nullable = false)
    private LocalDate startsOn;
    @Column(name = "ends_on", nullable = false)
    private LocalDate endsOn;
    @Column(name = "is_current", nullable = false)
    private boolean current;
    @Version
    private long version;

    protected AcademicYearEntity() { }

    public AcademicYearEntity(UUID branchId, String name, LocalDate startsOn, LocalDate endsOn, boolean current) {
        this.id = UUID.randomUUID();
        this.branchId = branchId;
        this.name = name;
        this.startsOn = startsOn;
        this.endsOn = endsOn;
        this.current = current;
    }

    public UUID getId() { return id; }
    public UUID getBranchId() { return branchId; }
    public String getName() { return name; }
    public LocalDate getStartsOn() { return startsOn; }
    public LocalDate getEndsOn() { return endsOn; }
    public boolean isCurrent() { return current; }
    public void setCurrent(boolean current) { this.current = current; }
}