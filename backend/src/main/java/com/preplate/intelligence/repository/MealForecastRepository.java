package com.preplate.intelligence.repository;

import com.preplate.intelligence.entity.MealForecast;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MealForecastRepository extends JpaRepository<MealForecast, Long> {
    Optional<MealForecast> findByMealId(Long mealId);

    @Modifying
    @Query("delete from MealForecast f where f.mealId = :mealId")
    void deleteByMealId(@Param("mealId") Long mealId);
}
