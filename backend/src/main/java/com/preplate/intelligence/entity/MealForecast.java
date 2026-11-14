package com.preplate.intelligence.entity;

import com.preplate.common.enums.ForecastSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "meal_forecasts")
public class MealForecast {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "meal_id", nullable = false, unique = true)
    private Long mealId;
    @Column(name = "predicted_attendance", nullable = false)
    private int predictedAttendance;
    @Column(name = "lower_bound", nullable = false)
    private int lowerBound;
    @Column(name = "upper_bound", nullable = false)
    private int upperBound;
    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal confidence;
    @Column(name = "recommended_preparation", nullable = false)
    private int recommendedPreparation;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ForecastSource source;
    @Column(name = "explanation_json", columnDefinition = "TEXT")
    private String explanationJson;
    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    protected MealForecast() {
    }

    public MealForecast(Long mealId, int predictedAttendance, int lowerBound, int upperBound,
            BigDecimal confidence, int recommendedPreparation, ForecastSource source,
            String explanationJson, Instant generatedAt) {
        this.mealId = mealId;
        this.predictedAttendance = predictedAttendance;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.confidence = confidence;
        this.recommendedPreparation = recommendedPreparation;
        this.source = source;
        this.explanationJson = explanationJson;
        this.generatedAt = generatedAt;
    }

    public Long getId() { return id; }
    public Long getMealId() { return mealId; }
    public int getPredictedAttendance() { return predictedAttendance; }
    public int getLowerBound() { return lowerBound; }
    public int getUpperBound() { return upperBound; }
    public BigDecimal getConfidence() { return confidence; }
    public int getRecommendedPreparation() { return recommendedPreparation; }
    public ForecastSource getSource() { return source; }
    public String getExplanationJson() { return explanationJson; }
    public Instant getGeneratedAt() { return generatedAt; }
}
