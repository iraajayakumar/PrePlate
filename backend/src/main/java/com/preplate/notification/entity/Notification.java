package com.preplate.notification.entity;

import com.preplate.common.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;
    @Column(nullable = false, length = 100)
    private String title;
    @Column(nullable = false, length = 255)
    private String message;
    @Column(name = "dedupe_key", length = 100)
    private String dedupeKey;
    @Column(name = "is_read", nullable = false)
    private boolean read;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Notification() {
    }

    public Notification(Long userId, NotificationType type, String title, String message,
            String dedupeKey, Instant createdAt) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.dedupeKey = dedupeKey;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getDedupeKey() { return dedupeKey; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public Instant getCreatedAt() { return createdAt; }
}
