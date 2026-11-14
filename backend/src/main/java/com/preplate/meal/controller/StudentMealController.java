package com.preplate.meal.controller;

import static com.preplate.common.dto.ApiDtos.*;

import com.preplate.auth.AuthInterceptor;
import com.preplate.common.enums.AttendanceStatus;
import com.preplate.meal.service.StudentMealService;
import com.preplate.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class StudentMealController {
    private final StudentMealService meals;

    public StudentMealController(StudentMealService meals) {
        this.meals = meals;
    }

    @GetMapping("/meals/upcoming")
    public UpcomingMealsResponse upcoming(HttpServletRequest request) {
        return meals.upcoming(user(request).getId());
    }

    @PutMapping("/meals/{mealId}/response")
    public SetResponseResult setResponse(@PathVariable Long mealId,
            @Valid @RequestBody SetResponseRequest body, HttpServletRequest request) {
        return meals.respond(user(request).getId(), mealId, body.status());
    }

    @PutMapping("/meals/{mealId}/dish-preferences")
    public DishPreferencesResponse setDishPreferences(@PathVariable Long mealId,
            @Valid @RequestBody DishPreferencesRequest body, HttpServletRequest request) {
        return meals.setDishPreferences(user(request).getId(), mealId, body.dishIds());
    }

    @GetMapping("/student/history")
    public HistoryResponse history(@RequestParam(defaultValue = "TODAY") String period,
            HttpServletRequest request) {
        return meals.history(user(request).getId(), period);
    }

    @GetMapping("/student/analytics")
    public StudentAnalyticsResponse analytics(@RequestParam(defaultValue = "THIS_MONTH") String scope,
            HttpServletRequest request) {
        return meals.analytics(user(request).getId(), scope);
    }

    private User user(HttpServletRequest request) {
        return (User) request.getAttribute(AuthInterceptor.USER_ATTRIBUTE);
    }
}
