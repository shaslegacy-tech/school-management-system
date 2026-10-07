package com.littlehome.school.communication;

import static com.littlehome.school.communication.AnnouncementDtos.AnnouncementResponse;
import static com.littlehome.school.communication.AnnouncementDtos.CreateRequest;

import com.littlehome.school.identity.UserEntity;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/announcements")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_ADMIN')")
public class AnnouncementController {
    private final AnnouncementRepository announcements;

    public AnnouncementController(AnnouncementRepository announcements) { this.announcements = announcements; }

    @GetMapping
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> list(@AuthenticationPrincipal UserEntity actor,
                                          @RequestParam(required = false) UUID branchId) {
        UUID scope = branchScope(actor, branchId);
        return (scope == null ? announcements.findAllByOrderByPublishedAtDesc() : announcements.findByBranchIdOrderByPublishedAtDesc(scope))
                .stream().map(AnnouncementResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<AnnouncementResponse> create(@AuthenticationPrincipal UserEntity actor,
                                                       @Valid @RequestBody CreateRequest request) {
        UUID scope = branchScope(actor, request.branchId());
        if (!"SUPER_ADMIN".equals(actor.getRoleCode()) && scope == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A branch is required.");
        }
        var announcement = announcements.save(new AnnouncementEntity(scope, actor.getId(), request.title().trim(),
                request.body().trim(), request.audience() == null ? "ALL" : request.audience()));
        return ResponseEntity.created(URI.create("/api/v1/announcements/" + announcement.getId())).body(AnnouncementResponse.from(announcement));
    }

    private UUID branchScope(UserEntity actor, UUID requestedBranchId) {
        if ("SUPER_ADMIN".equals(actor.getRoleCode())) return requestedBranchId;
        UUID ownBranch = actor.getBranchId();
        if (ownBranch == null || (requestedBranchId != null && !ownBranch.equals(requestedBranchId))) {
            throw new AccessDeniedException("Branch is outside the user's scope.");
        }
        return ownBranch;
    }
}