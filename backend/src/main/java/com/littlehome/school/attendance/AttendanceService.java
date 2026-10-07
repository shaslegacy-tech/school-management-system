package com.littlehome.school.attendance;

import static com.littlehome.school.attendance.AttendanceDtos.AttendanceResponse;
import static com.littlehome.school.attendance.AttendanceDtos.RegisterRequest;

import com.littlehome.school.identity.UserEntity;
import com.littlehome.school.student.ClassSectionRepository;
import com.littlehome.school.student.StudentRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AttendanceService {
    private final AttendanceRepository attendance;
    private final ClassSectionRepository sections;
    private final StudentRepository students;

    public AttendanceService(AttendanceRepository attendance, ClassSectionRepository sections, StudentRepository students) {
        this.attendance = attendance;
        this.sections = sections;
        this.students = students;
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> list(UserEntity actor, UUID requestedBranchId, UUID sectionId, LocalDate date) {
        UUID branchId = branchScope(actor, requestedBranchId);
        if (sectionId != null) {
            if (branchId == null) {
                var section = sections.findById(sectionId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found."));
                branchId = section.getBranchId();
            }
            requireSection(branchId, sectionId);
            return attendance.findByBranchIdAndAttendanceDateAndSectionIdOrderByMarkedAtDesc(branchId, date, sectionId)
                    .stream().map(AttendanceResponse::from).toList();
        }
        if (branchId == null) return attendance.findAllByAttendanceDateOrderByMarkedAtDesc(date).stream().map(AttendanceResponse::from).toList();
        return attendance.findByBranchIdAndAttendanceDateOrderByMarkedAtDesc(branchId, date).stream().map(AttendanceResponse::from).toList();
    }

    @Transactional
    public List<AttendanceResponse> mark(UserEntity actor, UUID requestedBranchId, RegisterRequest request) {
        UUID branchId = branchScope(actor, requestedBranchId);
        if (branchId == null) {
            var section = sections.findById(request.sectionId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found."));
            branchId = section.getBranchId();
        }
        requireSection(branchId, request.sectionId());
        UUID resolvedBranchId = branchId;
        return request.records().stream().map(record -> {
            if (!students.existsByIdAndSectionIdAndBranchId(record.studentId(), request.sectionId(), resolvedBranchId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Every student must belong to the selected section and branch.");
            }
            AttendanceEntity entry = attendance.findByStudentIdAndAttendanceDate(record.studentId(), request.date())
                    .map(existing -> {
                        if (!resolvedBranchId.equals(existing.getBranchId())) throw new AccessDeniedException("Attendance is outside the branch scope.");
                        existing.update(record.status(), actor.getId(), record.remarks());
                        return existing;
                    })
                    .orElseGet(() -> attendance.save(new AttendanceEntity(record.studentId(), resolvedBranchId,
                            request.sectionId(), request.date(), record.status(), actor.getId(), record.remarks())));
            return AttendanceResponse.from(entry);
        }).toList();
    }

    private void requireSection(UUID branchId, UUID sectionId) {
        if (!sections.existsByIdAndBranchId(sectionId, branchId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Section is not in the authorized branch.");
        }
    }

    private UUID branchScope(UserEntity actor, UUID requestedBranchId) {
        if ("SUPER_ADMIN".equals(actor.getRoleCode())) return requestedBranchId;
        UUID ownBranch = actor.getBranchId();
        if (ownBranch == null || (requestedBranchId != null && !ownBranch.equals(requestedBranchId))) {
            throw new AccessDeniedException("Requested branch is outside the user's authorization scope.");
        }
        return ownBranch;
    }
}