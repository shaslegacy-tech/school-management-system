package com.littlehome.school.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.time.Instant;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Table(name = "users")
public class UserEntity implements UserDetails {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private BranchEntity branch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected UserEntity() { }

    public UserEntity(RoleEntity role, String email, String passwordHash, String firstName, String lastName) {
        this.id = UUID.randomUUID();
        this.role = role;
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.active = true;
    }

    public UserEntity(BranchEntity branch, RoleEntity role, String email, String passwordHash,
                      String firstName, String lastName) {
        this(role, email, passwordHash, firstName, lastName);
        this.branch = branch;
    }

    public UUID getId() { return id; }
    public UUID getBranchId() { return branch == null ? null : branch.getId(); }
    public String getRoleCode() { return role.getCode(); }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getBranchName() { return branch == null ? null : branch.getName(); }
    public Instant getCreatedAt() { return createdAt; }
    public String getDisplayName() { return firstName + " " + lastName; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(new SimpleGrantedAuthority("ROLE_" + role.getCode())); }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isEnabled() { return active; }
}