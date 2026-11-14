package com.preplate.analytics.service;

import java.util.List;

public final class WasteMetrics {
    private WasteMetrics() {
    }

    public static double wastePercentage(double prepared, double leftover) {
        return prepared == 0 ? 0 : round(leftover / prepared * 100);
    }

    public static double forecastAccuracy(List<ForecastComparison> values) {
        if (values.isEmpty()) {
            return 0;
        }
        double meanError = values.stream()
                .mapToDouble(value -> Math.abs(value.predictedDemand() - value.consumedQuantity())
                        / (double) Math.max(value.consumedQuantity(), 1) * 100)
                .average().orElse(100);
        return round(Math.max(0, Math.min(100, 100 - meanError)));
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    public record ForecastComparison(int predictedDemand, int consumedQuantity) {
    }
}
