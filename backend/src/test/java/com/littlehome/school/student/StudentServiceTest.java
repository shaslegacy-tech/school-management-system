package com.littlehome.school.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.littlehome.school.identity.BranchRepository;
import com.littlehome.school.identity.UserEntity;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class StudentServiceTest {
    private StudentRepository students;
    private ClassSectionRepository sections;
    private BranchRepository branches;
    private StudentService service;

    @BeforeEach
    void setUp() {
        students = mock(StudentRepository.class);
        sections = mock(ClassSectionRepository.class);
        branches = mock(BranchRepository.class);
        service = new StudentService(students, sections, branches);
    }

    @Test
    void rejectsBranchAdminRequestOutsideAssignedBranch() {
        UserEntity actor = mock(UserEntity.class);
        UUID assignedBranchId = UUID.randomUUID();
        when(actor.getRoleCode()).thenReturn("BRANCH_ADMIN");
        when(actor.getBranchId()).thenReturn(assignedBranchId);
        var request = new StudentDtos.AdmissionRequest(UUID.randomUUID(), null, "LHS-1", "Aarav", "Sharma", LocalDate.now().minusYears(4), null);

        assertThatThrownBy(() -> service.admit(actor, request)).isInstanceOf(AccessDeniedException.class);
        verify(students, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createsStudentWithinAssignedBranch() {
        UserEntity actor = mock(UserEntity.class);
        UUID branchId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(actor.getRoleCode()).thenReturn("BRANCH_ADMIN");
        when(actor.getBranchId()).thenReturn(branchId);
        when(actor.getId()).thenReturn(userId);
        when(branches.existsById(branchId)).thenReturn(true);
        when(students.existsByBranchIdAndAdmissionNumberIgnoreCase(branchId, "LHS-2")).thenReturn(false);
        when(students.save(org.mockito.ArgumentMatchers.any(StudentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var request = new StudentDtos.AdmissionRequest(null, null, "LHS-2", " Mira ", "Kapoor", LocalDate.now().minusYears(3), null);

        var response = service.admit(actor, request);

        assertThat(response.branchId()).isEqualTo(branchId);
        assertThat(response.firstName()).isEqualTo("Mira");
        assertThat(response.lastName()).isEqualTo("Kapoor");
    }

    @Test
    void deniesProfileReadOutsideAssignedBranch() {
        UserEntity actor = mock(UserEntity.class);
        StudentEntity student = mock(StudentEntity.class);
        UUID branchId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        when(actor.getRoleCode()).thenReturn("BRANCH_ADMIN");
        when(actor.getBranchId()).thenReturn(branchId);
        when(students.findById(studentId)).thenReturn(Optional.of(student));
        when(student.getBranchId()).thenReturn(UUID.randomUUID());

        assertThatThrownBy(() -> service.get(actor, studentId)).isInstanceOf(AccessDeniedException.class);
    }
}