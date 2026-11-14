package com.preplate.meal.controller;

import static com.preplate.common.dto.ApiDtos.MealScheduleResponse;

import com.preplate.meal.service.MealScheduleService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meal-schedule")
public class MealScheduleController {
    private final MealScheduleService schedules;

    public MealScheduleController(MealScheduleService schedules) {
        this.schedules = schedules;
    }

    @GetMapping
    public List<MealScheduleResponse> list() {
        return schedules.list();
    }
}
