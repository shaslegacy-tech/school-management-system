package com.littlehome.school.learning;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "school_classes")
public class SchoolClassEntity {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true)
    private String code;
    @Column(nullable = false, unique = true)
    private String name;
    @Column(name = "display_order", nullable = false, unique = true)
    private short displayOrder;

    protected SchoolClassEntity() { }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public short getDisplayOrder() { return displayOrder; }
}