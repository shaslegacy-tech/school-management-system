package com.littlehome.school.attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRepository extends JpaRepository<AttendanceEntity, UUID> {
    Optional<AttendanceEntity> findByStudentIdAndAttendanceDate(UUID studentId, LocalDate date);
    List<AttendanceEntity> findAllByAttendanceDateOrderByMarkedAtDesc(LocalDate date);
    List<AttendanceEntity> findByBranchIdAndAttendanceDateOrderByMarkedAtDesc(UUID branchId, LocalDate date);
    List<AttendanceEntity> findByBranchIdAndAttendanceDateAndSectionIdOrderByMarkedAtDesc(UUID branchId, LocalDate date, UUID sectionId);
}