package com.preplate.intelligence.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.preplate.analytics.entity.ConsumptionRecord;
import com.preplate.common.enums.MealType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class ForecastRequestMapperTest {
    @Test
    void shouldMapMondayAndSundayToZeroBasedWeekdays() {
        assertEquals(0, ForecastService.weekdayIndex(LocalDate.of(2026, 3, 2)));
        assertEquals(6, ForecastService.weekdayIndex(LocalDate.of(2026, 3, 1)));
    }

    @Test
    void shouldMapMealTypesToLowercaseContractNames() {
        assertEquals("breakfast", ForecastService.mealTypeName(MealType.BREAKFAST));
        assertEquals("dinner", ForecastService.mealTypeName(MealType.DINNER));
        assertEquals("snacks", ForecastService.mealTypeName(MealType.SNACKS));
    }

    @Test
    void shouldComputeNoResponseAndKeepResponseGroupsSummedToEligibleStudents() {
        int eligible = 10;
        int noResponse = ForecastService.noResponse(eligible, 3, 2, 1);
        assertEquals(4, noResponse);
        assertEquals(eligible, 3 + 2 + 1 + noResponse);
        assertEquals(0, ForecastService.noResponse(eligible, 8, 2, 1));
    }

    @Test
    void shouldCapDishInterestAtEligibleStudents() {
        assertEquals(10, ForecastService.cappedInterest(10, 12));
        assertEquals(3, ForecastService.cappedInterest(10, 3));
    }

    @Test
    void shouldUseZeroForDishConsumptionWithoutHistory() {
        assertEquals(new BigDecimal("0.00"), ForecastService.averageConsumption(List.of()));
        ConsumptionRecord record = new ConsumptionRecord(1L, 2L, null, 10, 8, Instant.EPOCH);
        assertEquals(new BigDecimal("8.00"), ForecastService.averageConsumption(List.of(record)));
    }

    @Test
    void shouldSkipMlWhenThereAreFewerThanThreeCompletedMeals() {
        assertEquals("insufficient_history", ForecastService.preflightFailure(10, 4, 2));
        assertNull(ForecastService.preflightFailure(10, 4, 3));
        assertEquals("no_eligible_students", ForecastService.preflightFailure(0, 0, 3));
        assertEquals("invalid_request", ForecastService.preflightFailure(10, 11, 3));
    }

    @Test
    void shouldPassDishPreferencePickersToTheHeuristicInput() {
        ForecastService.ForecastInput input = new ForecastService.ForecastInput(10, 0, 0, 0, 0,
                List.of(), 0, 0, java.util.Map.of(), java.util.Map.of(), java.util.Map.of(), java.util.Set.of(), 3);

        assertEquals(3, input.heuristicInput().pickers());
    }
}
