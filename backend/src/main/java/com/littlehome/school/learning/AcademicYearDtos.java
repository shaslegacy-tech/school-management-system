package com.littlehome.school.learning;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public final class AcademicYearDtos {
    private AcademicYearDtos() { }

    public record CreateRequest(UUID branchId, @NotBlank @Size(max = 32) String name,
                                @NotNull LocalDate startsOn, @NotNull LocalDate endsOn,
                                boolean current) { }
    public record AcademicYearResponse(UUID id, UUID branchId, String name, LocalDate startsOn,
                                       LocalDate endsOn, boolean current) {
        static AcademicYearResponse from(AcademicYearEntity year) {
            return new AcademicYearResponse(year.getId(), year.getBranchId(), year.getName(), year.getStartsOn(), year.getEndsOn(), year.isCurrent());
        }
    }
}