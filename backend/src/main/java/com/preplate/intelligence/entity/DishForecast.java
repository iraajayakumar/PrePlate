package com.preplate.intelligence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "dish_forecasts", uniqueConstraints = @UniqueConstraint(
        name = "uk_dish_forecasts_meal_dish", columnNames = {"meal_id", "dish_id"}))
public class DishForecast {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "meal_id", nullable = false)
    private Long mealId;
    @Column(name = "dish_id", nullable = false)
    private Long dishId;
    @Column(name = "student_interest", nullable = false)
    private int studentInterest;
    @Column(name = "predicted_demand", nullable = false)
    private int predictedDemand;
    @Column(name = "recommended_preparation", nullable = false)
    private int recommendedPreparation;
    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    protected DishForecast() {
    }

    public DishForecast(Long mealId, Long dishId, int studentInterest, int predictedDemand,
            int recommendedPreparation, Instant generatedAt) {
        this.mealId = mealId;
        this.dishId = dishId;
        this.studentInterest = studentInterest;
        this.predictedDemand = predictedDemand;
        this.recommendedPreparation = recommendedPreparation;
        this.generatedAt = generatedAt;
    }

    public Long getId() { return id; }
    public Long getMealId() { return mealId; }
    public Long getDishId() { return dishId; }
    public int getStudentInterest() { return studentInterest; }
    public int getPredictedDemand() { return predictedDemand; }
    public int getRecommendedPreparation() { return recommendedPreparation; }
    public Instant getGeneratedAt() { return generatedAt; }
}
