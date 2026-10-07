package com.littlehome.school.learning;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolClassRepository extends JpaRepository<SchoolClassEntity, UUID> {
    List<SchoolClassEntity> findAllByOrderByDisplayOrderAsc();
}