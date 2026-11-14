package com.preplate.notification.repository;

import com.preplate.notification.entity.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    boolean existsByUserIdAndDedupeKey(Long userId, String dedupeKey);
    List<Notification> findTop30ByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndReadFalse(Long userId);
    List<Notification> findByUserIdAndReadFalse(Long userId);
}
