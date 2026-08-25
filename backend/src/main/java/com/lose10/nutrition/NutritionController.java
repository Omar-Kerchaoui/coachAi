package com.lose10.nutrition;

import com.lose10.nutrition.dto.LogMealRequest;
import com.lose10.nutrition.dto.MealItemResponse;
import com.lose10.nutrition.dto.UpdateNutritionProfileRequest;
import com.lose10.platform.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/nutrition")
public class NutritionController {

    private final NutritionService nutritionService;

    public NutritionController(NutritionService nutritionService) {
        this.nutritionService = nutritionService;
    }

    @GetMapping("/profile")
    public ResponseEntity<NutritionProfile> profile() {
        return ResponseEntity.ok(nutritionService.getOrCreateProfile(CurrentUser.id()));
    }

    @PatchMapping("/profile")
    public ResponseEntity<NutritionProfile> updateProfile(@RequestBody UpdateNutritionProfileRequest request) {
        return ResponseEntity.ok(nutritionService.updateProfile(CurrentUser.id(), request));
    }

    @PostMapping("/logs/{date}/meals")
    public ResponseEntity<MealItemResponse> logMeal(@PathVariable LocalDate date, @Valid @RequestBody LogMealRequest request) {
        return ResponseEntity.status(201).body(nutritionService.logMeal(CurrentUser.id(), date, request));
    }

    @GetMapping("/logs/{date}/meals")
    public ResponseEntity<List<MealItemResponse>> mealsForDate(@PathVariable LocalDate date) {
        return ResponseEntity.ok(nutritionService.mealsForDate(CurrentUser.id(), date));
    }
}
