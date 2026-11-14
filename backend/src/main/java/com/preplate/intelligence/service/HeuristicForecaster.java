package com.preplate.intelligence.service;

import static com.preplate.common.dto.ApiDtos.ForecastFactor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HeuristicForecaster {
    public ForecastValues forecast(ForecastInput input, double safetyBuffer) {
        double historicalRate = input.history().isEmpty() ? 0.75
                : input.history().stream().mapToDouble(value -> value / (double) Math.max(1, input.eligible()))
                        .average().orElse(0.75);
        double confirmedTerm = input.confirmed();
        double uncertainTerm = 0.5 * input.uncertain();
        double noResponseTerm = input.noResponse() * historicalRate * 0.5;
        int predicted = Math.min(input.eligible(), (int) Math.round(
                confirmedTerm + uncertainTerm + noResponseTerm));
        int responded = input.confirmed() + input.notAttending() + input.uncertain();
        double confidence = Math.min(0.95, 0.55 + 0.4 * responded / (double) Math.max(1, input.eligible()));
        int halfWidth = (int) Math.ceil(predicted * (1 - confidence) * 0.5 + 1);
        int lower = Math.max(input.confirmed(), predicted - halfWidth);
        int upper = Math.min(input.eligible(), predicted + halfWidth);
        int recommended = (int) Math.ceil(predicted * (1 + safetyBuffer));
        double total = confirmedTerm + uncertainTerm + noResponseTerm;
        List<ForecastFactor> factors = new ArrayList<>();
        if (total == 0) {
            double equal = 0.25;
            factors.add(new ForecastFactor("Confirmed attendance", equal));
            factors.add(new ForecastFactor("Uncertain responses", equal));
            factors.add(new ForecastFactor("Non-respondents (historical show-up rate)", equal));
            factors.add(new ForecastFactor("Historical attendance for this meal type", equal));
        } else {
            factors.add(new ForecastFactor("Confirmed attendance", confirmedTerm / total));
            factors.add(new ForecastFactor("Uncertain responses", uncertainTerm / total));
            factors.add(new ForecastFactor("Non-respondents (historical show-up rate)", noResponseTerm / total));
            factors.add(new ForecastFactor("Historical attendance for this meal type",
                    Math.max(0, total - confirmedTerm - uncertainTerm - noResponseTerm) / total));
        }
        return new ForecastValues(predicted, lower, upper, confidence, recommended, factors,
                historicalRate, noResponseTerm, total);
    }

    public int predictDish(int predicted, int interest, int pickers, double popularity) {
        int demand = Math.min(predicted, interest + (int) Math.round((predicted - pickers) * popularity));
        return Math.max(interest, demand);
    }

    public int recommend(int demand, double safetyBuffer) {
        return (int) Math.ceil(demand * (1 + safetyBuffer));
    }

    public record ForecastInput(int eligible, int confirmed, int notAttending, int uncertain,
            int noResponse, List<Integer> history, Map<Long, Integer> interest, int pickers) {
    }

    public record ForecastValues(int predicted, int lower, int upper, double confidence, int recommended,
            List<ForecastFactor> factors, double historicalRate, double noResponseTerm, double total) {
    }
}
