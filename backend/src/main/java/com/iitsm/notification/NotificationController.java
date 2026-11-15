package com.iitsm.notification;

import com.iitsm.common.ApiResponse;
import com.iitsm.notification.dto.NotificationResponse;
import com.iitsm.notification.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service=service; }
    @GetMapping public ApiResponse<List<NotificationResponse>> list(Authentication auth) { return ApiResponse.ok(service.list(auth.getName())); }
    @PatchMapping("/{id}/read") public ApiResponse<NotificationResponse> read(@PathVariable Long id, Authentication auth) { return ApiResponse.ok("Notification marked read",service.markRead(auth.getName(),id)); }
}
