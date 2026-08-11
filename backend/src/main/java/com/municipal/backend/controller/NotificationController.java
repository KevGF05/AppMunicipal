package com.municipal.backend.controller;

import com.municipal.backend.dto.NotificationResponse;
import com.municipal.backend.model.AppUser;
import com.municipal.backend.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> list(
        @RequestParam(defaultValue = "true") boolean soloNoLeidas,
        Authentication authentication
    ) {
        return notificationService.list(user(authentication), soloNoLeidas);
    }

    @PatchMapping("/{id}/leida")
    public NotificationResponse markRead(@PathVariable Long id, Authentication authentication) {
        return notificationService.markRead(id, user(authentication));
    }

    private AppUser user(Authentication authentication) {
        return (AppUser) authentication.getPrincipal();
    }
}
