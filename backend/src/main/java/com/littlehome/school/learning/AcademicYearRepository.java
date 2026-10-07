package com.littlehome.school.learning;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicYearRepository extends JpaRepository<AcademicYearEntity, UUID> {
    List<AcademicYearEntity> findByBranchIdOrderByStartsOnDesc(UUID branchId);
    List<AcademicYearEntity> findAllByOrderByStartsOnDesc();
    boolean existsByBranchIdAndNameIgnoreCase(UUID branchId, String name);
}