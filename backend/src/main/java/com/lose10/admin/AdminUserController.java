package com.lose10.admin;

import com.lose10.admin.dto.AddAdminNoteRequest;
import com.lose10.admin.dto.AdminUserDetailResponse;
import com.lose10.admin.dto.CoachingStateResponse;
import com.lose10.admin.dto.SetCoachingStateRequest;
import com.lose10.auth.UserDirectory;
import com.lose10.auth.dto.AdminUserListItem;
import com.lose10.platform.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final UserDirectory userDirectory;
    private final AdminUserDetailService detailService;
    private final AdminNoteService noteService;
    private final AdminCoachingOverrideService coachingOverrideService;

    public AdminUserController(
            UserDirectory userDirectory,
            AdminUserDetailService detailService,
            AdminNoteService noteService,
            AdminCoachingOverrideService coachingOverrideService
    ) {
        this.userDirectory = userDirectory;
        this.detailService = detailService;
        this.noteService = noteService;
        this.coachingOverrideService = coachingOverrideService;
    }

    @GetMapping
    public Page<AdminUserListItem> search(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return userDirectory.search(q, PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    public AdminUserDetailResponse detail(@PathVariable UUID id) {
        return detailService.getDetail(id, CurrentUser.id());
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<Void> addNote(@PathVariable UUID id, @Valid @RequestBody AddAdminNoteRequest request) {
        noteService.addNote(id, CurrentUser.id(), request.note());
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/{id}/coaching-state")
    public ResponseEntity<CoachingStateResponse> setCoachingState(@PathVariable UUID id, @Valid @RequestBody SetCoachingStateRequest request) {
        return ResponseEntity.ok(CoachingStateResponse.from(coachingOverrideService.setMode(id, CurrentUser.id(), request.mode(), request.reason())));
    }
}
