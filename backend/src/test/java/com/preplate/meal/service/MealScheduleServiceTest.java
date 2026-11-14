package com.preplate.meal.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.preplate.common.enums.MealType;
import com.preplate.meal.entity.MealSchedule;
import com.preplate.meal.repository.MealScheduleRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class MealScheduleServiceTest {
    @Mock
    private MealScheduleRepository repository;
    private MealScheduleService service;

    @BeforeEach
    void setUp() {
        service = new MealScheduleService(repository, "Asia/Kolkata");
        lenient().when(repository.findById(any())).thenAnswer(invocation ->
                Optional.of(schedule((MealType) invocation.getArgument(0))));
    }

    @Test
    void shouldCalculateDeadlineForEveryMealTypeWhenUsingSchedule() {
        LocalDate date = LocalDate.of(2026, 11, 14);
        assertEquals(Instant.parse("2026-11-13T12:30:00Z"), service.deadlineAt(date, MealType.BREAKFAST));
        assertEquals(Instant.parse("2026-11-13T16:30:00Z"), service.deadlineAt(date, MealType.LUNCH));
        assertEquals(Instant.parse("2026-11-14T04:30:00Z"), service.deadlineAt(date, MealType.SNACKS));
        assertEquals(Instant.parse("2026-11-14T08:30:00Z"), service.deadlineAt(date, MealType.DINNER));
    }

    @Test
    void shouldTreatDeadlineAsClosedWhenNowIsAtOrAfterDeadline() {
        LocalDate date = LocalDate.of(2026, 11, 14);
        Instant deadline = service.deadlineAt(date, MealType.DINNER);
        assertTrue(service.isOpen(date, MealType.DINNER, deadline.minusNanos(1)));
        assertFalse(service.isOpen(date, MealType.DINNER, deadline));
        assertFalse(service.isOpen(date, MealType.DINNER, deadline.plusSeconds(1)));
    }

    @Test
    void shouldLabelPreviousDayDeadlineWhenScheduleHasNegativeOffset() {
        when(repository.findAllByOrderByMealTypeAsc()).thenReturn(List.of(schedule(MealType.BREAKFAST)));
        assertEquals("6 PM, day before", service.list().get(0).deadlineLabel());
    }

    private MealSchedule schedule(MealType type) {
        return switch (type) {
            case BREAKFAST -> new MealSchedule(type, LocalTime.of(7, 30), LocalTime.of(9, 30), LocalTime.of(18, 0), -1);
            case LUNCH -> new MealSchedule(type, LocalTime.of(12, 30), LocalTime.of(14, 30), LocalTime.of(22, 0), -1);
            case SNACKS -> new MealSchedule(type, LocalTime.of(16, 30), LocalTime.of(18, 0), LocalTime.of(10, 0), 0);
            case DINNER -> new MealSchedule(type, LocalTime.of(19, 30), LocalTime.of(21, 30), LocalTime.of(14, 0), 0);
        };
    }
}
