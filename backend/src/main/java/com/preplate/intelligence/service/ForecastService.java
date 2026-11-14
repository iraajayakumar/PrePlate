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
import com.preplate.common.enums.Role;
import com.preplate.common.exception.NotFoundException;
import com.preplate.intelligence.entity.DishForecast;
import com.preplate.intelligence.entity.MealForecast;
import com.preplate.intelligence.repository.DishForecastRepository;
import com.preplate.intelligence.repository.MealForecastRepository;
import com.preplate.meal.entity.Dish;
import com.preplate.meal.entity.DishPreference;
import com.preplate.meal.entity.Meal;
import com.preplate.meal.entity.MealResponse;
import com.preplate.meal.repository.DishPreferenceRepository;
import com.preplate.meal.repository.MealRepository;
import com.preplate.meal.repository.MealResponseRepository;
import com.preplate.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ForecastService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForecastService.class);
    private static final String NO_DISH_HISTORY_NOTE =
            "No dish consumption history. Zero is a placeholder, not estimated zero demand.";
    private final MealRepository meals;
    private final MealResponseRepository responses;
    private final DishPreferenceRepository preferences;
    private final UserRepository users;
    private final MealForecastRepository mealForecasts;
    private final DishForecastRepository dishForecasts;
    private final ConsumptionRecordRepository consumption;
    private final MlForecastClient mlClient;
    private final HeuristicForecaster heuristic = new HeuristicForecaster();
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final boolean mlEnabled;
    private final double safetyBuffer;
    private final String itemUnit;

    public ForecastService(MealRepository meals, MealResponseRepository responses,
            DishPreferenceRepository preferences, UserRepository users, MealForecastRepository mealForecasts,
            DishForecastRepository dishForecasts, ConsumptionRecordRepository consumption,
            MlForecastClient mlClient, ObjectMapper objectMapper, Clock clock,
            @Value("${preplate.forecast.enabled:true}") boolean mlEnabled,
            @Value("${preplate.forecast.safety-buffer:0.05}") double safetyBuffer,
            @Value("${preplate.forecast.item-unit:servings}") String itemUnit) {
        this.meals = meals;
        this.responses = responses;
        this.preferences = preferences;
        this.users = users;
        this.mealForecasts = mealForecasts;
        this.dishForecasts = dishForecasts;
        this.consumption = consumption;
        this.mlClient = mlClient;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.mlEnabled = mlEnabled;
        this.safetyBuffer = safetyBuffer;
        this.itemUnit = itemUnit;
    }

    @Transactional
    public ForecastResponse generate(Long mealId) {
        Meal meal = meals.findWithDishesById(mealId).orElseThrow(() -> new NotFoundException("Meal not found."));
        ForecastInput input = gather(meal);
        MlForecastClient.MlResponse mlResponse = null;
        String fallbackReason = "ml_disabled";
        if (mlEnabled) {
            fallbackReason = preflightFailure(input.eligible(), input.responseCount(), input.history().size());
            if (fallbackReason == null) {
                MlForecastClient.ForecastAttempt attempt = mlClient.forecast(toMlRequest(meal, input));
                if (attempt.response().isPresent()) {
                    mlResponse = attempt.response().get();
                } else {
                    fallbackReason = attempt.failureReason();
                }
            }
        }

        ForecastValues values = mlResponse == null
                ? heuristicValues(meal, input, fallbackReason)
                : mlValues(mlResponse, input);
        return persist(meal, values, input.interest());
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

    MlForecastClient.MlRequest toMlRequest(Meal meal, ForecastInput input) {
        int responded = input.yes() + input.no() + input.unsure();
        int noResponse = noResponse(input.eligible(), input.yes(), input.no(), input.unsure());
        if (responded > input.eligible()) {
            LOGGER.warn("Meal {} has {} responses for {} eligible students; no_response is clamped to zero.",
                    meal.getId(), responded, input.eligible());
        }
        List<MlForecastClient.DishInput> dishInputs = meal.getDishes().stream()
                .sorted(Comparator.comparing(Dish::getId)).map(dish ->
                new MlForecastClient.DishInput(dish.getId(),
                        cappedInterest(input.eligible(), input.interest().getOrDefault(dish.getId(), 0)),
                        input.popularity().getOrDefault(dish.getId(), 0.0),
                        input.avgConsumption().getOrDefault(dish.getId(), BigDecimal.ZERO).doubleValue(),
                        itemUnit)).toList();
        return new MlForecastClient.MlRequest(meal.getId(), mealTypeName(meal.getMealType()),
                weekdayIndex(meal.getMealDate()), input.eligible(),
                new MlForecastClient.Responses(input.yes(), input.no(), input.unsure(), noResponse),
                new MlForecastClient.Historical(input.avgAttendance(), input.avgWastePercentage()), dishInputs);
    }

    private DashboardResponse dashboardFor(Meal meal, boolean generateIfMissing) {
        if (generateIfMissing && mealForecasts.findByMealId(meal.getId()).isEmpty()) {
            generate(meal.getId());
        }
        long eligible = users.countByRole(Role.STUDENT);
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
                    return result == null
                            ? new DishForecastResponse(dish.getId(), dish.getName(), 0, BigDecimal.ZERO,
                                    BigDecimal.ZERO, itemUnit, null, null, null, null)
                            : new DishForecastResponse(dish.getId(), dish.getName(), result.getStudentInterest(),
                                    result.getPredictedDemand(), result.getRecommendedPreparation(), result.getUnit(),
                                    result.getMethod(), result.getNote(), result.getIntervalLower(),
                                    result.getIntervalUpper());
                }).toList();
        return new DashboardResponse(true, meal.getId(), meal.getMealDate(), meal.getMealType(), eligible,
                confirmed, notAttending, uncertain, noResponse, forecast == null ? null : response(forecast), dishResults);
    }

    private ForecastInput gather(Meal meal) {
        int eligible = Math.toIntExact(users.countByRole(Role.STUDENT));
        List<MealResponse> currentResponses = responses.findByMealId(meal.getId());
        int yes = count(currentResponses, AttendanceStatus.ATTENDING);
        int no = count(currentResponses, AttendanceStatus.NOT_ATTENDING);
        int unsure = count(currentResponses, AttendanceStatus.NOT_SURE);
        int responseCount = yes + no + unsure;
        if (responseCount > eligible) {
            LOGGER.warn("Meal {} has {} responses for {} eligible students.", meal.getId(), responseCount, eligible);
        }

        LocalDate previousDay = meal.getMealDate().minusDays(1);
        List<Meal> previousMeals = previousDay.isBefore(LocalDate.of(1900, 1, 1))
                ? List.of() : meals.findByMealDateBetween(LocalDate.of(1900, 1, 1), previousDay);
        List<Meal> previousSameType = previousMeals.stream()
                .filter(value -> value.getMealType() == meal.getMealType())
                .sorted(Comparator.comparing(Meal::getMealDate).reversed()).toList();
        List<Long> sameTypeIds = previousSameType.stream().map(Meal::getId).toList();
        Map<Long, List<ConsumptionRecord>> recordsByMeal = new HashMap<>();
        if (!sameTypeIds.isEmpty()) {
            consumption.findByMealIdIn(sameTypeIds).forEach(record ->
                    recordsByMeal.computeIfAbsent(record.getMealId(), ignored -> new ArrayList<>()).add(record));
        }
        List<Meal> completedHistory = previousSameType.stream()
                .filter(value -> recordsByMeal.containsKey(value.getId())).limit(14).toList();
        List<Long> completedIds = completedHistory.stream().map(Meal::getId).toList();
        Map<Long, Integer> attendanceByMeal = new HashMap<>();
        if (!completedIds.isEmpty()) {
            responses.findByMealIdIn(completedIds).stream()
                    .filter(value -> value.getStatus() == AttendanceStatus.ATTENDING)
                    .forEach(value -> attendanceByMeal.merge(value.getMealId(), 1, Integer::sum));
        }
        List<Integer> history = completedHistory.stream()
                .map(value -> attendanceByMeal.getOrDefault(value.getId(), 0)).toList();
        double avgAttendance = history.stream().mapToInt(Integer::intValue).average().orElse(0);
        double avgWaste = completedHistory.stream().mapToDouble(value -> {
            List<ConsumptionRecord> rows = recordsByMeal.get(value.getId());
            double prepared = rows.stream().mapToDouble(ConsumptionRecord::getPreparedQuantity).sum();
            double leftover = rows.stream().mapToDouble(ConsumptionRecord::getLeftoverQuantity).sum();
            return Math.max(0, Math.min(100, prepared == 0 ? 0 : leftover / prepared * 100));
        }).average().orElse(0);

        List<DishPreference> previousPreferences = sameTypeIds.isEmpty()
                ? List.of() : preferences.findByMealIdIn(sameTypeIds);
        Map<Long, Integer> attendingByMeal = new HashMap<>();
        if (!sameTypeIds.isEmpty()) {
            responses.findByMealIdIn(sameTypeIds).stream()
                    .filter(value -> value.getStatus() == AttendanceStatus.ATTENDING)
                    .forEach(value -> attendingByMeal.merge(value.getMealId(), 1, Integer::sum));
        }
        Map<String, Integer> picksByMealDish = new HashMap<>();
        previousPreferences.forEach(value -> picksByMealDish.merge(
                value.getMealId() + ":" + value.getDishId(), 1, Integer::sum));

        List<DishPreference> currentPreferences = preferences.findByMealIdIn(List.of(meal.getId()));
        Map<Long, Integer> interest = new HashMap<>();
        currentPreferences.forEach(value -> interest.merge(value.getDishId(), 1, Integer::sum));
        Set<Long> attendingUsers = currentResponses.stream()
                .filter(value -> value.getStatus() == AttendanceStatus.ATTENDING)
                .map(MealResponse::getUserId).collect(java.util.stream.Collectors.toSet());
        int pickers = (int) currentPreferences.stream().filter(value -> attendingUsers.contains(value.getUserId()))
                .map(DishPreference::getUserId).distinct().count();
        Map<Long, Double> popularity = new HashMap<>();
        Map<Long, BigDecimal> avgConsumption = new HashMap<>();
        Set<Long> dishesWithoutConsumptionHistory = new HashSet<>();
        for (Dish dish : meal.getDishes()) {
            List<Double> priorRatios = previousSameType.stream()
                    .filter(value -> value.getDishes().stream().anyMatch(oldDish -> oldDish.getId().equals(dish.getId())))
                    .map(value -> {
                        int attending = attendingByMeal.getOrDefault(value.getId(), 0);
                        int picks = picksByMealDish.getOrDefault(value.getId() + ":" + dish.getId(), 0);
                        return attending == 0 ? 0.0 : Math.min(1.0, picks / (double) attending);
                    }).toList();
            popularity.put(dish.getId(), priorRatios.stream().mapToDouble(Double::doubleValue).average().orElse(0));
            List<ConsumptionRecord> quantities = consumption.findRecentByDishIdBefore(
                    dish.getId(), meal.getMealDate(), PageRequest.of(0, 10));
            avgConsumption.put(dish.getId(), averageConsumption(quantities));
            if (quantities.isEmpty()) {
                dishesWithoutConsumptionHistory.add(dish.getId());
            }
        }
        return new ForecastInput(eligible, yes, no, unsure, responseCount, history, avgAttendance,
                avgWaste, interest, popularity, avgConsumption, dishesWithoutConsumptionHistory, pickers);
    }

    private ForecastValues heuristicValues(Meal meal, ForecastInput input, String fallbackReason) {
        HeuristicForecaster.ForecastValues forecast = heuristic.forecast(input.heuristicInput(), safetyBuffer);
        List<DishValue> dishes = meal.getDishes().stream().map(dish -> {
            int interest = input.interest().getOrDefault(dish.getId(), 0);
            int demand = heuristic.predictDish(forecast.predicted(), interest, input.pickers(),
                    input.popularity().getOrDefault(dish.getId(), 0.0));
            BigDecimal decimalDemand = BigDecimal.valueOf(demand).setScale(2, RoundingMode.HALF_UP);
            String note = input.noConsumptionHistory().contains(dish.getId()) && demand == 0
                    ? NO_DISH_HISTORY_NOTE : null;
            return new DishValue(dish.getId(), interest, decimalDemand,
                    BigDecimal.valueOf(heuristic.recommend(demand, safetyBuffer)).setScale(2, RoundingMode.HALF_UP),
                    itemUnit, null, note, null, null);
        }).toList();
        return new ForecastValues(forecast.predicted(), forecast.lower(), forecast.upper(), forecast.confidence(),
                forecast.recommended(), ForecastSource.HEURISTIC, null, null, null, fallbackReason,
                forecast.factors(), dishes);
    }

    private ForecastValues mlValues(MlForecastClient.MlResponse response, ForecastInput input) {
        MlForecastClient.Forecast ml = response.forecast();
        int predicted = Math.min(input.eligible(), ml.predictedAttendance());
        MlForecastClient.PredictionInterval interval = response.attendanceInterval();
        int lower = interval == null ? predicted : clamp((int) Math.round(interval.lower()), 0, input.eligible());
        int upper = interval == null ? predicted : clamp((int) Math.round(interval.upper()), 0, input.eligible());
        lower = Math.min(lower, predicted);
        upper = Math.max(upper, predicted);
        double confidence = interval == null ? 0 : interval.targetCoverage();
        ForecastSource source = "ml".equals(ml.forecastMethod())
                ? ForecastSource.ML_SERVICE : ForecastSource.ML_FALLBACK;
        Map<Long, MlForecastClient.DishDetails> detailsByDish = new HashMap<>();
        response.dishDetails().forEach(value -> detailsByDish.put(value.dishId(), value));
        List<DishValue> dishes = ml.dishForecasts().stream().map(value -> {
            MlForecastClient.DishDetails details = detailsByDish.get(value.dishId());
            BigDecimal lowerValue = details.interval() == null ? null : decimal(details.interval().lower());
            BigDecimal upperValue = details.interval() == null ? null : decimal(details.interval().upper());
            return new DishValue(value.dishId(),
                    input.interest().getOrDefault(value.dishId(), 0), decimal(value.predictedQuantity()),
                    decimal(value.recommendedQuantity()), value.unit(), details.method(), details.note(),
                    lowerValue, upperValue);
        }).toList();
        return new ForecastValues(predicted, lower, upper, confidence, recommend(predicted),
                source, ml.modelVersion(), ml.forecastMethod(), response.dataSource(), response.fallbackReason(),
                List.of(), dishes);
    }

    private ForecastResponse persist(Meal meal, ForecastValues values, Map<Long, Integer> interest) {
        mealForecasts.findByMealId(meal.getId()).ifPresent(mealForecasts::delete);
        mealForecasts.flush();
        String factors;
        try {
            factors = objectMapper.writeValueAsString(values.factors());
        } catch (JacksonException exception) {
            throw new IllegalStateException("Unable to serialize forecast factors.", exception);
        }
        MealForecast forecast = mealForecasts.save(new MealForecast(meal.getId(), values.predicted(),
                values.lower(), values.upper(), BigDecimal.valueOf(values.confidence()).setScale(3, RoundingMode.HALF_UP),
                values.recommended(), values.source(), factors, clock.instant(), values.modelVersion(),
                values.forecastMethod(), values.dataSource(), values.fallbackReason()));
        dishForecasts.deleteByMealId(meal.getId());
        dishForecasts.flush();
        Map<Long, DishValue> valuesByDish = new HashMap<>();
        values.dishes().forEach(value -> valuesByDish.put(value.dishId(), value));
        List<DishForecast> dishRows = meal.getDishes().stream().map(dish -> {
            DishValue value = valuesByDish.get(dish.getId());
            if (value == null) {
                throw new IllegalStateException("Forecast is missing dish " + dish.getId());
            }
            return new DishForecast(meal.getId(), dish.getId(), interest.getOrDefault(dish.getId(), 0),
                    value.predicted(), value.recommended(), value.unit(), value.method(), value.note(),
                    value.intervalLower(), value.intervalUpper(), forecast.getGeneratedAt());
        }).toList();
        dishForecasts.saveAll(dishRows);
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
                forecast.getRecommendedPreparation(), forecast.getSource(), forecast.getGeneratedAt(), factors,
                forecast.getModelVersion(), forecast.getForecastMethod(), forecast.getDataSource(),
                forecast.getFallbackReason());
    }

    private int count(List<MealResponse> values, AttendanceStatus status) {
        return (int) values.stream().filter(value -> value.getStatus() == status).count();
    }

    static int weekdayIndex(LocalDate date) {
        return date.getDayOfWeek().getValue() - 1;
    }

    static String mealTypeName(MealType mealType) {
        return mealType.name().toLowerCase(java.util.Locale.ROOT);
    }

    static int cappedInterest(int eligible, int interest) {
        return Math.max(0, Math.min(eligible, interest));
    }

    static int noResponse(int eligible, int yes, int no, int unsure) {
        return Math.max(0, eligible - yes - no - unsure);
    }

    static BigDecimal averageConsumption(List<ConsumptionRecord> records) {
        return BigDecimal.valueOf(records.stream().mapToInt(ConsumptionRecord::getConsumedQuantity)
                .average().orElse(0)).setScale(2, RoundingMode.HALF_UP);
    }

    static String preflightFailure(int eligible, int responseCount, int completedHistoryCount) {
        if (eligible <= 0) {
            return "no_eligible_students";
        }
        if (responseCount > eligible) {
            return "invalid_request";
        }
        return completedHistoryCount < 3 ? "insufficient_history" : null;
    }

    private int recommend(int predicted) {
        return (int) Math.ceil(predicted * (1 + safetyBuffer));
    }

    private int clamp(int value, int lower, int upper) {
        return Math.max(lower, Math.min(upper, value));
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    record ForecastInput(int eligible, int yes, int no, int unsure, int responseCount,
            List<Integer> history, double avgAttendance, double avgWastePercentage,
            Map<Long, Integer> interest, Map<Long, Double> popularity,
            Map<Long, BigDecimal> avgConsumption, Set<Long> noConsumptionHistory, int pickers) {
        HeuristicForecaster.ForecastInput heuristicInput() {
            int noResponse = Math.max(0, eligible - responseCount);
            return new HeuristicForecaster.ForecastInput(eligible, yes, no, unsure, noResponse,
                    history, interest, pickers);
        }
    }

    private record DishValue(Long dishId, int studentInterest, BigDecimal predicted,
            BigDecimal recommended, String unit, String method, String note,
            BigDecimal intervalLower, BigDecimal intervalUpper) {
    }

    private record ForecastValues(int predicted, int lower, int upper, double confidence, int recommended,
            ForecastSource source, String modelVersion, String forecastMethod, String dataSource,
            String fallbackReason, List<ForecastFactor> factors, List<DishValue> dishes) {
    }
}
