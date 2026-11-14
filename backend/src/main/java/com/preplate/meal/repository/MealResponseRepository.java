package com.preplate.meal.repository;

import com.preplate.common.enums.AttendanceStatus;
import com.preplate.meal.entity.MealResponse;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealResponseRepository extends JpaRepository<MealResponse, Long> {
    Optional<MealResponse> findByUserIdAndMealId(Long userId, Long mealId);
    List<MealResponse> findByUserIdAndMealIdIn(Long userId, Collection<Long> mealIds);
    List<MealResponse> findByMealIdIn(Collection<Long> mealIds);
    List<MealResponse> findByMealId(Long mealId);
    long countByMealIdAndStatus(Long mealId, AttendanceStatus status);
    long countByMealId(Long mealId);
    long countByMealIdAndStatusAndAttendedTrue(Long mealId, AttendanceStatus status);
}
