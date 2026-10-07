package com.littlehome.school.communication;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class AnnouncementDtos {
    private AnnouncementDtos() { }

    public record CreateRequest(UUID branchId, @NotBlank @Size(max = 180) String title,
                                @NotBlank @Size(max = 10000) String body,
                                @Pattern(regexp = "ALL|STAFF|PARENTS|TEACHERS") String audience) { }
    public record AnnouncementResponse(UUID id, UUID branchId, String title, String body,
                                       String audience, Instant publishedAt) {
        static AnnouncementResponse from(AnnouncementEntity announcement) {
            return new AnnouncementResponse(announcement.getId(), announcement.getBranchId(), announcement.getTitle(),
                    announcement.getBody(), announcement.getAudience(), announcement.getPublishedAt());
        }
    }
}