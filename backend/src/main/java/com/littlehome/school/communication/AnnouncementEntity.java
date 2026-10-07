package com.littlehome.school.communication;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "announcements")
public class AnnouncementEntity {
    @Id
    private UUID id;
    @Column(name = "branch_id")
    private UUID branchId;
    @Column(name = "author_id", nullable = false)
    private UUID authorId;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false)
    private String body;
    @Column(nullable = false)
    private String audience;
    @Column(name = "published_at")
    private Instant publishedAt;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;
    @Version
    private long version;

    protected AnnouncementEntity() { }

    public AnnouncementEntity(UUID branchId, UUID authorId, String title, String body, String audience) {
        this.id = UUID.randomUUID();
        this.branchId = branchId;
        this.authorId = authorId;
        this.title = title;
        this.body = body;
        this.audience = audience;
        this.publishedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getBranchId() { return branchId; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getAudience() { return audience; }
    public Instant getPublishedAt() { return publishedAt; }
}