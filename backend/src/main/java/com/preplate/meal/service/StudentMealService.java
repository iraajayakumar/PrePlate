package com.preplate.meal.service;

import static com.preplate.common.dto.ApiDtos.*;

import com.preplate.common.enums.AttendanceStatus;
import com.preplate.common.enums.MealType;
import com.preplate.common.exception.ConflictException;
import com.preplate.common.exception.NotFoundException;
import com.preplate.meal.entity.Dish;
import com.preplate.meal.entity.DishPreference;
import com.preplate.meal.entity.Meal;
import com.preplate.meal.entity.MealResponse;
import com.preplate.meal.repository.DishPreferenceRepository;
import com.preplate.meal.repository.DishRepository;
import com.preplate.meal.repository.MealRepository;
import com.preplate.meal.repository.MealResponseRepository;
import com.preplate.notification.service.NotificationService;
import com.preplate.user.entity.User;
import com.preplate.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class StudentMealService {
    private final MealRepository meals;
    private final MealResponseRepository responses;
    private final DishPreferenceRepository preferences;
    private final DishRepository dishes;
    private final UserRepository users;
    private final MealScheduleService scheduleService;
    private final NotificationService notificationService;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;
    private final ConcurrentHashMap<String, Object> responseLocks = new ConcurrentHashMap<>();

    public StudentMealService(MealRepository meals, MealResponseRepository responses,
            DishPreferenceRepository preferences, DishRepository dishes, UserRepository users,
            MealScheduleService scheduleService, NotificationService notificationService, Clock clock,
            PlatformTransactionManager transactionManager) {
        this.meals = meals;
        this.responses = responses;
        this.preferences = preferences;
        this.dishes = dishes;
        this.users = users;
        this.scheduleService = scheduleService;
        this.notificationService = notificationService;
        this.clock = clock;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public UpcomingMealsResponse upcoming(Long userId) {
        Instant now = clock.instant();
        LocalDate today = LocalDate.now(clock);
        List<Meal> plannedMeals = meals.findByMealDateBetween(today, today.plusDays(7));
        Map<MealType, Meal> nextMealByType = new HashMap<>();
        for (Meal meal : plannedMeals) {
            Instant deadline = scheduleService.deadlineAt(meal.getMealDate(), meal.getMealType());
            if (deadline.isAfter(now) && !nextMealByType.containsKey(meal.getMealType())) {
                nextMealByType.put(meal.getMealType(), meal);
            }
        }
        List<Meal> selectedMeals = nextMealByType.values().stream()
                .sorted(Comparator.comparing(Meal::getMealType)).toList();
        List<Long> mealIds = selectedMeals.stream().map(Meal::getId).toList();
        if (mealIds.isEmpty()) {
            return new UpcomingMealsResponse(now, List.of());
        }
        Map<Long, MealResponse> responseByMeal = new HashMap<>();
        responses.findByUserIdAndMealIdIn(userId, mealIds)
                .forEach(response -> responseByMeal.put(response.getMealId(), response));
        Map<Long, List<Long>> preferenceByMeal = new HashMap<>();
        preferences.findByUserIdAndMealIdIn(userId, mealIds).forEach(preference ->
                preferenceByMeal.computeIfAbsent(preference.getMealId(), ignored -> new ArrayList<>())
                        .add(preference.getDishId()));
        List<UpcomingMealResponse> result = selectedMeals.stream().map(meal -> {
            MealResponse response = responseByMeal.get(meal.getId());
            return new UpcomingMealResponse(meal.getId(), meal.getMealType(), meal.getMealDate(),
                    scheduleService.deadlineAt(meal.getMealDate(), meal.getMealType()),
                    response == null ? null : new MealResponseStatus(response.getStatus(), response.getRespondedAt()),
                    preferenceByMeal.getOrDefault(meal.getId(), List.of()),
                    meal.getDishes().stream().sorted(Comparator.comparing(Dish::getName))
                            .map(MealDtoMapper::dish).toList());
        }).toList();
        return new UpcomingMealsResponse(now, result);
    }

    public SetResponseResult respond(Long userId, Long mealId, AttendanceStatus status) {
        String lockKey = userId + ":" + mealId;
        Object lock = responseLocks.computeIfAbsent(lockKey, ignored -> new Object());
        synchronized (lock) {
            SetResponseResult result = transactionTemplate.execute(transaction -> {
                Meal meal = meal(mealId);
                Instant now = clock.instant();
                if (!scheduleService.isOpen(meal.getMealDate(), meal.getMealType(), now)) {
                    throw new ConflictException("Responses for this meal are closed.");
                }
                MealResponse response = responses.findByUserIdAndMealId(userId, mealId)
                        .orElseGet(() -> new MealResponse(userId, mealId, status, now));
                response.setStatus(status);
                response.setRespondedAt(now);
                response.setUpdatedAt(now);
                MealResponse saved = responses.saveAndFlush(response);
                if (status != AttendanceStatus.ATTENDING) {
                    preferences.deleteForUserMeal(userId, mealId);
                }
                notificationService.createConfirmation(userId, meal, status, now);
                return new SetResponseResult(mealId, saved.getStatus(), saved.getRespondedAt());
            });
            return result;
        }
    }

    public DishPreferencesResponse setDishPreferences(Long userId, Long mealId, List<Long> dishIds) {
        return transactionTemplate.execute(transaction -> {
            Meal meal = meal(mealId);
            Instant now = clock.instant();
            if (!scheduleService.isOpen(meal.getMealDate(), meal.getMealType(), now)) {
                throw new ConflictException("Responses for this meal are closed.");
            }
            MealResponse response = responses.findByUserIdAndMealId(userId, mealId)
                    .orElseThrow(() -> new ConflictException("Confirm your attendance before choosing dishes."));
            if (response.getStatus() != AttendanceStatus.ATTENDING) {
                throw new ConflictException("Confirm your attendance before choosing dishes.");
            }
            Set<Long> selected = new HashSet<>(dishIds);
            Set<Long> mealDishIds = new HashSet<>(meal.getDishes().stream().map(Dish::getId).toList());
            if (!mealDishIds.containsAll(selected) || selected.size() != dishIds.size()) {
                throw new IllegalArgumentException("Every selected dish must belong to this meal.");
            }
            preferences.deleteForUserMeal(userId, mealId);
            preferences.saveAll(selected.stream().map(id -> new DishPreference(userId, mealId, id)).toList());
            return new DishPreferencesResponse(mealId, selected.stream().sorted().toList());
        });
    }

    public HistoryResponse history(Long userId, String period) {
        LocalDate today = LocalDate.now(clock);
        LocalDate from = switch (period) {
            case "TODAY" -> today;
            case "LAST_WEEK" -> today.minusDays(6);
            case "LAST_MONTH" -> today.minusDays(29);
            default -> throw new IllegalArgumentException("Invalid period.");
        };
        List<Meal> planned = meals.findByMealDateBetween(from, today);
        Map<Long, Meal> mealById = new HashMap<>();
        planned.forEach(meal -> mealById.put(meal.getId(), meal));
        List<Long> mealIds = mealById.keySet().stream().toList();
        Map<Long, List<Long>> selectedDishIds = new HashMap<>();
        if (!mealIds.isEmpty()) {
            preferences.findByUserIdAndMealIdIn(userId, mealIds).forEach(preference ->
                    selectedDishIds.computeIfAbsent(preference.getMealId(), ignored -> new ArrayList<>())
                            .add(preference.getDishId()));
        }
        Map<Long, Dish> dishById = new HashMap<>();
        dishes.findAllById(selectedDishIds.values().stream().flatMap(List::stream).distinct().toList())
                .forEach(dish -> dishById.put(dish.getId(), dish));
        Map<MealType, List<HistoryEntry>> entries = new HashMap<>();
        for (MealResponse response : responses.findByUserIdAndMealIdIn(userId, mealIds)) {
            Meal meal = mealById.get(response.getMealId());
            List<String> names = selectedDishIds.getOrDefault(meal.getId(), List.of()).stream()
                    .map(dishById::get).filter(java.util.Objects::nonNull).map(Dish::getName).sorted().toList();
            entries.computeIfAbsent(meal.getMealType(), ignored -> new ArrayList<>())
                    .add(new HistoryEntry(meal.getId(), meal.getMealDate(), response.getStatus(), names));
        }
        List<HistoryCategory> categories = List.of(MealType.values()).stream().map(type ->
                new HistoryCategory(type, entries.getOrDefault(type, List.of()).stream()
                        .sorted(Comparator.comparing(HistoryEntry::mealDate).reversed()).toList())).toList();
        return new HistoryResponse(period, categories);
    }

    public StudentAnalyticsResponse analytics(Long userId, String scope) {
        LocalDate today = LocalDate.now(clock);
        LocalDate from = switch (scope) {
            case "THIS_MONTH" -> today.withDayOfMonth(1);
            case "ALL_TIME" -> LocalDate.of(1900, 1, 1);
            default -> throw new IllegalArgumentException("Invalid scope.");
        };
        List<Meal> planned = meals.findByMealDateBetween(from, today);
        List<Long> mealIds = planned.stream().map(Meal::getId).toList();
        List<MealResponse> userResponses = mealIds.isEmpty() ? List.of()
                : responses.findByUserIdAndMealIdIn(userId, mealIds);
        Map<Long, Meal> mealById = new HashMap<>();
        planned.forEach(meal -> mealById.put(meal.getId(), meal));
        List<Participation> participation = List.of(MealType.values()).stream().map(type -> {
            long available = planned.stream().filter(meal -> meal.getMealType() == type).count();
            List<MealResponse> typed = userResponses.stream()
                    .filter(response -> mealById.get(response.getMealId()).getMealType() == type).toList();
            long selected = typed.stream().filter(r -> r.getStatus() == AttendanceStatus.ATTENDING).count();
            long attended = typed.stream().filter(r -> r.getStatus() == AttendanceStatus.ATTENDING
                    && Boolean.TRUE.equals(r.getAttended())).count();
            return new Participation(type, available, selected, attended);
        }).toList();

        Map<Long, Dish> dishById = new HashMap<>();
        Map<Long, Long> counts = new HashMap<>();
        if (!mealIds.isEmpty()) {
            preferences.findByUserIdAndMealIdIn(userId, mealIds).forEach(preference -> {
                counts.merge(preference.getDishId(), 1L, Long::sum);
                mealById.computeIfPresent(preference.getMealId(), (id, meal) -> meal);
            });
            dishes.findAllById(counts.keySet()).forEach(dish -> dishById.put(dish.getId(), dish));
        }
        List<TopDish> topDishes = counts.entrySet().stream()
                .map(entry -> new TopDish(dishById.get(entry.getKey()).getName(), entry.getValue()))
                .sorted(Comparator.comparingLong(TopDish::timesSelected).reversed().thenComparing(TopDish::dishName))
                .limit(5).toList();
        return new StudentAnalyticsResponse(scope, participation, topDishes);
    }

    private Meal meal(Long mealId) {
        return meals.findWithDishesById(mealId).orElseThrow(() -> new NotFoundException("Meal not found."));
    }
}
