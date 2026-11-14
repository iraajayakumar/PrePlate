package com.preplate.meal.controller;

import static com.preplate.common.dto.ApiDtos.*;

import com.preplate.meal.service.AdminMealService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminMealController {
    private final AdminMealService meals;

    public AdminMealController(AdminMealService meals) {
        this.meals = meals;
    }

    @GetMapping("/dishes")
    public List<DishResponse> dishes() {
        return meals.listDishes();
    }

    @PostMapping("/dishes")
    public DishResponse createDish(@Valid @RequestBody CreateDishRequest request) {
        return meals.createDish(request);
    }

    @GetMapping("/meals")
    public List<AdminMealResponse> meals(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return meals.listMeals(from, to);
    }

    @PostMapping("/meals")
    public AdminMealResponse createMeal(@Valid @RequestBody CreateMealRequest request) {
        return meals.createMeal(request);
    }

    @PutMapping("/meals/{mealId}")
    public AdminMealResponse updateMeal(@PathVariable Long mealId, @Valid @RequestBody UpdateMealRequest request) {
        return meals.updateMeal(mealId, request);
    }

    @DeleteMapping("/meals/{mealId}")
    public MessageResponse deleteMeal(@PathVariable Long mealId) {
        return meals.deleteMeal(mealId);
    }
}
