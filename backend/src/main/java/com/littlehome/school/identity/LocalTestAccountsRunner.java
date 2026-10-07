package com.littlehome.school.identity;

import com.littlehome.school.student.StudentEntity;
import com.littlehome.school.student.StudentRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")
@ConditionalOnProperty(prefix = "app.test-accounts", name = "enabled", havingValue = "true")
public class LocalTestAccountsRunner implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(LocalTestAccountsRunner.class);
    private static final String SUPER_ADMIN_EMAIL = "superadmin@test.littlehome.local";
    private static final String BRANCH_ADMIN_EMAIL = "branchadmin@test.littlehome.local";
    private static final String TEACHER_EMAIL = "teacher@test.littlehome.local";
    private static final String PARENT_EMAIL = "parent@test.littlehome.local";

    private final UserRepository users;
    private final RoleRepository roles;
    private final BranchRepository branches;
    private final StudentRepository students;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final String superAdminPassword;
    private final String branchAdminPassword;
    private final String teacherPassword;
    private final String parentPassword;

    public LocalTestAccountsRunner(
            UserRepository users,
            RoleRepository roles,
            BranchRepository branches,
            StudentRepository students,
            JdbcTemplate jdbc,
            PasswordEncoder encoder,
            @Value("${app.test-accounts.super-admin-password}") String superAdminPassword,
            @Value("${app.test-accounts.branch-admin-password}") String branchAdminPassword,
            @Value("${app.test-accounts.teacher-password}") String teacherPassword,
            @Value("${app.test-accounts.parent-password}") String parentPassword) {
        this.users = users;
        this.roles = roles;
        this.branches = branches;
        this.students = students;
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.superAdminPassword = superAdminPassword;
        this.branchAdminPassword = branchAdminPassword;
        this.teacherPassword = teacherPassword;
        this.parentPassword = parentPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        requireTestPasswords();
        BranchEntity central = branches.findByCode("LHS-CENTRAL")
                .orElseThrow(() -> new IllegalStateException("The LHS-CENTRAL seed branch is missing."));

        UserEntity superAdmin = createUser(null, "SUPER_ADMIN", SUPER_ADMIN_EMAIL, superAdminPassword, "Test", "Super Admin");
        UserEntity branchAdmin = createUser(central, "BRANCH_ADMIN", BRANCH_ADMIN_EMAIL, branchAdminPassword, "Test", "Branch Admin");
        UserEntity teacher = createUser(central, "TEACHER", TEACHER_EMAIL, teacherPassword, "Test", "Teacher");
        UserEntity parent = createUser(central, "PARENT", PARENT_EMAIL, parentPassword, "Test", "Parent");

        jdbc.update("""
                INSERT INTO teachers (user_id, employee_number)
                VALUES (?, 'TEST-TEACHER-001')
                ON CONFLICT (user_id) DO NOTHING
                """, teacher.getId());

        UUID parentId = jdbc.queryForObject("""
                INSERT INTO parents (user_id, first_name, last_name, email, phone)
                VALUES (?, 'Test', 'Parent', ?, '+910000000001')
                ON CONFLICT (user_id) DO UPDATE SET email = EXCLUDED.email
                RETURNING id
                """, UUID.class, parent.getId(), PARENT_EMAIL);

        StudentEntity child = createTestChild(central, branchAdmin);
        jdbc.update("""
                INSERT INTO student_parents (student_id, parent_id, relationship, is_primary, can_pick_up)
                VALUES (?, ?, 'PARENT', true, true)
                ON CONFLICT (student_id, parent_id) DO NOTHING
                """, child.getId(), parentId);

        logger.info("Local role test accounts are ready for Super Admin, Branch Admin, Teacher, and Parent.");
        logger.info("Local test account emails: {}, {}, {}, {}", SUPER_ADMIN_EMAIL, BRANCH_ADMIN_EMAIL, TEACHER_EMAIL, PARENT_EMAIL);
    }

    private UserEntity createUser(BranchEntity branch, String roleCode, String email, String rawPassword,
                                  String firstName, String lastName) {
        return users.findByEmailIgnoreCase(email).orElseGet(() -> {
            RoleEntity role = roles.findByCode(roleCode)
                    .orElseThrow(() -> new IllegalStateException("Seeded role is missing: " + roleCode));
            String passwordHash = encoder.encode(rawPassword);
            UserEntity user = branch == null
                    ? new UserEntity(role, email, passwordHash, firstName, lastName)
                    : new UserEntity(branch, role, email, passwordHash, firstName, lastName);
                        return users.saveAndFlush(user);
        });
    }

    private StudentEntity createTestChild(BranchEntity branch, UserEntity createdBy) {
        String admissionNumber = "TEST-CHILD-001";
        return students.findByBranchIdAndAdmissionNumberIgnoreCase(branch.getId(), admissionNumber)
                .orElseGet(() -> students.saveAndFlush(new StudentEntity(branch.getId(), null, admissionNumber, "Test", "Learner",
                        LocalDate.now().minusYears(4), "UNSPECIFIED", createdBy.getId())));
    }

    private void requireTestPasswords() {
        if (superAdminPassword.length() < 16 || branchAdminPassword.length() < 16
                || teacherPassword.length() < 16 || parentPassword.length() < 16) {
            throw new IllegalStateException("Each local test account password must be at least 16 characters.");
        }
    }
}