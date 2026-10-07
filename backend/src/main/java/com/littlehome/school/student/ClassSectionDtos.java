package com.littlehome.school.student;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class ClassSectionDtos {
    private ClassSectionDtos() { }

    public record CreateRequest(UUID branchId, @NotNull UUID academicYearId, @NotNull UUID classId,
                                @NotBlank @Size(max = 16) String sectionName,
                                @Min(1) @Max(100) short capacity) { }
    public record SectionResponse(UUID id, UUID branchId, UUID academicYearId, UUID classId,
                                  String sectionName, short capacity, UUID homeroomTeacherId) {
        static SectionResponse from(ClassSectionEntity section) {
            return new SectionResponse(section.getId(), section.getBranchId(), section.getAcademicYearId(),
                    section.getClassId(), section.getSectionName(), section.getCapacity(), section.getHomeroomTeacherId());
        }
    }
}