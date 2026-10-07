package com.littlehome.school.communication;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementRepository extends JpaRepository<AnnouncementEntity, UUID> {
    List<AnnouncementEntity> findAllByOrderByPublishedAtDesc();
    List<AnnouncementEntity> findByBranchIdOrderByPublishedAtDesc(UUID branchId);
}