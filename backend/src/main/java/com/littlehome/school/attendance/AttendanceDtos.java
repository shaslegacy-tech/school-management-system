package com.littlehome.school.attendance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class AttendanceDtos {
    private AttendanceDtos() { }

    public record MarkRecord(@NotNull UUID studentId,
                             @NotNull @Pattern(regexp = "PRESENT|ABSENT|LEAVE") String status,
                             @Size(max = 500) String remarks) { }
    public record RegisterRequest(@NotNull UUID sectionId, @NotNull LocalDate date,
                                  @NotEmpty @Valid List<MarkRecord> records) { }
    public record AttendanceResponse(UUID id, UUID studentId, UUID branchId, UUID sectionId,
                                     LocalDate date, String status, Instant markedAt, String remarks) {
        static AttendanceResponse from(AttendanceEntity entity) {
            return new AttendanceResponse(entity.getId(), entity.getStudentId(), entity.getBranchId(),
                    entity.getSectionId(), entity.getAttendanceDate(), entity.getStatus(), entity.getMarkedAt(), entity.getRemarks());
        }
    }
}