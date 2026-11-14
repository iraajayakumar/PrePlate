package com.preplate.intelligence.service;

import static com.preplate.common.dto.ApiDtos.*;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.preplate.analytics.entity.ConsumptionRecord;
import com.preplate.analytics.repository.ConsumptionRecordRepository;
import com.preplate.common.enums.AttendanceStatus;
import com.preplate.common.enums.ForecastSource;
import com.preplate.common.enums.MealType;
import com.preplate.common.exception.NotFoundException;
import com.preplate.intelligence.entity.DishForecast;
import com.preplate.intelligence.entity.MealForecast;
import com.preplate.intelligence.repository.DishForecastRepository;
import com.preplate.intelligence.repository.MealForecastRepository;
import com.preplate.meal.entity.Dish;
import com.preplate.meal.entity.Meal;
import com.preplate.meal.entity.MealResponse;
import com.preplate.meal.repository.DishPreferenceRepository;
import com.preplate.meal.repository.MealRepository;
import com.preplate.meal.repository.MealResponseRepository;
import com.preplate.meal.service.MealScheduleService;
import com.preplate.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ForecastService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForecastService.class);
    private final MealRepository meals;
    private final MealResponseRepository responses;
    private final DishPreferenceRepository preferences;
    private final UserRepository users;
    private final MealForecastRepository mealForecasts;
    private final DishForecastRepository dishForecasts;
    private final ConsumptionRecordRepository consumption;
    private final MealScheduleService schedules;
    private final MlForecastClient mlClient;
    private final HeuristicForecaster heuristic = new HeuristicForecaster();
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final boolean mlEnabled;
    private final double safetyBuffer;

    public ForecastService(MealRepository meals, MealResponseRepository responses,
            DishPreferenceRepository preferences, UserRepository users, MealForecastRepository mealForecasts,
            DishForecastRepository dishForecasts, ConsumptionRecordRepository consumption,
            MealScheduleService schedules, MlForecastClient mlClient, ObjectMapper objectMapper, Clock clock,
            @Value("${preplate.forecast.enabled:false}") boolean mlEnabled,
            @Value("${preplate.forecast.safety-buffer:0.05}") double safetyBuffer) {
        this.meals = meals;
        this.responses = responses;
        this.preferences = preferences;
        this.users = users;
        this.mealForecasts = mealForecasts;
        this.dishForecasts = dishForecasts;
        this.consumption = consumption;
        this.schedules = schedules;
        this.mlClient = mlClient;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.mlEnabled = mlEnabled;
        this.safetyBuffer = safetyBuffer;
    }

    @Transactional
    public ForecastResponse generate(Long mealId) {
        Meal meal = meals.findWithDishesById(mealId).orElseThrow(() -> new NotFoundException("Meal not found."));
        ForecastInput input = gather(meal);
        HeuristicForecaster.ForecastValues values;
        Map<Long, Integer> predictedDishDemand = new HashMap<>();
        ForecastSource source = ForecastSource.HEURISTIC;

        if (mlEnabled) {
            try {
                MlForecastClient.MlResponse model = mlClient.predict(mlRequest(meal, input));
                if (model == null || model.dishes() == null || model.factors() == null) {
                    throw new IllegalStateException("Forecast response was incomplete.");
                }
                int predicted = clamp(model.predictedAttendance(), input.confirmed(), input.eligible());
                double confidence = Math.max(0, Math.min(1, model.confidence()));
                int lower = Math.max(input.confirmed(), Math.min(predicted, model.lowerBound()));
                int upper = Math.max(predicted, Math.min(input.eligible(), model.upperBound()));
                values = new HeuristicForecaster.ForecastValues(predicted, lower, upper, confidence,
                        recommend(predicted), model.factors(), 0, 0, predicted);
                model.dishes().forEach(dish -> predictedDishDemand.put(dish.dishId(),
                        clamp(dish.predictedDemand(), 0, predicted)));
                source = ForecastSource.ML_SERVICE;
            } catch (RuntimeException exception) {
                LOGGER.warn("Forecast service failed for meal {}; using heuristic: {}", mealId, exception.getMessage());
                values = heuristic.forecast(input.heuristicInput(), safetyBuffer);
            }
        } else {
            values = heuristic.forecast(input.heuristicInput(), safetyBuffer);
        }

        for (Dish dish : meal.getDishes()) {
            int interest = input.interest().getOrDefault(dish.getId(), 0);
            int demand = predictedDishDemand.getOrDefault(dish.getId(),
                    heuristic.predictDish(values.predicted(), interest, input.pickers(), 0.7));
            demand = Math.max(interest, Math.min(values.predicted(), demand));
            predictedDishDemand.put(dish.getId(), demand);
        }
        return persist(meal, values, predictedDishDemand, input.interest(), source);
    }

    @Transactional
    public DashboardResponse dashboard(LocalDate date, MealType type) {
        Meal meal = meals.findByMealDateAndMealType(date, type).orElse(null);
        if (meal == null) {
            return new DashboardResponse(false, null, date, type, 0, 0, 0, 0, 0, null, List.of());
        }
        return dashboardFor(meal, true);
    }

    @Transactional
    public DashboardResponse refresh(Long mealId) {
        if (!meals.existsById(mealId)) {
            throw new NotFoundException("Meal not found.");
        }
        generate(mealId);
        Meal meal = meals.findWithDishesById(mealId).orElseThrow(() -> new NotFoundException("Meal not found."));
        return dashboardFor(meal, false);
    }

    @Transactional
    public ForecastResponse ensureForecast(Long mealId) {
        MealForecast stored = mealForecasts.findByMealId(mealId).orElse(null);
        return stored == null ? generate(mealId) : response(stored);
    }

    @Transactional(readOnly = true)
    public List<DishForecast> dishForecasts(Long mealId) {
        return dishForecasts.findByMealId(mealId);
    }

    private DashboardResponse dashboardFor(Meal meal, boolean generateIfMissing) {
        if (generateIfMissing && mealForecasts.findByMealId(meal.getId()).isEmpty()) {
            generate(meal.getId());
        }
        long eligible = users.countByRole(com.preplate.common.enums.Role.STUDENT);
        List<MealResponse> mealResponses = responses.findByMealId(meal.getId());
        long confirmed = mealResponses.stream().filter(r -> r.getStatus() == AttendanceStatus.ATTENDING).count();
        long notAttending = mealResponses.stream().filter(r -> r.getStatus() == AttendanceStatus.NOT_ATTENDING).count();
        long uncertain = mealResponses.stream().filter(r -> r.getStatus() == AttendanceStatus.NOT_SURE).count();
        long noResponse = Math.max(0, eligible - mealResponses.size());
        MealForecast forecast = mealForecasts.findByMealId(meal.getId()).orElse(null);
        Map<Long, DishForecast> byDishId = new HashMap<>();
        dishForecasts.findByMealId(meal.getId()).forEach(value -> byDishId.put(value.getDishId(), value));
        List<DishForecastResponse> dishResults = meal.getDishes().stream().sorted(Comparator.comparing(Dish::getName))
                .map(dish -> {
                    DishForecast result = byDishId.get(dish.getId());
                    return result == null ? new DishForecastResponse(dish.getId(), dish.getName(), 0, 0, 0)
                            : new DishForecastResponse(dish.getId(), dish.getName(), result.getStudentInterest(),
                                    result.getPredictedDemand(), result.getRecommendedPreparation());
                }).toList();
        return new DashboardResponse(true, meal.getId(), meal.getMealDate(), meal.getMealType(), eligible,
                confirmed, notAttending, uncertain, noResponse, forecast == null ? null : response(forecast), dishResults);
    }

    private ForecastInput gather(Meal meal) {
        long eligible = users.countByRole(com.preplate.common.enums.Role.STUDENT);
        List<MealResponse> mealResponses = responses.findByMealId(meal.getId());
        int confirmed = count(mealResponses, AttendanceStatus.ATTENDING);
        int notAttending = count(mealResponses, AttendanceStatus.NOT_ATTENDING);
        int uncertain = count(mealResponses, AttendanceStatus.NOT_SURE);
        int noResponse = (int) Math.max(0, eligible - mealResponses.size());
        Set<Long> attendingUsers = new HashSet<>();
        mealResponses.stream().filter(value -> value.getStatus() == AttendanceStatus.ATTENDING)
                .forEach(value -> attendingUsers.add(value.getUserId()));
        List<com.preplate.meal.entity.DishPreference> dishPicks = preferences.findByMealIdIn(List.of(meal.getId()));
        Map<Long, Integer> interest = new HashMap<>();
        Set<Long> pickers = new HashSet<>();
        for (com.preplate.meal.entity.DishPreference preference : dishPicks) {
            interest.merge(preference.getDishId(), 1, Integer::sum);
            if (attendingUsers.contains(preference.getUserId())) {
                pickers.add(preference.getUserId());
            }
        }
        LocalDate today = LocalDate.now(clock);
        List<Meal> historyMeals = meals.findByMealDateBetween(today.minusYears(3), today.minusDays(1)).stream()
                .filter(value -> value.getMealType() == meal.getMealType())
                .sorted(Comparator.comparing(Meal::getMealDate).reversed()).limit(14).toList();
        List<Long> historyMealIds = historyMeals.stream().map(Meal::getId).toList();
        Map<Long, Integer> maxConsumedByMeal = new HashMap<>();
        if (!historyMealIds.isEmpty()) {
            consumption.findByMealIdIn(historyMealIds).forEach(record ->
                    maxConsumedByMeal.merge(record.getMealId(), record.getConsumedQuantity(), Math::max));
        }
        List<Integer> history = historyMeals.stream().map(value -> maxConsumedByMeal.getOrDefault(value.getId(), 0)).toList();
        return new ForecastInput((int) eligible, confirmed, notAttending, uncertain, noResponse,
                history, interest, pickers.size(), meal, historyMeals, maxConsumedByMeal);
    }

    private MlForecastClient.MlRequest mlRequest(Meal meal, ForecastInput input) {
        List<MlForecastClient.MlDishInput> dishInput = meal.getDishes().stream()
                .map(dish -> new MlForecastClient.MlDishInput(dish.getId(), dish.getName(),
                        input.interest().getOrDefault(dish.getId(), 0))).toList();
        List<MlForecastClient.MlHistoryInput> history = input.historyMeals().stream()
                .map(value -> new MlForecastClient.MlHistoryInput(value.getMealDate(),
                        input.maxConsumedByMeal().getOrDefault(value.getId(), 0))).toList();
        return new MlForecastClient.MlRequest(meal.getId(), meal.getMealDate(), meal.getMealType(),
                meal.getMealDate().getDayOfWeek().name(), input.eligible(), input.confirmed(),
                input.notAttending(), input.uncertain(), input.noResponse(), dishInput, history);
    }

    private ForecastResponse persist(Meal meal, HeuristicForecaster.ForecastValues values, Map<Long, Integer> predictedDemand,
            Map<Long, Integer> interest, ForecastSource source) {
        var existing = mealForecasts.findByMealId(meal.getId());
        existing.ifPresent(mealForecasts::delete);
        mealForecasts.flush();
        String factors;
        try {
            factors = objectMapper.writeValueAsString(values.factors());
        } catch (JacksonException exception) {
            throw new IllegalStateException("Unable to serialize forecast factors.", exception);
        }
        MealForecast forecast = mealForecasts.save(new MealForecast(meal.getId(), values.predicted(),
                values.lower(), values.upper(), BigDecimal.valueOf(values.confidence()).setScale(3, RoundingMode.HALF_UP),
                values.recommended(), source, factors, clock.instant()));
        dishForecasts.deleteByMealId(meal.getId());
        dishForecasts.flush();
        List<DishForecast> dishValues = meal.getDishes().stream().map(dish -> {
            int dishInterest = interest.getOrDefault(dish.getId(), 0);
            int demand = predictedDemand.getOrDefault(dish.getId(), 0);
            return new DishForecast(meal.getId(), dish.getId(), dishInterest, demand,
                    heuristic.recommend(demand, safetyBuffer), forecast.getGeneratedAt());
        }).toList();
        dishForecasts.saveAll(dishValues);
        return response(forecast);
    }

    private ForecastResponse response(MealForecast forecast) {
        List<ForecastFactor> factors;
        try {
            factors = objectMapper.readValue(forecast.getExplanationJson(),
                    new TypeReference<List<ForecastFactor>>() { });
        } catch (Exception exception) {
            LOGGER.warn("Could not read forecast factors for meal {}", forecast.getMealId(), exception);
            factors = List.of();
        }
        return new ForecastResponse(forecast.getPredictedAttendance(), forecast.getLowerBound(),
                forecast.getUpperBound(), forecast.getConfidence().doubleValue(),
                forecast.getRecommendedPreparation(), forecast.getSource(), forecast.getGeneratedAt(), factors);
    }

    private int count(List<MealResponse> values, AttendanceStatus status) {
        return (int) values.stream().filter(value -> value.getStatus() == status).count();
    }

    private int recommend(int predicted) {
        return (int) Math.ceil(predicted * (1 + safetyBuffer));
    }

    private int clamp(int value, int lower, int upper) {
        return Math.max(lower, Math.min(upper, value));
    }

    private record ForecastInput(int eligible, int confirmed, int notAttending, int uncertain, int noResponse,
            List<Integer> history, Map<Long, Integer> interest, int pickers, Meal meal, List<Meal> historyMeals,
            Map<Long, Integer> maxConsumedByMeal) {
        private HeuristicForecaster.ForecastInput heuristicInput() {
            return new HeuristicForecaster.ForecastInput(eligible, confirmed, notAttending, uncertain, noResponse,
                    history, interest, pickers);
        }
    }
}
