package com.preplate.support;

import com.preplate.common.enums.MealType;
import com.preplate.common.enums.Role;
import com.preplate.meal.entity.Dish;
import com.preplate.meal.entity.Meal;
import com.preplate.meal.entity.MealSchedule;
import com.preplate.meal.repository.DishRepository;
import com.preplate.meal.repository.MealRepository;
import com.preplate.meal.repository.MealScheduleRepository;
import com.preplate.user.entity.User;
import com.preplate.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class TestDataFactory {
    private final JdbcTemplate jdbc;
    private final MealScheduleRepository schedules;
    private final DishRepository dishes;
    private final MealRepository meals;
    private final UserRepository users;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public TestDataFactory(JdbcTemplate jdbc, MealScheduleRepository schedules, DishRepository dishes,
            MealRepository meals, UserRepository users) {
        this.jdbc = jdbc;
        this.schedules = schedules;
        this.dishes = dishes;
        this.meals = meals;
        this.users = users;
    }

    public Fixture resetAndCreate() {
        jdbc.update("DELETE FROM consumption_records");
        jdbc.update("DELETE FROM dish_forecasts");
        jdbc.update("DELETE FROM meal_forecasts");
        jdbc.update("DELETE FROM notifications");
        jdbc.update("DELETE FROM dish_preferences");
        jdbc.update("DELETE FROM meal_responses");
        jdbc.update("DELETE FROM meal_dishes");
        jdbc.update("DELETE FROM meals");
        jdbc.update("DELETE FROM sustainability_facts");
        jdbc.update("DELETE FROM dishes");
        jdbc.update("DELETE FROM meal_schedules");
        jdbc.update("DELETE FROM users");

        schedules.saveAll(List.of(
                new MealSchedule(MealType.BREAKFAST, LocalTime.of(7, 30), LocalTime.of(9, 30), LocalTime.of(18, 0), -1),
                new MealSchedule(MealType.LUNCH, LocalTime.of(12, 30), LocalTime.of(14, 30), LocalTime.of(22, 0), -1),
                new MealSchedule(MealType.SNACKS, LocalTime.of(16, 30), LocalTime.of(18, 0), LocalTime.of(10, 0), 0),
                new MealSchedule(MealType.DINNER, LocalTime.of(19, 30), LocalTime.of(21, 30), LocalTime.of(14, 0), 0)));
        List<Dish> dishList = new ArrayList<>();
        for (int index = 1; index <= 8; index++) {
            dishList.add(new Dish("Test Dish " + index, "Test menu dish", 100 + index,
                    BigDecimal.valueOf(5), BigDecimal.valueOf(12), BigDecimal.valueOf(2),
                    "Rice, salt", true, null));
        }
        dishList = dishes.saveAllAndFlush(dishList);
        User student = users.saveAndFlush(new User("TEST001", "Test Student",
                passwordEncoder.encode("student123"), Role.STUDENT, "CSE", "Kaveri", "101"));
        User admin = users.saveAndFlush(new User("ADMIN001", "Test Admin",
                passwordEncoder.encode("admin123"), Role.ADMIN, null, null, null));

        LocalDate today = LocalDate.of(2026, 11, 14);
        List<Meal> mealList = new ArrayList<>();
        mealList.add(new Meal(today, MealType.DINNER, new HashSet<>(dishList.subList(0, 2))));
        for (MealType type : MealType.values()) {
            mealList.add(new Meal(today.plusDays(1), type, new HashSet<>(dishList.subList(2, 4))));
        }
        mealList = meals.saveAllAndFlush(mealList);
        return new Fixture(student, admin, dishList, mealList);
    }

    public record Fixture(User student, User admin, List<Dish> dishes, List<Meal> meals) {
        public Meal dinner() {
            return meals.stream().filter(meal -> meal.getMealType() == MealType.DINNER
                    && meal.getMealDate().equals(LocalDate.of(2026, 11, 14))).findFirst().orElseThrow();
        }
    }
}
