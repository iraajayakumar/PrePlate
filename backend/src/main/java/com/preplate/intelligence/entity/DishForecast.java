package com.preplate.intelligence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
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
    @Column(name = "predicted_demand", nullable = false, precision = 10, scale = 2)
    private BigDecimal predictedDemand;
    @Column(name = "recommended_preparation", nullable = false, precision = 10, scale = 2)
    private BigDecimal recommendedPreparation;
    @Column(length = 20)
    private String unit;
    @Column(length = 20)
    private String method;
    @Column(length = 255)
    private String note;
    @Column(name = "interval_lower", precision = 10, scale = 2)
    private BigDecimal intervalLower;
    @Column(name = "interval_upper", precision = 10, scale = 2)
    private BigDecimal intervalUpper;
    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    protected DishForecast() {
    }

    public DishForecast(Long mealId, Long dishId, int studentInterest, int predictedDemand,
            int recommendedPreparation, Instant generatedAt) {
        this(mealId, dishId, studentInterest, BigDecimal.valueOf(predictedDemand),
                BigDecimal.valueOf(recommendedPreparation), null, null, null, null, null, generatedAt);
    }

    public DishForecast(Long mealId, Long dishId, int studentInterest, BigDecimal predictedDemand,
            BigDecimal recommendedPreparation, String unit, String method, String note,
            BigDecimal intervalLower, BigDecimal intervalUpper, Instant generatedAt) {
        this.mealId = mealId;
        this.dishId = dishId;
        this.studentInterest = studentInterest;
        this.predictedDemand = predictedDemand;
        this.recommendedPreparation = recommendedPreparation;
        this.unit = unit;
        this.method = method;
        this.note = note;
        this.intervalLower = intervalLower;
        this.intervalUpper = intervalUpper;
        this.generatedAt = generatedAt;
    }

    public Long getId() { return id; }
    public Long getMealId() { return mealId; }
    public Long getDishId() { return dishId; }
    public int getStudentInterest() { return studentInterest; }
    public BigDecimal getPredictedDemand() { return predictedDemand; }
    public BigDecimal getRecommendedPreparation() { return recommendedPreparation; }
    public String getUnit() { return unit; }
    public String getMethod() { return method; }
    public String getNote() { return note; }
    public BigDecimal getIntervalLower() { return intervalLower; }
    public BigDecimal getIntervalUpper() { return intervalUpper; }
    public Instant getGeneratedAt() { return generatedAt; }
}
