package com.preplate.meal.service;

import static com.preplate.common.dto.ApiDtos.*;

import com.preplate.common.enums.MealType;
import com.preplate.common.exception.ConflictException;
import com.preplate.common.exception.NotFoundException;
import com.preplate.meal.entity.Dish;
import com.preplate.meal.entity.Meal;
import com.preplate.meal.repository.DishRepository;
import com.preplate.meal.repository.MealRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminMealService {
    private final DishRepository dishes;
    private final MealRepository meals;
    private final MealScheduleService schedules;

    public AdminMealService(DishRepository dishes, MealRepository meals, MealScheduleService schedules) {
        this.dishes = dishes;
        this.meals = meals;
        this.schedules = schedules;
    }

    @Transactional(readOnly = true)
    public List<DishResponse> listDishes() {
        return dishes.findAllByOrderByNameAsc().stream().map(MealDtoMapper::dish).toList();
    }

    @Transactional
    public DishResponse createDish(CreateDishRequest request) {
        if (dishes.existsByNameIgnoreCase(request.name().trim())) {
            throw new ConflictException("A dish with this name already exists.");
        }
        try {
            Dish dish = dishes.saveAndFlush(new Dish(request.name().trim(), request.description(),
                    request.calories(), BigDecimal.valueOf(request.proteinG()),
                    BigDecimal.valueOf(request.carbsG()), BigDecimal.valueOf(request.fatG()),
                    request.ingredients() == null ? "" : String.join(", ", request.ingredients()),
                    request.vegetarian(), null));
            return MealDtoMapper.dish(dish);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("A dish with this name already exists.");
        }
    }

    @Transactional(readOnly = true)
    public List<AdminMealResponse> listMeals(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' must be on or before 'to'.");
        }
        return meals.findByMealDateBetweenOrderByMealDateAsc(from, to).stream()
                .sorted(Comparator.comparing(Meal::getMealDate).thenComparing(Meal::getMealType))
                .map(this::response).toList();
    }

    @Transactional
    public AdminMealResponse createMeal(CreateMealRequest request) {
        if (meals.existsByMealDateAndMealType(request.mealDate(), request.mealType())) {
            throw new ConflictException("A meal is already planned for this date and type.");
        }
        Set<Dish> selectedDishes = findDishes(request.dishIds());
        try {
            return response(meals.saveAndFlush(new Meal(request.mealDate(), request.mealType(), selectedDishes)));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("A meal is already planned for this date and type.");
        }
    }

    @Transactional
    public AdminMealResponse updateMeal(Long mealId, UpdateMealRequest request) {
        Meal meal = meal(mealId);
        meal.setDishes(findDishes(request.dishIds()));
        return response(meals.save(meal));
    }

    @Transactional
    public MessageResponse deleteMeal(Long mealId) {
        Meal meal = meal(mealId);
        meals.delete(meal);
        return new MessageResponse("Deleted");
    }

    @Transactional(readOnly = true)
    public Meal getMeal(Long mealId) {
        return meal(mealId);
    }

    private Set<Dish> findDishes(List<Long> ids) {
        Set<Long> uniqueIds = new HashSet<>(ids);
        if (uniqueIds.size() != ids.size()) {
            throw new IllegalArgumentException("Dish ids must be unique.");
        }
        List<Dish> found = dishes.findAllById(uniqueIds);
        if (found.size() != uniqueIds.size()) {
            throw new IllegalArgumentException("Every dish id must exist.");
        }
        return new HashSet<>(found);
    }

    private Meal meal(Long mealId) {
        return meals.findWithDishesById(mealId).orElseThrow(() -> new NotFoundException("Meal not found."));
    }

    private AdminMealResponse response(Meal meal) {
        return new AdminMealResponse(meal.getId(), meal.getMealDate(), meal.getMealType(),
                schedules.deadlineAt(meal.getMealDate(), meal.getMealType()),
                meal.getDishes().stream().sorted(Comparator.comparing(Dish::getName))
                        .map(MealDtoMapper::dish).toList());
    }
}
