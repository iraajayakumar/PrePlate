package com.preplate.analytics.repository;

import com.preplate.analytics.entity.ConsumptionRecord;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsumptionRecordRepository extends JpaRepository<ConsumptionRecord, Long> {
    List<ConsumptionRecord> findByMealId(Long mealId);
    Optional<ConsumptionRecord> findByMealIdAndDishId(Long mealId, Long dishId);

    @Query("select r from ConsumptionRecord r join Meal m on m.id = r.mealId where m.mealDate between :from and :to")
    List<ConsumptionRecord> findBetweenMealDates(@Param("from") LocalDate from, @Param("to") LocalDate to);

    List<ConsumptionRecord> findByMealIdIn(Collection<Long> mealIds);
}
