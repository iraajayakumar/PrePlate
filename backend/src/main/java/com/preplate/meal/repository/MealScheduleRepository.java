package com.preplate.meal.repository;

import com.preplate.common.enums.MealType;
import com.preplate.meal.entity.MealSchedule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealScheduleRepository extends JpaRepository<MealSchedule, MealType> {
    List<MealSchedule> findAllByOrderByMealTypeAsc();
}
