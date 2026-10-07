package com.littlehome.school.student;

import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassSectionRepository extends JpaRepository<ClassSectionEntity, UUID> {
    boolean existsByIdAndBranchId(UUID id, UUID branchId);
    List<ClassSectionEntity> findByBranchIdOrderBySectionNameAsc(UUID branchId);
    List<ClassSectionEntity> findAllByOrderBySectionNameAsc();
    boolean existsByBranchIdAndAcademicYearIdAndClassIdAndSectionNameIgnoreCase(UUID branchId, UUID academicYearId, UUID classId, String sectionName);
}