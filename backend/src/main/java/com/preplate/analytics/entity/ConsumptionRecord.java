package com.preplate.analytics.entity;

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
@Table(name = "consumption_records", uniqueConstraints = @UniqueConstraint(
        name = "uk_consumption_records_meal_dish", columnNames = {"meal_id", "dish_id"}))
public class ConsumptionRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "meal_id", nullable = false)
    private Long mealId;
    @Column(name = "dish_id", nullable = false)
    private Long dishId;
    @Column(name = "recommended_quantity", precision = 10, scale = 2)
    private BigDecimal recommendedQuantity;
    @Column(name = "prepared_quantity", nullable = false)
    private int preparedQuantity;
    @Column(name = "consumed_quantity", nullable = false)
    private int consumedQuantity;
    @Column(name = "leftover_quantity", nullable = false)
    private int leftoverQuantity;
    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected ConsumptionRecord() {
    }

    public ConsumptionRecord(Long mealId, Long dishId, BigDecimal recommendedQuantity, int preparedQuantity,
            int consumedQuantity, Instant recordedAt) {
        this.mealId = mealId;
        this.dishId = dishId;
        this.recommendedQuantity = recommendedQuantity;
        this.preparedQuantity = preparedQuantity;
        this.consumedQuantity = consumedQuantity;
        this.leftoverQuantity = preparedQuantity - consumedQuantity;
        this.recordedAt = recordedAt;
    }

    public void update(BigDecimal recommendedQuantity, int preparedQuantity, int consumedQuantity, Instant recordedAt) {
        this.recommendedQuantity = recommendedQuantity;
        this.preparedQuantity = preparedQuantity;
        this.consumedQuantity = consumedQuantity;
        this.leftoverQuantity = preparedQuantity - consumedQuantity;
        this.recordedAt = recordedAt;
    }

    public Long getId() { return id; }
    public Long getMealId() { return mealId; }
    public Long getDishId() { return dishId; }
    public BigDecimal getRecommendedQuantity() { return recommendedQuantity; }
    public int getPreparedQuantity() { return preparedQuantity; }
    public int getConsumedQuantity() { return consumedQuantity; }
    public int getLeftoverQuantity() { return leftoverQuantity; }
    public Instant getRecordedAt() { return recordedAt; }
}
