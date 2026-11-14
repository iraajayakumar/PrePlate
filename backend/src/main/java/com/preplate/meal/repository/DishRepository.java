package com.preplate.meal.repository;

import com.preplate.meal.entity.Dish;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DishRepository extends JpaRepository<Dish, Long> {
    boolean existsByNameIgnoreCase(String name);
    List<Dish> findAllByOrderByNameAsc();
    List<Dish> findByNameIn(Collection<String> names);
}
