package com.preplate.seed;

import com.preplate.analytics.entity.ConsumptionRecord;
import com.preplate.analytics.repository.ConsumptionRecordRepository;
import com.preplate.common.enums.AttendanceStatus;
import com.preplate.common.enums.MealType;
import com.preplate.common.enums.Role;
import com.preplate.intelligence.entity.DishForecast;
import com.preplate.intelligence.service.ForecastService;
import com.preplate.meal.entity.Dish;
import com.preplate.meal.entity.DishPreference;
import com.preplate.meal.entity.Meal;
import com.preplate.meal.entity.MealResponse;
import com.preplate.meal.entity.MealSchedule;
import com.preplate.meal.repository.DishPreferenceRepository;
import com.preplate.meal.repository.DishRepository;
import com.preplate.meal.repository.MealRepository;
import com.preplate.meal.repository.MealResponseRepository;
import com.preplate.meal.repository.MealScheduleRepository;
import com.preplate.user.entity.User;
import com.preplate.user.repository.UserRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!test")
@ConditionalOnProperty(name = "preplate.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final List<String> FIRST_NAMES = List.of("Aarav", "Aadhya", "Aditya", "Ananya", "Arjun", "Diya",
            "Ishaan", "Kavya", "Meera", "Rohan", "Saanvi", "Vikram");
    private static final List<String> LAST_NAMES = List.of("Mehta", "Iyer", "Reddy", "Sharma", "Nair", "Patel",
            "Rao", "Menon", "Kapoor", "Das", "Kumar", "Singh");
    private static final List<String> DEPARTMENTS = List.of("CSE", "ECE", "Mechanical", "Civil", "Biotech");
    private static final List<String> HOSTELS = List.of("Kaveri", "Ganga", "Narmada", "Yamuna");
    private static final Map<MealType, List<List<String>>> MENUS = menus();

    private final DishRepository dishes;
    private final MealScheduleRepository schedules;
    private final MealRepository meals;
    private final UserRepository users;
    private final MealResponseRepository responses;
    private final DishPreferenceRepository preferences;
    private final ConsumptionRecordRepository consumption;
    private final ForecastService forecasts;
    private final Clock clock;
    private final int studentCount;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DemoDataSeeder(DishRepository dishes, MealScheduleRepository schedules, MealRepository meals,
            UserRepository users, MealResponseRepository responses, DishPreferenceRepository preferences,
            ConsumptionRecordRepository consumption, ForecastService forecasts, Clock clock,
            @Value("${preplate.seed.student-count:60}") int studentCount) {
        this.dishes = dishes;
        this.schedules = schedules;
        this.meals = meals;
        this.users = users;
        this.responses = responses;
        this.preferences = preferences;
        this.consumption = consumption;
        this.forecasts = forecasts;
        this.clock = clock;
        this.studentCount = studentCount;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        List<Dish> dishList = dishes.findAllByOrderByNameAsc();
        if (dishList.isEmpty()) {
            LOGGER.warn("Run database/seed.sql first");
            return;
        }
        if (schedules.count() == 0) {
            LOGGER.warn("Run database/seed.sql first");
            return;
        }
        if (users.count() == 0) {
            seedUsers();
        }
        List<User> students = users.findAllByRole(Role.STUDENT);
        Map<String, Dish> dishByName = new HashMap<>();
        dishList.forEach(dish -> dishByName.put(dish.getName(), dish));
        seedMeals(dishByName);
        if (responses.count() == 0 && !students.isEmpty()) {
            seedResponsesAndConsumption(students);
        }
        LOGGER.info("PrePlate demo data ready: {} users, {} meals, {} responses, {} consumption records",
                users.count(), meals.count(), responses.count(), consumption.count());
    }

    private void seedUsers() {
        users.save(new User("ADMIN001", "Kitchen Admin", passwordEncoder.encode("admin123"),
                Role.ADMIN, null, null, null));
        List<User> studentList = new ArrayList<>();
        for (int index = 0; index < studentCount; index++) {
            String first = FIRST_NAMES.get(index % FIRST_NAMES.size());
            String last = LAST_NAMES.get((index / FIRST_NAMES.size() + index) % LAST_NAMES.size());
            String registration = "RA211100301" + String.format("%04d", index + 1);
            studentList.add(new User(registration, first + " " + last, passwordEncoder.encode("student123"),
                    Role.STUDENT, DEPARTMENTS.get(index % DEPARTMENTS.size()),
                    HOSTELS.get(index % HOSTELS.size()), String.valueOf(101 + index % 420)));
        }
        users.saveAll(studentList);
        users.flush();
    }

    private void seedMeals(Map<String, Dish> dishByName) {
        var today = java.time.LocalDate.now(clock);
        List<Meal> missing = new ArrayList<>();
        for (var date = today.minusDays(14); !date.isAfter(today.plusDays(3)); date = date.plusDays(1)) {
            for (MealType type : MealType.values()) {
                if (!meals.existsByMealDateAndMealType(date, type)) {
                    List<String> menu = MENUS.get(type).get(Math.floorMod(date.getDayOfYear(), MENUS.get(type).size()));
                    Set<Dish> selectedDishes = new HashSet<>();
                    for (String name : menu) {
                        Dish dish = dishByName.get(name);
                        if (dish != null) {
                            selectedDishes.add(dish);
                        }
                    }
                    missing.add(new Meal(date, type, selectedDishes));
                }
            }
        }
        meals.saveAll(missing);
        meals.flush();
    }

    private void seedResponsesAndConsumption(List<User> students) {
        Random random = new Random(42);
        var today = java.time.LocalDate.now(clock);
        List<Meal> allMeals = meals.findByMealDateBetween(today.minusDays(14), today.plusDays(3));
        List<MealResponse> responseRows = new ArrayList<>();
        List<DishPreference> preferenceRows = new ArrayList<>();
        User demoStudent = users.findByRegistrationNumber("RA2111003010001").orElse(null);
        for (Meal meal : allMeals) {
            boolean past = meal.getMealDate().isBefore(today);
            for (User student : students) {
                if (!past && demoStudent != null && student.getId().equals(demoStudent.getId())) {
                    continue;
                }
                double chance = random.nextDouble();
                if (chance >= 0.85) {
                    continue;
                }
                AttendanceStatus status = chance < 0.60 ? AttendanceStatus.ATTENDING
                        : chance < 0.75 ? AttendanceStatus.NOT_ATTENDING : AttendanceStatus.NOT_SURE;
                MealResponse response = new MealResponse(student.getId(), meal.getId(), status, clock.instant());
                if (past && status == AttendanceStatus.ATTENDING) {
                    response.setAttended(random.nextDouble() < 0.90);
                    if (random.nextDouble() < 0.70 && !meal.getDishes().isEmpty()) {
                        List<Dish> menu = new ArrayList<>(meal.getDishes());
                        java.util.Collections.shuffle(menu, random);
                        int pickCount = Math.min(menu.size(), 2 + random.nextInt(3));
                        for (Dish dish : menu.subList(0, pickCount)) {
                            preferenceRows.add(new DishPreference(student.getId(), meal.getId(), dish.getId()));
                        }
                    }
                }
                responseRows.add(response);
            }
        }
        responses.saveAll(responseRows);
        responses.flush();
        preferences.saveAll(preferenceRows);
        preferences.flush();

        List<ConsumptionRecord> consumptionRows = new ArrayList<>();
        List<Meal> pastMeals = allMeals.stream().filter(meal -> meal.getMealDate().isBefore(today))
                .sorted(java.util.Comparator.comparing(Meal::getMealDate).thenComparing(Meal::getMealType)).toList();
        for (Meal meal : pastMeals) {
            forecasts.generate(meal.getId());
            Map<Long, DishForecast> forecastByDish = new HashMap<>();
            forecasts.dishForecasts(meal.getId()).forEach(value -> forecastByDish.put(value.getDishId(), value));
            for (Dish dish : meal.getDishes()) {
                DishForecast forecast = forecastByDish.get(dish.getId());
                if (forecast == null) {
                    continue;
                }
                int prepared = (int) Math.round(forecast.getRecommendedPreparation() * (1 + random.nextDouble() * 0.05));
                int consumedQuantity = Math.min(prepared,
                        (int) Math.round(forecast.getPredictedDemand() * (0.80 + random.nextDouble() * 0.20)));
                consumptionRows.add(new ConsumptionRecord(meal.getId(), dish.getId(),
                        forecast.getRecommendedPreparation(), prepared, consumedQuantity, clock.instant()));
            }
        }
        consumption.saveAll(consumptionRows);
        consumption.flush();
    }

    private static Map<MealType, List<List<String>>> menus() {
        return Map.of(
                MealType.BREAKFAST, List.of(
                        List.of("Idli", "Medu Vada", "Sambar", "Coconut Chutney"),
                        List.of("Masala Dosa", "Sambar", "Coconut Chutney"),
                        List.of("Ven Pongal", "Medu Vada", "Sambar", "Coconut Chutney"),
                        List.of("Upma", "Coconut Chutney", "Boiled Egg"),
                        List.of("Poha", "Boiled Egg", "Masala Tea")),
                MealType.LUNCH, List.of(
                        List.of("Steamed Rice", "Dal Tadka", "Paneer Butter Masala", "Aloo Gobi", "Chapati", "Curd"),
                        List.of("Veg Pulao", "Dal Fry", "Mixed Veg Curry", "Papad", "Curd"),
                        List.of("Jeera Rice", "Rajma Masala", "Beans Poriyal", "Chapati", "Curd"),
                        List.of("Steamed Rice", "Sambar", "Rasam", "Egg Curry", "Beans Poriyal", "Papad")),
                MealType.SNACKS, List.of(
                        List.of("Samosa", "Masala Tea"), List.of("Veg Puff", "Masala Tea"),
                        List.of("Sundal", "Masala Tea"), List.of("Bhel Puri", "Masala Tea"),
                        List.of("Bread Pakoda", "Masala Tea")),
                MealType.DINNER, List.of(
                        List.of("Chapati", "Paneer Bhurji", "Dal Tadka", "Curd"),
                        List.of("Veg Fried Rice", "Gobi Manchurian"),
                        List.of("Steamed Rice", "Chicken Curry", "Dal Fry", "Papad"),
                        List.of("Chapati", "Rajma Masala", "Jeera Rice", "Fruit Salad"),
                        List.of("Poori Masala", "Aloo Gobi", "Kheer")));
    }
}
