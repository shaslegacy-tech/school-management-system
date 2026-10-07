package com.littlehome.school.identity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    @EntityGraph(attributePaths = {"role", "branch"})
    Optional<UserEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Override
    @EntityGraph(attributePaths = {"role", "branch"})
    Page<UserEntity> findAll(Pageable pageable);
}