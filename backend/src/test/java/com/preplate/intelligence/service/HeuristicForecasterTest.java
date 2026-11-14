package com.preplate.intelligence.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class HeuristicForecasterTest {
    private final HeuristicForecaster forecaster = new HeuristicForecaster();

    @Test
    void shouldUseHistoricalShowUpRateWhenPredictingNoResponses() {
        var noHistory = forecaster.forecast(new HeuristicForecaster.ForecastInput(
                100, 0, 0, 0, 100, List.of(), Map.of(), 0), 0.05);
        var withHistory = forecaster.forecast(new HeuristicForecaster.ForecastInput(
                100, 0, 0, 0, 100, List.of(50, 50), Map.of(), 0), 0.05);

        assertEquals(38, noHistory.predicted());
        assertEquals(25, withHistory.predicted());
    }

    @Test
    void shouldKeepBoundsAndRecommendationsWithinValidRangeWhenAllStudentsConfirmed() {
        var result = forecaster.forecast(new HeuristicForecaster.ForecastInput(
                12, 12, 0, 0, 0, List.of(), Map.of(), 0), 0.05);

        assertEquals(12, result.predicted());
        assertTrue(result.lower() >= 12);
        assertTrue(result.upper() <= 12);
        assertTrue(result.recommended() >= result.predicted());
        assertEquals(1.0, result.factors().stream().mapToDouble(value -> value.importance()).sum(), 0.0001);
    }

    @Test
    void shouldBoundDishDemandBetweenInterestAndPredictedAttendance() {
        int demand = forecaster.predictDish(30, 8, 10, 0.7);

        assertTrue(demand >= 8);
        assertTrue(demand <= 30);
        assertTrue(forecaster.recommend(demand, 0.05) >= demand);
    }
}
