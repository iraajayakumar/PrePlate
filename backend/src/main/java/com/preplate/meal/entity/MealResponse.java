package com.preplate.meal.entity;

import com.preplate.common.enums.AttendanceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "meal_responses", uniqueConstraints = @UniqueConstraint(name = "uk_meal_responses_user_meal",
        columnNames = {"user_id", "meal_id"}))
public class MealResponse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "meal_id", nullable = false)
    private Long mealId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status;
    private Boolean attended;
    @Column(name = "responded_at", nullable = false)
    private Instant respondedAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MealResponse() {
    }

    public MealResponse(Long userId, Long mealId, AttendanceStatus status, Instant respondedAt) {
        this.userId = userId;
        this.mealId = mealId;
        this.status = status;
        this.respondedAt = respondedAt;
        this.updatedAt = respondedAt;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getMealId() { return mealId; }
    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus status) { this.status = status; }
    public Boolean getAttended() { return attended; }
    public void setAttended(Boolean attended) { this.attended = attended; }
    public Instant getRespondedAt() { return respondedAt; }
    public void setRespondedAt(Instant respondedAt) { this.respondedAt = respondedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
