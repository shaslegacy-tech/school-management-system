package com.littlehome.school.fees;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public final class FeeStructureDtos {
    private FeeStructureDtos() { }

    public record CreateRequest(@NotNull UUID branchId, @NotNull UUID academicYearId, @NotNull UUID classId,
                                @NotBlank @Size(max = 100) String feeName,
                                @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal amount,
                                @Pattern(regexp = "[A-Z]{3}") String currency,
                                @Min(1) @Max(28) Short dueDay) { }
    public record FeeResponse(UUID id, UUID branchId, UUID academicYearId, UUID classId,
                              String feeName, BigDecimal amount, String currency, Short dueDay) {
        static FeeResponse from(FeeStructureEntity fee) {
            return new FeeResponse(fee.getId(), fee.getBranchId(), fee.getAcademicYearId(), fee.getClassId(),
                    fee.getFeeName(), fee.getAmount(), fee.getCurrency(), fee.getDueDay());
        }
    }
}