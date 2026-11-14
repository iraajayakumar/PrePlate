package com.preplate.analytics.controller;

import static com.preplate.common.dto.ApiDtos.*;

import com.preplate.analytics.service.ConsumptionService;
import com.preplate.analytics.service.WasteAnalyticsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminConsumptionController {
    private final ConsumptionService consumption;
    private final WasteAnalyticsService analytics;

    public AdminConsumptionController(ConsumptionService consumption, WasteAnalyticsService analytics) {
        this.consumption = consumption;
        this.analytics = analytics;
    }

    @GetMapping("/consumption/{mealId}")
    public ConsumptionResponse get(@PathVariable Long mealId) {
        return consumption.get(mealId);
    }

    @PostMapping("/consumption")
    public ConsumptionResponse save(@Valid @RequestBody SaveConsumptionRequest request) {
        return consumption.save(request);
    }

    @GetMapping("/analytics/waste")
    public WasteAnalyticsResponse waste(@RequestParam(defaultValue = "14") int days) {
        return analytics.analyze(days);
    }
}
