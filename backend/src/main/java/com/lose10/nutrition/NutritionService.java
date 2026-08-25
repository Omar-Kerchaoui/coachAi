package com.lose10.nutrition;

import com.lose10.nutrition.dto.LogMealRequest;
import com.lose10.nutrition.dto.MealEstimate;
import com.lose10.nutrition.dto.MealItemResponse;
import com.lose10.nutrition.dto.UpdateNutritionProfileRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class NutritionService {

    private final NutritionProfileRepository profileRepository;
    private final NutritionLogRepository logRepository;
    private final MealRepository mealRepository;
    private final MealItemRepository mealItemRepository;
    private final MealCalorieEstimator calorieEstimator;

    public NutritionService(
            NutritionProfileRepository profileRepository,
            NutritionLogRepository logRepository,
            MealRepository mealRepository,
            MealItemRepository mealItemRepository,
            MealCalorieEstimator calorieEstimator
    ) {
        this.profileRepository = profileRepository;
        this.logRepository = logRepository;
        this.mealRepository = mealRepository;
        this.mealItemRepository = mealItemRepository;
        this.calorieEstimator = calorieEstimator;
    }

    @Transactional(readOnly = true)
    public NutritionProfile getOrCreateProfile(UUID userId) {
        return profileRepository.findByUserId(userId).orElseGet(() -> profileRepository.save(new NutritionProfile(userId)));
    }

    @Transactional
    public NutritionProfile updateProfile(UUID userId, UpdateNutritionProfileRequest request) {
        NutritionProfile profile = getOrCreateProfile(userId);
        profile.update(request.dietaryRestrictions(), request.dislikedFoods(), request.targetCalories(), request.targetProteinG());
        return profile;
    }

    @Transactional
    public MealItemResponse logMeal(UUID userId, LocalDate date, LogMealRequest request) {
        NutritionLog log = logRepository.findByUserIdAndLogDate(userId, date)
                .orElseGet(() -> logRepository.save(new NutritionLog(userId, date)));

        String mealType = request.mealType() != null ? request.mealType() : "SNACK";
        Meal meal = mealRepository.save(new Meal(log.getId(), Instant.now(), mealType));

        MealEstimate estimate = calorieEstimator.estimate(userId, request.description());
        MealItem item = mealItemRepository.save(new MealItem(meal.getId(), request.description(), estimate.calories(), estimate.proteinG(), "AI_ESTIMATE"));

        return MealItemResponse.from(item);
    }

    @Transactional(readOnly = true)
    public List<MealItemResponse> mealsForDate(UUID userId, LocalDate date) {
        return logRepository.findByUserIdAndLogDate(userId, date)
                .map(log -> mealRepository.findByNutritionLogId(log.getId()).stream()
                        .flatMap(meal -> mealItemRepository.findByMealId(meal.getId()).stream())
                        .map(MealItemResponse::from)
                        .toList())
                .orElse(List.of());
    }
}
