package com.preplate.intelligence.controller;

import static com.preplate.common.dto.ApiDtos.*;

import com.preplate.common.enums.MealType;
import com.preplate.intelligence.service.ForecastService;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {
    private final ForecastService forecasts;

    public AdminDashboardController(ForecastService forecasts) {
        this.forecasts = forecasts;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mealDate,
            @RequestParam @NotNull MealType mealType) {
        return forecasts.dashboard(mealDate, mealType);
    }

    @PostMapping("/forecasts/{mealId}/refresh")
    public DashboardResponse refresh(@PathVariable Long mealId) {
        return forecasts.refresh(mealId);
    }
}
