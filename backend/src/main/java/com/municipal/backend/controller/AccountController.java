package com.municipal.backend.controller;

import com.municipal.backend.dto.MessageResponse;
import com.municipal.backend.dto.PasswordChangeRequest;
import com.municipal.backend.model.AppUser;
import com.municipal.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
public class AccountController {
    private final AuthService authService;

    public AccountController(AuthService authService) {
        this.authService = authService;
    }

    @PatchMapping("/password")
    public MessageResponse changePassword(
        @Valid @RequestBody PasswordChangeRequest request,
        Authentication authentication
    ) {
        authService.changePassword((AppUser) authentication.getPrincipal(), request);
        return new MessageResponse("Contraseña actualizada correctamente.");
    }
}
