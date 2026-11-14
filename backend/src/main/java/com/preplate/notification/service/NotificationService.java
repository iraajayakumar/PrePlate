package com.preplate.notification.service;

import static com.preplate.common.dto.ApiDtos.*;

import com.preplate.common.enums.AttendanceStatus;
import com.preplate.common.enums.MealType;
import com.preplate.common.enums.NotificationType;
import com.preplate.meal.entity.Meal;
import com.preplate.meal.entity.MealResponse;
import com.preplate.meal.repository.MealRepository;
import com.preplate.meal.repository.MealResponseRepository;
import com.preplate.meal.service.MealScheduleService;
import com.preplate.notification.entity.Notification;
import com.preplate.notification.repository.NotificationRepository;
import com.preplate.user.entity.User;
import com.preplate.user.repository.UserRepository;
import com.preplate.common.enums.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class NotificationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);
    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("h:mm a");
    private final NotificationRepository notifications;
    private final MealRepository meals;
    private final MealResponseRepository responses;
    private final UserRepository users;
    private final MealScheduleService schedules;
    private final Clock clock;
    private final int reminderLeadMinutes;
    private final TransactionTemplate transactions;
    private final ZoneId zone;

    public NotificationService(NotificationRepository notifications, MealRepository meals,
            MealResponseRepository responses, UserRepository users, MealScheduleService schedules, Clock clock,
            PlatformTransactionManager transactionManager,
            @Value("${preplate.notifications.reminder-lead-minutes:30}") int reminderLeadMinutes,
            @Value("${preplate.timezone:Asia/Kolkata}") String timezone) {
        this.notifications = notifications;
        this.meals = meals;
        this.responses = responses;
        this.users = users;
        this.schedules = schedules;
        this.clock = clock;
        this.reminderLeadMinutes = reminderLeadMinutes;
        this.transactions = new TransactionTemplate(transactionManager);
        this.zone = ZoneId.of(timezone);
    }

    public NotificationsResponse list(Long userId) {
        generateTimeBasedNotifications(userId);
        List<NotificationItem> items = notifications.findTop30ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(item -> new NotificationItem(item.getId(), item.getType(), item.getTitle(),
                        item.getMessage(), item.isRead(), item.getCreatedAt())).toList();
        return new NotificationsResponse(notifications.countByUserIdAndReadFalse(userId), items);
    }

    public UpdatedResponse markAllRead(Long userId) {
        List<Notification> unread = notifications.findByUserIdAndReadFalse(userId);
        unread.forEach(item -> item.setRead(true));
        notifications.saveAll(unread);
        return new UpdatedResponse(unread.size());
    }

    public void createConfirmation(Long userId, Meal meal, AttendanceStatus status, Instant now) {
        String label = schedules.mealLabel(meal.getMealType()).toLowerCase(java.util.Locale.ROOT);
        String message = status == AttendanceStatus.ATTENDING
                ? "Your " + label + " preference has been recorded."
                : "Your response has been saved.";
        saveIfAbsent(new Notification(userId, NotificationType.CONFIRMATION, "Response saved", message,
                "CONFIRM:" + meal.getId() + ":" + now.getEpochSecond(), now));
        if (status == AttendanceStatus.ATTENDING) {
            saveIfAbsent(new Notification(userId, NotificationType.OPTIONAL_PREFERENCE, "Choose dishes",
                    "Want to be more specific? You can still add dish-level preferences.",
                    "OPTIONAL:" + meal.getId(), now));
        }
    }

    public void generateTimeBasedNotifications(Long userId) {
        Instant now = clock.instant();
        LocalDate today = LocalDate.now(clock);
        List<Meal> planned = meals.findByMealDateBetween(today, today.plusDays(2));
        if (planned.isEmpty()) {
            return;
        }
        Map<Long, MealResponse> responseByMeal = new HashMap<>();
        responses.findByUserIdAndMealIdIn(userId, planned.stream().map(Meal::getId).toList())
                .forEach(response -> responseByMeal.put(response.getMealId(), response));
        generateForUser(userId, planned, responseByMeal, now);
    }

    public void generateTimeBasedNotificationsForAllStudents() {
        Instant now = clock.instant();
        LocalDate today = LocalDate.now(clock);
        List<User> students = users.findAllByRole(Role.STUDENT);
        List<Meal> planned = meals.findByMealDateBetween(today, today.plusDays(2));
        if (students.isEmpty() || planned.isEmpty()) {
            return;
        }
        Set<Long> studentIds = new HashSet<>();
        students.forEach(user -> studentIds.add(user.getId()));
        Map<Long, Map<Long, MealResponse>> responseByStudent = new HashMap<>();
        responses.findByMealIdIn(planned.stream().map(Meal::getId).toList()).stream()
                .filter(response -> studentIds.contains(response.getUserId()))
                .forEach(response -> responseByStudent.computeIfAbsent(response.getUserId(), ignored -> new HashMap<>())
                        .put(response.getMealId(), response));
        for (User student : students) {
            generateForUser(student.getId(), planned,
                    responseByStudent.getOrDefault(student.getId(), Map.of()), now);
        }
    }

    private void generateForUser(Long userId, List<Meal> planned,
            Map<Long, MealResponse> responseByMeal, Instant now) {
        for (Meal meal : planned) {
            Instant deadline = schedules.deadlineAt(meal.getMealDate(), meal.getMealType());
            MealResponse response = responseByMeal.get(meal.getId());
            if (response == null && deadline.isAfter(now)
                    && !now.isBefore(deadline.minusSeconds(reminderLeadMinutes * 60L))) {
                long minutesLeft = Math.max(1, (long) Math.ceil(
                        Duration.between(now, deadline).toMillis() / 60000.0));
                String mealLabel = schedules.mealLabel(meal.getMealType());
                String deadlineText = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
                        .withZone(zone).format(deadline);
                String message = "Time to mark your preference for the next meal! " + mealLabel
                        + " closes at " + deadlineText + " (in " + minutesLeft + " minutes).";
                saveIfAbsent(new Notification(userId, NotificationType.REMINDER, "Reminder", message,
                        "REMINDER:" + meal.getId(), now));
            }
            if (response != null && response.getStatus() == AttendanceStatus.ATTENDING
                    && !deadline.isAfter(now) && !meal.getMealDate().isBefore(LocalDate.now(clock))) {
                saveIfAbsent(new Notification(userId, NotificationType.NEXT_MEAL, "You're all set!",
                        "You're all set! Waiting for your next meal.",
                        "ALLSET:" + meal.getId(), now));
            }
        }
    }

    private void saveIfAbsent(Notification notification) {
        if (notifications.existsByUserIdAndDedupeKey(notification.getUserId(), notification.getDedupeKey())) {
            return;
        }
        try {
            transactions.executeWithoutResult(status -> notifications.saveAndFlush(notification));
        } catch (DataIntegrityViolationException exception) {
            LOGGER.debug("Notification deduplication race ignored for key {}", notification.getDedupeKey());
        }
    }
}
