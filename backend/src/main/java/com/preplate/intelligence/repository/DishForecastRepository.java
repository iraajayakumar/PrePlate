package com.preplate.intelligence.repository;

import com.preplate.intelligence.entity.DishForecast;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DishForecastRepository extends JpaRepository<DishForecast, Long> {
    List<DishForecast> findByMealId(Long mealId);
    List<DishForecast> findByMealIdIn(Iterable<Long> mealIds);
    java.util.Optional<DishForecast> findByMealIdAndDishId(Long mealId, Long dishId);
    @Modifying
    @Query("delete from DishForecast f where f.mealId = :mealId")
    void deleteByMealId(@Param("mealId") Long mealId);
}
