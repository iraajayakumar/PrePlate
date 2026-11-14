package com.preplate.meal.repository;

import com.preplate.meal.entity.DishPreference;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DishPreferenceRepository extends JpaRepository<DishPreference, Long> {
    List<DishPreference> findByUserIdAndMealId(Long userId, Long mealId);
    List<DishPreference> findByMealIdIn(Collection<Long> mealIds);
    List<DishPreference> findByUserIdAndMealIdIn(Long userId, Collection<Long> mealIds);
    long countByMealIdAndDishId(Long mealId, Long dishId);

    @Modifying
    @Query("delete from DishPreference p where p.userId = :userId and p.mealId = :mealId")
    void deleteForUserMeal(@Param("userId") Long userId, @Param("mealId") Long mealId);

    @Modifying
    @Query("delete from DishPreference p where p.mealId = :mealId")
    void deleteByMealId(@Param("mealId") Long mealId);
}
