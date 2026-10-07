package com.littlehome.school.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

@Entity
@Table(name = "branches")
public class BranchEntity {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String phone;
    private String email;
    private String city;

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Version
    private long version;

    protected BranchEntity() { }

    public BranchEntity(String code, String name, String phone, String email, String city) {
        this.id = UUID.randomUUID();
        this.code = code;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.city = city;
    }

    public void update(String name, String phone, String email, String city, String status) {
        if (name != null) this.name = name;
        if (phone != null) this.phone = phone;
        if (email != null) this.email = email;
        if (city != null) this.city = city;
        if (status != null) this.status = status;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getCity() { return city; }
    public String getStatus() { return status; }
}