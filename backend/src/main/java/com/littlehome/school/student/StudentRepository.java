package com.littlehome.school.student;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StudentRepository extends JpaRepository<StudentEntity, UUID>, JpaSpecificationExecutor<StudentEntity> {

    boolean existsByBranchIdAndAdmissionNumberIgnoreCase(UUID branchId, String admissionNumber);

    boolean existsByIdAndSectionIdAndBranchId(UUID id, UUID sectionId, UUID branchId);

    java.util.Optional<StudentEntity> findByBranchIdAndAdmissionNumberIgnoreCase(UUID branchId, String admissionNumber);
}