package com.lose10.profile;

import com.lose10.auth.AccountService;
import com.lose10.platform.security.CurrentUser;
import com.lose10.profile.dto.ConsentRequest;
import com.lose10.profile.dto.DataExportResponse;
import com.lose10.profile.dto.ProfileResponse;
import com.lose10.profile.dto.UpdatePreferencesRequest;
import com.lose10.profile.dto.UpdateProfileRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class ProfileController {

    private final ProfileService profileService;
    private final AccountService accountService;
    private final DataExportService dataExportService;

    public ProfileController(ProfileService profileService, AccountService accountService, DataExportService dataExportService) {
        this.profileService = profileService;
        this.accountService = accountService;
        this.dataExportService = dataExportService;
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getMe() {
        return ResponseEntity.ok(profileService.getProfile(CurrentUser.id()));
    }

    @PatchMapping("/me")
    public ResponseEntity<ProfileResponse> updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(profileService.updateProfile(CurrentUser.id(), request));
    }

    @PatchMapping("/me/preferences")
    public ResponseEntity<ProfileResponse> updatePreferences(@Valid @RequestBody UpdatePreferencesRequest request) {
        return ResponseEntity.ok(profileService.updatePreferences(CurrentUser.id(), request));
    }

    @PostMapping("/me/consents")
    public ResponseEntity<Void> recordConsent(@Valid @RequestBody ConsentRequest request) {
        profileService.recordConsent(CurrentUser.id(), request);
        return ResponseEntity.status(201).build();
    }

    @DeleteMapping("/me/consents/{type}")
    public ResponseEntity<Void> revokeConsent(@PathVariable String type) {
        profileService.revokeConsent(CurrentUser.id(), type);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/export")
    public ResponseEntity<DataExportResponse> exportMyData() {
        return ResponseEntity.ok(dataExportService.export(CurrentUser.id()));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount() {
        accountService.deleteAccount(CurrentUser.id());
        return ResponseEntity.noContent().build();
    }
}
