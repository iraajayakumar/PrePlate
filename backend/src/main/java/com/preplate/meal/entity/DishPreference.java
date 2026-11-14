package com.preplate.meal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "dish_preferences", uniqueConstraints = @UniqueConstraint(
        name = "uk_dish_preferences_user_meal_dish", columnNames = {"user_id", "meal_id", "dish_id"}))
public class DishPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "meal_id", nullable = false)
    private Long mealId;
    @Column(name = "dish_id", nullable = false)
    private Long dishId;

    protected DishPreference() {
    }

    public DishPreference(Long userId, Long mealId, Long dishId) {
        this.userId = userId;
        this.mealId = mealId;
        this.dishId = dishId;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getMealId() { return mealId; }
    public Long getDishId() { return dishId; }
}
