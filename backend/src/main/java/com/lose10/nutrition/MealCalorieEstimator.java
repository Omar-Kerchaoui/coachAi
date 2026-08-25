package com.lose10.nutrition;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lose10.ai.AIProvider;
import com.lose10.ai.AIProviderException;
import com.lose10.ai.dto.AIRequest;
import com.lose10.ai.dto.AIResponse;
import com.lose10.ai.dto.Message;
import com.lose10.nutrition.dto.MealEstimate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI-based calorie/protein estimation from free-text meal descriptions —
 * no food database required for v1 (docs/MEMORY.md nutrition spec §26).
 * Falls back to a zeroed estimate (never blocks logging) if the AI call
 * fails; the raw text is always saved regardless.
 */
@Component
public class MealCalorieEstimator {

    private static final Logger log = LoggerFactory.getLogger(MealCalorieEstimator.class);

    private static final String SYSTEM_PROMPT = """
            Estimate the calories and protein in the described meal. Respond with ONLY a JSON object,
            no other text: {"calories": 450, "protein_g": 25}
            Use your best reasonable estimate for a typical serving if quantities aren't specified.
            """;

    private static final Pattern JSON_PATTERN = Pattern.compile("\\{.*}", Pattern.DOTALL);

    private final AIProvider aiProvider;
    private final ObjectMapper objectMapper;

    public MealCalorieEstimator(AIProvider aiProvider, ObjectMapper objectMapper) {
        this.aiProvider = aiProvider;
        this.objectMapper = objectMapper;
    }

    public MealEstimate estimate(String mealDescription) {
        try {
            AIResponse response = aiProvider.generate(new AIRequest(
                    SYSTEM_PROMPT, List.of(Message.user(mealDescription)), List.of(), 100, AIRequest.AIRequestPurpose.COACHING_TURN
            ));
            return parse(response.text());
        } catch (AIProviderException e) {
            log.warn("Meal calorie estimation failed, saving with zeroed estimate", e);
            return MealEstimate.UNKNOWN;
        }
    }

    private MealEstimate parse(String rawText) {
        Matcher matcher = JSON_PATTERN.matcher(rawText);
        if (!matcher.find()) {
            return MealEstimate.UNKNOWN;
        }
        try {
            var node = objectMapper.readTree(matcher.group());
            return new MealEstimate(node.path("calories").asInt(0), node.path("protein_g").asInt(0));
        } catch (Exception e) {
            return MealEstimate.UNKNOWN;
        }
    }
}
