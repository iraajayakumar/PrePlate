package com.preplate.analytics.service;

import static com.preplate.common.dto.ApiDtos.*;

import com.preplate.analytics.entity.ConsumptionRecord;
import com.preplate.analytics.repository.ConsumptionRecordRepository;
import com.preplate.common.enums.MealType;
import com.preplate.intelligence.entity.DishForecast;
import com.preplate.intelligence.repository.DishForecastRepository;
import com.preplate.meal.entity.Meal;
import com.preplate.meal.repository.MealRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WasteAnalyticsService {
    private final ConsumptionRecordRepository records;
    private final DishForecastRepository forecasts;
    private final MealRepository meals;
    private final Clock clock;

    public WasteAnalyticsService(ConsumptionRecordRepository records, DishForecastRepository forecasts,
            MealRepository meals, Clock clock) {
        this.records = records;
        this.forecasts = forecasts;
        this.meals = meals;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public WasteAnalyticsResponse analyze(int days) {
        if (days < 1 || days > 90) {
            throw new IllegalArgumentException("Days must be between 1 and 90.");
        }
        LocalDate today = LocalDate.now(clock);
        LocalDate from = today.minusDays(days - 1L);
        List<ConsumptionRecord> rows = records.findBetweenMealDates(from, today);
        if (rows.isEmpty()) {
            return new WasteAnalyticsResponse(new WasteTotals(0, 0, 0, 0, 0),
                    List.of(), List.of(), List.of());
        }
        List<Long> mealIds = rows.stream().map(ConsumptionRecord::getMealId).distinct().toList();
        Map<Long, Meal> mealById = new HashMap<>();
        meals.findAllById(mealIds).forEach(meal -> mealById.put(meal.getId(), meal));
        Map<String, DishForecast> forecastByKey = new HashMap<>();
        forecasts.findByMealIdIn(mealIds).forEach(forecast ->
                forecastByKey.put(key(forecast.getMealId(), forecast.getDishId()), forecast));

        double prepared = rows.stream().mapToDouble(ConsumptionRecord::getPreparedQuantity).sum();
        double consumed = rows.stream().mapToDouble(ConsumptionRecord::getConsumedQuantity).sum();
        double leftover = rows.stream().mapToDouble(ConsumptionRecord::getLeftoverQuantity).sum();
        List<WasteMetrics.ForecastComparison> comparisons = new ArrayList<>();
        for (ConsumptionRecord row : rows) {
            DishForecast forecast = forecastByKey.get(key(row.getMealId(), row.getDishId()));
            if (forecast != null) {
                comparisons.add(new WasteMetrics.ForecastComparison(forecast.getPredictedDemand().doubleValue(),
                        row.getConsumedQuantity()));
            }
        }
        WasteTotals totals = new WasteTotals(prepared, consumed, leftover, percentage(leftover, prepared),
                WasteMetrics.forecastAccuracy(comparisons));

        Map<LocalDate, double[]> daily = new HashMap<>();
        Map<MealType, double[]> mealTypeTotals = new HashMap<>();
        Map<String, double[]> dishTotals = new HashMap<>();
        Map<Long, String> dishNames = new HashMap<>();
        meals.findByMealDateBetween(from, today).forEach(meal ->
                meal.getDishes().forEach(dish -> dishNames.put(dish.getId(), dish.getName())));
        for (ConsumptionRecord row : rows) {
            Meal meal = mealById.get(row.getMealId());
            if (meal == null) {
                continue;
            }
            add(daily.computeIfAbsent(meal.getMealDate(), ignored -> new double[3]), row);
            add(mealTypeTotals.computeIfAbsent(meal.getMealType(), ignored -> new double[3]), row);
            String name = dishNames.getOrDefault(row.getDishId(), "Dish " + row.getDishId());
            add(dishTotals.computeIfAbsent(name, ignored -> new double[3]), row);
        }
        List<DailyWaste> dailyTrend = daily.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> new DailyWaste(entry.getKey(), entry.getValue()[0], entry.getValue()[1],
                        entry.getValue()[2], percentage(entry.getValue()[2], entry.getValue()[0]))).toList();
        List<MealTypeWaste> byMealType = List.of(MealType.values()).stream()
                .map(type -> {
                    double[] values = mealTypeTotals.getOrDefault(type, new double[3]);
                    return new MealTypeWaste(type, percentage(values[2], values[0]));
                }).toList();
        List<DishWaste> byDish = dishTotals.entrySet().stream()
                .map(entry -> new DishWaste(entry.getKey(), percentage(entry.getValue()[2], entry.getValue()[0])))
                .sorted(Comparator.comparingDouble(DishWaste::wastePercentage).reversed()
                        .thenComparing(DishWaste::dishName))
                .limit(8).toList();
        return new WasteAnalyticsResponse(totals, dailyTrend, byMealType, byDish);
    }

    public static double percentage(double leftover, double prepared) {
        return WasteMetrics.wastePercentage(prepared, leftover);
    }

    private static void add(double[] totals, ConsumptionRecord row) {
        totals[0] += row.getPreparedQuantity();
        totals[1] += row.getConsumedQuantity();
        totals[2] += row.getLeftoverQuantity();
    }

    private static String key(Long mealId, Long dishId) {
        return mealId + ":" + dishId;
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
