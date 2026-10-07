package com.littlehome.school.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "app.bootstrap-admin", name = "enabled", havingValue = "true")
public class BootstrapAdminRunner implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(BootstrapAdminRunner.class);
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public BootstrapAdminRunner(UserRepository users, RoleRepository roles, PasswordEncoder passwordEncoder,
                                @Value("${app.bootstrap-admin.email}") String email,
                                @Value("${app.bootstrap-admin.password}") String password) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            logger.warn("Bootstrap admin skipped because user accounts already exist.");
            return;
        }
        if (email.isBlank() || password.length() < 16) {
            throw new IllegalStateException("Bootstrap requires an email and a password of at least 16 characters.");
        }
        RoleEntity superAdmin = roles.findByCode("SUPER_ADMIN")
                .orElseThrow(() -> new IllegalStateException("SUPER_ADMIN role is missing; run database migrations first."));
        users.save(new UserEntity(superAdmin, email.trim().toLowerCase(), passwordEncoder.encode(password), "School", "Administrator"));
        logger.info("Initial super-admin account created. Disable bootstrap credentials before the next restart.");
    }
}