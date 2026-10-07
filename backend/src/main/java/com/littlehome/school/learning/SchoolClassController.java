package com.littlehome.school.learning;

import com.littlehome.school.identity.UserEntity;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/classes")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_ADMIN', 'TEACHER')")
public class SchoolClassController {
    private final SchoolClassRepository classes;

    public SchoolClassController(SchoolClassRepository classes) { this.classes = classes; }

    @GetMapping
    @Transactional(readOnly = true)
    public List<SchoolClassEntity> list(@AuthenticationPrincipal UserEntity actor) {
        return classes.findAllByOrderByDisplayOrderAsc();
    }
}