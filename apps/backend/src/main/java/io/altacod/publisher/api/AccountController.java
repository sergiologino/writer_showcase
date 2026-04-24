package io.altacod.publisher.api;

import io.altacod.publisher.api.dto.AddProfilePhotoRequest;
import io.altacod.publisher.api.dto.MeResponse;
import io.altacod.publisher.api.dto.SetAvatarRequest;
import io.altacod.publisher.api.dto.UpdateProfilePayload;
import io.altacod.publisher.web.ActiveWorkspace;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/me")
    public MeResponse me() {
        return accountService.me();
    }

    @PatchMapping("/me")
    public MeResponse updateMe(@Valid @RequestBody UpdateProfilePayload payload) {
        return accountService.updateProfile(payload);
    }

    @PutMapping("/me/avatar")
    public MeResponse setAvatar(
            @ActiveWorkspace Long workspaceId,
            @RequestBody(required = false) SetAvatarRequest request
    ) {
        if (request == null) {
            return accountService.setAvatar(workspaceId, new SetAvatarRequest(null));
        }
        return accountService.setAvatar(workspaceId, request);
    }

    @PostMapping("/me/profile-photos")
    public MeResponse addProfilePhoto(
            @ActiveWorkspace Long workspaceId,
            @Valid @RequestBody AddProfilePhotoRequest request
    ) {
        return accountService.addProfilePhoto(workspaceId, request);
    }

    @DeleteMapping("/me/profile-photos/{mediaAssetId}")
    public MeResponse removeProfilePhoto(
            @ActiveWorkspace Long workspaceId,
            @PathVariable long mediaAssetId
    ) {
        return accountService.removeProfilePhoto(workspaceId, mediaAssetId);
    }
}
