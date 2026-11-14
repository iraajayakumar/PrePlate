package com.preplate.meal.service;

import static com.preplate.common.dto.ApiDtos.MealScheduleResponse;

import com.preplate.common.enums.MealType;
import com.preplate.common.exception.NotFoundException;
import com.preplate.meal.entity.MealSchedule;
import com.preplate.meal.repository.MealScheduleRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MealScheduleService {
    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
    private final MealScheduleRepository schedules;
    private final ZoneId zone;

    public MealScheduleService(MealScheduleRepository schedules,
            @Value("${preplate.timezone:Asia/Kolkata}") String timezone) {
        this.schedules = schedules;
        this.zone = ZoneId.of(timezone);
    }

    @Transactional(readOnly = true)
    public List<MealScheduleResponse> list() {
        return schedules.findAllByOrderByMealTypeAsc().stream()
                .map(schedule -> new MealScheduleResponse(schedule.getMealType(), schedule.getServingStart(),
                        schedule.getServingEnd(), schedule.getDeadlineTime(), schedule.getDeadlineDayOffset(),
                        deadlineLabel(schedule)))
                .toList();
    }

    @Transactional(readOnly = true)
    public Instant deadlineAt(LocalDate mealDate, MealType mealType) {
        MealSchedule schedule = schedules.findById(mealType)
                .orElseThrow(() -> new NotFoundException("Meal schedule not found."));
        return LocalDateTime.of(mealDate.plusDays(schedule.getDeadlineDayOffset()), schedule.getDeadlineTime())
                .atZone(zone).toInstant();
    }

    public boolean isOpen(LocalDate mealDate, MealType mealType, Instant now) {
        return now.isBefore(deadlineAt(mealDate, mealType));
    }

    public String mealLabel(MealType mealType) {
        return switch (mealType) {
            case BREAKFAST -> "Breakfast";
            case LUNCH -> "Lunch";
            case SNACKS -> "Snacks";
            case DINNER -> "Dinner";
        };
    }

    private String deadlineLabel(MealSchedule schedule) {
        String time = schedule.getDeadlineTime().format(
                schedule.getDeadlineTime().getMinute() == 0
                        ? DateTimeFormatter.ofPattern("h a", Locale.US)
                        : CLOCK_FORMAT);
        return time + (schedule.getDeadlineDayOffset() == -1 ? ", day before" : ", same day");
    }
}
