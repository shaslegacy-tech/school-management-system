package com.littlehome.school.identity;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<BranchEntity, UUID> {
	java.util.Optional<BranchEntity> findByCode(String code);
	boolean existsByCodeIgnoreCase(String code);
}