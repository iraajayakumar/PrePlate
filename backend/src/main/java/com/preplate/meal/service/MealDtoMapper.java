package com.preplate.meal.service;

import static com.preplate.common.dto.ApiDtos.DishResponse;

import com.preplate.meal.entity.Dish;
import java.util.Arrays;
import java.util.List;

public final class MealDtoMapper {
    private MealDtoMapper() {
    }

    public static DishResponse dish(Dish dish) {
        List<String> ingredients = dish.getIngredients() == null || dish.getIngredients().isBlank()
                ? List.of()
                : Arrays.stream(dish.getIngredients().split(","))
                        .map(String::trim).filter(value -> !value.isEmpty()).toList();
        return new DishResponse(dish.getId(), dish.getName(), dish.getDescription(), dish.getCalories(),
                dish.getProteinG().doubleValue(), dish.getCarbsG().doubleValue(), dish.getFatG().doubleValue(),
                ingredients, dish.isVegetarian(), dish.getImageFile());
    }
}
