package com.lose10.training;

import com.lose10.platform.security.CurrentUser;
import com.lose10.training.dto.GeneratePlanRequest;
import com.lose10.training.dto.LogWorkoutSessionRequest;
import com.lose10.training.dto.WorkoutResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/training")
public class TrainingController {

    private final TrainingService trainingService;

    public TrainingController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    @PostMapping("/plan")
    public ResponseEntity<List<WorkoutResponse>> generatePlan(@Valid @RequestBody GeneratePlanRequest request) {
        TrainingPlan plan = trainingService.generateInitialPlan(CurrentUser.id(), request.goalId(), request.environment());
        return ResponseEntity.status(201).body(trainingService.currentPlanWorkouts(plan.getUserId()));
    }

    @GetMapping("/plan")
    public ResponseEntity<List<WorkoutResponse>> currentPlan() {
        return ResponseEntity.ok(trainingService.currentPlanWorkouts(CurrentUser.id()));
    }

    @GetMapping("/plan/today")
    public ResponseEntity<Optional<WorkoutResponse>> todayWorkout() {
        return ResponseEntity.ok(trainingService.todayWorkout(CurrentUser.id(), LocalDate.now().getDayOfWeek()));
    }

    @PostMapping("/sessions")
    public ResponseEntity<Void> logSession(@Valid @RequestBody LogWorkoutSessionRequest request) {
        trainingService.logSession(CurrentUser.id(), request);
        return ResponseEntity.status(201).build();
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<WorkoutSession>> sessions() {
        return ResponseEntity.ok(trainingService.recentSessions(CurrentUser.id()));
    }
}
