package com.preplate.notification.service;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class NotificationScheduler {
    private final NotificationService notificationService;

    public NotificationScheduler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelay = 30000, initialDelay = 5000)
    public void generateNotifications() {
        notificationService.generateTimeBasedNotificationsForAllStudents();
    }
}
