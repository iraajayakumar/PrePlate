package com.preplate.meal.repository;

import com.preplate.common.enums.MealType;
import com.preplate.meal.entity.Meal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealRepository extends JpaRepository<Meal, Long> {
    boolean existsByMealDateAndMealType(LocalDate mealDate, MealType mealType);
    Optional<Meal> findByMealDateAndMealType(LocalDate mealDate, MealType mealType);

    @EntityGraph(attributePaths = "dishes")
    List<Meal> findByMealDateBetweenOrderByMealDateAsc(LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = "dishes")
    List<Meal> findByMealDateBetween(LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = "dishes")
    Optional<Meal> findWithDishesById(Long id);
}
