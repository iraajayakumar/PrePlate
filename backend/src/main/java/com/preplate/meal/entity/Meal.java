package com.preplate.meal.entity;

import com.preplate.common.enums.MealType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "meals", uniqueConstraints = @UniqueConstraint(name = "uk_meals_date_type",
        columnNames = {"meal_date", "meal_type"}))
public class Meal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "meal_date", nullable = false)
    private LocalDate mealDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false, length = 20)
    private MealType mealType;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "meal_dishes",
            joinColumns = @JoinColumn(name = "meal_id"),
            inverseJoinColumns = @JoinColumn(name = "dish_id"))
    private Set<Dish> dishes = new LinkedHashSet<>();

    protected Meal() {
    }

    public Meal(LocalDate mealDate, MealType mealType, Set<Dish> dishes) {
        this.mealDate = mealDate;
        this.mealType = mealType;
        this.dishes = new LinkedHashSet<>(dishes);
    }

    public Long getId() { return id; }
    public LocalDate getMealDate() { return mealDate; }
    public MealType getMealType() { return mealType; }
    public Set<Dish> getDishes() { return dishes; }
    public void setDishes(Set<Dish> dishes) { this.dishes = new LinkedHashSet<>(dishes); }
}
