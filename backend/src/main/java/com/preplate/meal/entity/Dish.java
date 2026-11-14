package com.preplate.meal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "dishes")
public class Dish {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    @Column(length = 255)
    private String description;
    @Column(nullable = false)
    private int calories;
    @Column(name = "protein_g", nullable = false, precision = 6, scale = 1)
    private BigDecimal proteinG;
    @Column(name = "carbs_g", nullable = false, precision = 6, scale = 1)
    private BigDecimal carbsG;
    @Column(name = "fat_g", nullable = false, precision = 6, scale = 1)
    private BigDecimal fatG;
    @Column(columnDefinition = "TEXT")
    private String ingredients;
    @Column(nullable = false)
    private boolean vegetarian = true;
    @Column(name = "image_file", length = 100)
    private String imageFile;

    protected Dish() {
    }

    public Dish(String name, String description, int calories, BigDecimal proteinG, BigDecimal carbsG,
            BigDecimal fatG, String ingredients, boolean vegetarian, String imageFile) {
        this.name = name;
        this.description = description;
        this.calories = calories;
        this.proteinG = proteinG;
        this.carbsG = carbsG;
        this.fatG = fatG;
        this.ingredients = ingredients;
        this.vegetarian = vegetarian;
        this.imageFile = imageFile;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getCalories() { return calories; }
    public BigDecimal getProteinG() { return proteinG; }
    public BigDecimal getCarbsG() { return carbsG; }
    public BigDecimal getFatG() { return fatG; }
    public String getIngredients() { return ingredients; }
    public boolean isVegetarian() { return vegetarian; }
    public String getImageFile() { return imageFile; }
}
