package com.preplate.notification.controller;

import static com.preplate.common.dto.ApiDtos.*;

import com.preplate.auth.AuthInterceptor;
import com.preplate.notification.service.NotificationService;
import com.preplate.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public NotificationsResponse list(HttpServletRequest request) {
        return notifications.list(currentUser(request).getId());
    }

    @PostMapping("/read-all")
    public UpdatedResponse markAllRead(HttpServletRequest request) {
        return notifications.markAllRead(currentUser(request).getId());
    }

    private User currentUser(HttpServletRequest request) {
        return (User) request.getAttribute(AuthInterceptor.USER_ATTRIBUTE);
    }
}
