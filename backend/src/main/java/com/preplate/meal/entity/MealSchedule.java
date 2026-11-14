package com.preplate.meal.entity;

import com.preplate.common.enums.MealType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;

@Entity
@Table(name = "meal_schedules")
public class MealSchedule {
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", length = 20)
    private MealType mealType;
    @Column(name = "serving_start", nullable = false)
    private LocalTime servingStart;
    @Column(name = "serving_end", nullable = false)
    private LocalTime servingEnd;
    @Column(name = "deadline_time", nullable = false)
    private LocalTime deadlineTime;
    @Column(name = "deadline_day_offset", nullable = false)
    private int deadlineDayOffset;

    protected MealSchedule() {
    }

    public MealSchedule(MealType mealType, LocalTime servingStart, LocalTime servingEnd,
            LocalTime deadlineTime, int deadlineDayOffset) {
        this.mealType = mealType;
        this.servingStart = servingStart;
        this.servingEnd = servingEnd;
        this.deadlineTime = deadlineTime;
        this.deadlineDayOffset = deadlineDayOffset;
    }

    public MealType getMealType() { return mealType; }
    public LocalTime getServingStart() { return servingStart; }
    public LocalTime getServingEnd() { return servingEnd; }
    public LocalTime getDeadlineTime() { return deadlineTime; }
    public int getDeadlineDayOffset() { return deadlineDayOffset; }
}
