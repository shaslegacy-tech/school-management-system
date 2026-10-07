package com.littlehome.school.fees;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "fee_structures")
public class FeeStructureEntity {
    @Id
    private UUID id;
    @Column(name = "branch_id", nullable = false)
    private UUID branchId;
    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;
    @Column(name = "class_id", nullable = false)
    private UUID classId;
    @Column(name = "fee_name", nullable = false)
    private String feeName;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3, columnDefinition = "char(3)")
    private String currency;
    @Column(name = "due_day")
    private Short dueDay;
    @Version
    private long version;

    protected FeeStructureEntity() { }

    public FeeStructureEntity(UUID branchId, UUID academicYearId, UUID classId, String feeName,
                              BigDecimal amount, String currency, Short dueDay) {
        this.id = UUID.randomUUID();
        this.branchId = branchId;
        this.academicYearId = academicYearId;
        this.classId = classId;
        this.feeName = feeName;
        this.amount = amount;
        this.currency = currency;
        this.dueDay = dueDay;
    }

    public UUID getId() { return id; }
    public UUID getBranchId() { return branchId; }
    public UUID getAcademicYearId() { return academicYearId; }
    public UUID getClassId() { return classId; }
    public String getFeeName() { return feeName; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public Short getDueDay() { return dueDay; }
}