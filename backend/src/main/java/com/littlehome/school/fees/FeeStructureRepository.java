package com.littlehome.school.fees;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeeStructureRepository extends JpaRepository<FeeStructureEntity, UUID> {
    List<FeeStructureEntity> findAllByOrderByFeeNameAsc();
    List<FeeStructureEntity> findByBranchIdOrderByFeeNameAsc(UUID branchId);
}