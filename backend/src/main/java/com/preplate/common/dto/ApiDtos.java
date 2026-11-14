package com.preplate.common.dto;

import com.preplate.common.enums.AttendanceStatus;
import com.preplate.common.enums.ForecastSource;
import com.preplate.common.enums.MealType;
import com.preplate.common.enums.NotificationType;
import com.preplate.common.enums.Role;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class ApiDtos {
    private ApiDtos() {
    }

    public record UserResponse(Long id, String registrationNumber, String fullName, Role role,
            String department, String hostelName, String hostelRoom) {
    }

    public record LoginRequest(@NotBlank String registrationNumber, @NotBlank String password) {
    }

    public record RegisterRequest(@NotBlank String registrationNumber, @NotBlank String fullName,
            @NotBlank String department, @NotBlank String hostelName, @NotBlank String hostelRoom,
            @NotBlank @Size(min = 6) String password) {
    }

    public record AuthResponse(String token, UserResponse user) {
    }

    public record MessageResponse(String message) {
    }

    public record DishResponse(Long id, String name, String description, int calories, double proteinG,
            double carbsG, double fatG, List<String> ingredients, boolean vegetarian, String imageFile) {
    }

    public record MealScheduleResponse(MealType mealType, LocalTime servingStart, LocalTime servingEnd,
            LocalTime deadlineTime, int deadlineDayOffset, String deadlineLabel) {
    }

    public record FactResponse(Long id, String factText, String sourceNote) {
    }

    public record MealResponseStatus(AttendanceStatus status, Instant respondedAt) {
    }

    public record UpcomingMealResponse(Long mealId, MealType mealType, LocalDate mealDate, Instant deadlineAt,
            MealResponseStatus response, List<Long> selectedDishIds, List<DishResponse> dishes) {
    }

    public record UpcomingMealsResponse(Instant serverNow, List<UpcomingMealResponse> meals) {
    }

    public record SetResponseRequest(@NotNull AttendanceStatus status) {
    }

    public record SetResponseResult(Long mealId, AttendanceStatus status, Instant respondedAt) {
    }

    public record DishPreferencesRequest(@NotNull List<Long> dishIds) {
    }

    public record DishPreferencesResponse(Long mealId, List<Long> dishIds) {
    }

    public record HistoryEntry(Long mealId, LocalDate mealDate, AttendanceStatus status, List<String> dishNames) {
    }

    public record HistoryCategory(MealType mealType, List<HistoryEntry> entries) {
    }

    public record HistoryResponse(String period, List<HistoryCategory> categories) {
    }

    public record Participation(MealType mealType, long available, long selected, long attended) {
    }

    public record TopDish(String dishName, long timesSelected) {
    }

    public record StudentAnalyticsResponse(String scope, List<Participation> participation, List<TopDish> topDishes) {
    }

    public record NotificationItem(Long id, NotificationType type, String title, String message, boolean read,
            Instant createdAt) {
    }

    public record NotificationsResponse(long unreadCount, List<NotificationItem> notifications) {
    }

    public record UpdatedResponse(int updated) {
    }

    public record CreateDishRequest(@NotBlank String name, String description, @Min(0) int calories,
            @DecimalMin("0.0") double proteinG, @DecimalMin("0.0") double carbsG,
            @DecimalMin("0.0") double fatG, List<String> ingredients, boolean vegetarian) {
    }

    public record CreateMealRequest(@NotNull LocalDate mealDate, @NotNull MealType mealType,
            @NotNull @NotEmpty List<Long> dishIds) {
    }

    public record UpdateMealRequest(@NotNull List<Long> dishIds) {
    }

    public record AdminMealResponse(Long mealId, LocalDate mealDate, MealType mealType, Instant deadlineAt,
            List<DishResponse> dishes) {
    }

    public record ForecastFactor(String name, double importance) {
    }

    public record ForecastResponse(int predictedAttendance, int lowerBound, int upperBound, double confidence,
            int recommendedPreparation, ForecastSource source, Instant generatedAt, List<ForecastFactor> factors) {
    }

    public record DishForecastResponse(Long dishId, String dishName, int studentInterest, int predictedDemand,
            int recommendedPreparation) {
    }

    public record DashboardResponse(boolean mealExists, Long mealId, LocalDate mealDate, MealType mealType,
            long eligibleStudents, long confirmedAttendance, long notAttending, long uncertain, long noResponse,
            ForecastResponse forecast, List<DishForecastResponse> dishes) {
    }

    public record ConsumptionInput(@NotNull Long dishId, @Min(0) int preparedQuantity,
            @Min(0) int consumedQuantity) {
    }

    public record SaveConsumptionRequest(@NotNull Long mealId, @NotNull @NotEmpty List<ConsumptionInput> records) {
    }

    public record ConsumptionRecordResponse(Long dishId, String dishName, Integer recommendedQuantity,
            Integer preparedQuantity, Integer consumedQuantity) {
    }

    public record ConsumptionResponse(Long mealId, List<ConsumptionRecordResponse> records) {
    }

    public record WasteTotals(double prepared, double consumed, double leftover, double wastePercentage,
            double forecastAccuracy) {
    }

    public record DailyWaste(LocalDate date, double prepared, double consumed, double leftover, double wastePercentage) {
    }

    public record MealTypeWaste(MealType mealType, double wastePercentage) {
    }

    public record DishWaste(String dishName, double wastePercentage) {
    }

    public record WasteAnalyticsResponse(WasteTotals totals, List<DailyWaste> dailyTrend,
            List<MealTypeWaste> byMealType, List<DishWaste> byDish) {
    }
}
