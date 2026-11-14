package com.preplate.notification;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.preplate.common.enums.MealType;
import com.preplate.meal.entity.Meal;
import com.preplate.meal.service.MealScheduleService;
import com.preplate.meal.repository.MealRepository;
import com.preplate.meal.repository.MealResponseRepository;
import com.preplate.notification.repository.NotificationRepository;
import com.preplate.notification.service.NotificationService;
import com.preplate.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("unit")
class NotificationServiceTest {
    @Test
    void shouldCreateReminderOnlyInsideDeadlineWindowWhenStudentHasNoResponse() {
        NotificationRepository notifications = mock(NotificationRepository.class);
        MealRepository meals = mock(MealRepository.class);
        MealResponseRepository responses = mock(MealResponseRepository.class);
        UserRepository users = mock(UserRepository.class);
        MealScheduleService schedules = mock(MealScheduleService.class);
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus());
        Meal dinner = new Meal(LocalDate.of(2026, 11, 14), MealType.DINNER, java.util.Set.of());
        ReflectionTestUtils.setField(dinner, "id", 91L);
        Instant deadline = Instant.parse("2026-11-14T08:30:00Z");
        when(meals.findByMealDateBetween(any(), any())).thenReturn(List.of(dinner));
        when(responses.findByUserIdAndMealIdIn(eq(5L), any())).thenReturn(List.of());
        when(schedules.deadlineAt(dinner.getMealDate(), dinner.getMealType())).thenReturn(deadline);
        when(schedules.mealLabel(MealType.DINNER)).thenReturn("Dinner");
        when(notifications.existsByUserIdAndDedupeKey(anyLong(), anyString())).thenReturn(false);
        Clock at1310 = Clock.fixed(Instant.parse("2026-11-14T07:40:00Z"), ZoneId.of("Asia/Kolkata"));
        NotificationService service = new NotificationService(notifications, meals, responses, users, schedules,
                at1310, transactions, 30, "Asia/Kolkata");

        service.generateTimeBasedNotifications(5L);
        verify(notifications, never()).saveAndFlush(any());

        Clock at1340 = Clock.fixed(Instant.parse("2026-11-14T08:10:00Z"), ZoneId.of("Asia/Kolkata"));
        NotificationService inWindow = new NotificationService(notifications, meals, responses, users, schedules,
                at1340, transactions, 30, "Asia/Kolkata");
        inWindow.generateTimeBasedNotifications(5L);

        verify(notifications).saveAndFlush(argThat(value ->
                value.getDedupeKey().equals("REMINDER:91")
                        && value.getMessage().contains("2:00 PM")
                        && value.getMessage().contains("20 minutes")));
    }

    @Test
    void shouldNotCreateReminderAfterDeadlineWhenDeadlineHasPassed() {
        NotificationRepository notifications = mock(NotificationRepository.class);
        MealRepository meals = mock(MealRepository.class);
        MealResponseRepository responses = mock(MealResponseRepository.class);
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        MealScheduleService schedules = mock(MealScheduleService.class);
        when(transactions.getTransaction(any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus());
        Meal dinner = new Meal(LocalDate.of(2026, 11, 14), MealType.DINNER, java.util.Set.of());
        ReflectionTestUtils.setField(dinner, "id", 92L);
        when(meals.findByMealDateBetween(any(), any())).thenReturn(List.of(dinner));
        when(responses.findByUserIdAndMealIdIn(anyLong(), any())).thenReturn(List.of());
        when(schedules.deadlineAt(any(), any())).thenReturn(Instant.parse("2026-11-14T08:30:00Z"));
        NotificationService service = new NotificationService(notifications, meals, responses, mock(UserRepository.class),
                schedules, Clock.fixed(Instant.parse("2026-11-14T08:31:00Z"), ZoneId.of("Asia/Kolkata")),
                transactions, 30, "Asia/Kolkata");

        service.generateTimeBasedNotifications(5L);

        verify(notifications, never()).saveAndFlush(any());
    }
}
