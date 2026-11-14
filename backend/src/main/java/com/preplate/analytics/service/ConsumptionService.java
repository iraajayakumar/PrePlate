package com.preplate.analytics.service;

import static com.preplate.common.dto.ApiDtos.*;

import com.preplate.analytics.entity.ConsumptionRecord;
import com.preplate.analytics.repository.ConsumptionRecordRepository;
import com.preplate.common.exception.NotFoundException;
import com.preplate.intelligence.entity.DishForecast;
import com.preplate.intelligence.repository.DishForecastRepository;
import com.preplate.meal.entity.Dish;
import com.preplate.meal.entity.Meal;
import com.preplate.meal.repository.MealRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsumptionService {
    private final MealRepository meals;
    private final ConsumptionRecordRepository records;
    private final DishForecastRepository forecasts;
    private final Clock clock;

    public ConsumptionService(MealRepository meals, ConsumptionRecordRepository records,
            DishForecastRepository forecasts, Clock clock) {
        this.meals = meals;
        this.records = records;
        this.forecasts = forecasts;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ConsumptionResponse get(Long mealId) {
        Meal meal = meal(mealId);
        Map<Long, ConsumptionRecord> recordByDish = new HashMap<>();
        records.findByMealId(mealId).forEach(record -> recordByDish.put(record.getDishId(), record));
        Map<Long, DishForecast> forecastByDish = new HashMap<>();
        forecasts.findByMealId(mealId).forEach(forecast -> forecastByDish.put(forecast.getDishId(), forecast));
        return response(meal, recordByDish, forecastByDish);
    }

    @Transactional
    public ConsumptionResponse save(SaveConsumptionRequest request) {
        Meal meal = meal(request.mealId());
        Map<Long, Dish> dishById = new HashMap<>();
        meal.getDishes().forEach(dish -> dishById.put(dish.getId(), dish));
        Set<Long> submitted = new HashSet<>();
        for (ConsumptionInput input : request.records()) {
            if (input.consumedQuantity() > input.preparedQuantity()) {
                throw new IllegalArgumentException("Consumed quantity cannot exceed prepared quantity.");
            }
            if (!dishById.containsKey(input.dishId())) {
                throw new IllegalArgumentException("Every dish must belong to the selected meal.");
            }
            if (!submitted.add(input.dishId())) {
                throw new IllegalArgumentException("Dish ids must be unique.");
            }
        }
        Map<Long, DishForecast> forecastByDish = new HashMap<>();
        forecasts.findByMealId(meal.getId()).forEach(forecast -> forecastByDish.put(forecast.getDishId(), forecast));
        List<ConsumptionRecord> saved = new ArrayList<>();
        for (ConsumptionInput input : request.records()) {
            DishForecast forecast = forecastByDish.get(input.dishId());
            Integer recommended = forecast == null ? null : forecast.getRecommendedPreparation();
            ConsumptionRecord record = records.findByMealIdAndDishId(meal.getId(), input.dishId())
                    .orElseGet(() -> new ConsumptionRecord(meal.getId(), input.dishId(), recommended,
                            input.preparedQuantity(), input.consumedQuantity(), clock.instant()));
            record.update(recommended, input.preparedQuantity(), input.consumedQuantity(), clock.instant());
            saved.add(record);
        }
        records.saveAll(saved);
        records.flush();
        return get(meal.getId());
    }

    private ConsumptionResponse response(Meal meal, Map<Long, ConsumptionRecord> recordByDish,
            Map<Long, DishForecast> forecastByDish) {
        List<ConsumptionRecordResponse> values = meal.getDishes().stream().map(dish -> {
            ConsumptionRecord record = recordByDish.get(dish.getId());
            DishForecast forecast = forecastByDish.get(dish.getId());
            return new ConsumptionRecordResponse(dish.getId(), dish.getName(),
                    forecast == null ? null : forecast.getRecommendedPreparation(),
                    record == null ? null : record.getPreparedQuantity(),
                    record == null ? null : record.getConsumedQuantity());
        }).toList();
        return new ConsumptionResponse(meal.getId(), values);
    }

    private Meal meal(Long mealId) {
        return meals.findWithDishesById(mealId).orElseThrow(() -> new NotFoundException("Meal not found."));
    }
}
