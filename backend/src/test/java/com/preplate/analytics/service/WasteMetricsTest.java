package com.preplate.analytics.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class WasteMetricsTest {
    @Test
    void shouldRoundWastePercentageWhenPreparedIsPositive() {
        assertEquals(12.5, WasteMetrics.wastePercentage(40, 5));
    }

    @Test
    void shouldReturnZeroWastePercentageWhenNothingWasPrepared() {
        assertEquals(0, WasteMetrics.wastePercentage(0, 0));
    }

    @Test
    void shouldClampForecastAccuracyWhenPredictionErrorIsLarge() {
        assertEquals(0, WasteMetrics.forecastAccuracy(List.of(
                new WasteMetrics.ForecastComparison(100, 1))));
        assertEquals(100, WasteMetrics.forecastAccuracy(List.of(
                new WasteMetrics.ForecastComparison(10, 10))));
        assertEquals(0, WasteMetrics.forecastAccuracy(List.of()));
    }
}
