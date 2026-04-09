package io.altacod.publisher.api;

import io.altacod.publisher.api.dto.MeResponse;
import io.altacod.publisher.api.dto.UpdateProfilePayload;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
}
