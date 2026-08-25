package com.lose10.training;

import com.lose10.platform.error.ApiException;
import com.lose10.training.dto.LogWorkoutSessionRequest;
import com.lose10.training.dto.WorkoutExerciseResponse;
import com.lose10.training.dto.WorkoutResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Generates and adapts training plans (docs/backlog L10-603). The initial
 * generator is template-based (a fixed 3-day bodyweight split) rather than
 * a fully personalized algorithm — real personalization (fitness level,
 * available equipment/time) is a content/tuning problem layered onto this
 * structure, not an architecture change; the versioning and adaptation
 * mechanics here are what actually needed to be right.
 */
@Service
public class TrainingService {

    private static final List<DayOfWeek> DEFAULT_TRAINING_DAYS = List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
    private static final List<String> DEFAULT_HOME_EXERCISES = List.of(
            "Push-ups", "Bodyweight Squats", "Plank", "Walking Lunges", "Glute Bridges", "Jumping Jacks"
    );

    private final TrainingPlanRepository planRepository;
    private final TrainingPlanVersionRepository versionRepository;
    private final WorkoutRepository workoutRepository;
    private final WorkoutExerciseRepository workoutExerciseRepository;
    private final WorkoutSessionRepository sessionRepository;
    private final ExerciseLibraryRepository exerciseLibraryRepository;

    public TrainingService(
            TrainingPlanRepository planRepository,
            TrainingPlanVersionRepository versionRepository,
            WorkoutRepository workoutRepository,
            WorkoutExerciseRepository workoutExerciseRepository,
            WorkoutSessionRepository sessionRepository,
            ExerciseLibraryRepository exerciseLibraryRepository
    ) {
        this.planRepository = planRepository;
        this.versionRepository = versionRepository;
        this.workoutRepository = workoutRepository;
        this.workoutExerciseRepository = workoutExerciseRepository;
        this.sessionRepository = sessionRepository;
        this.exerciseLibraryRepository = exerciseLibraryRepository;
    }

    @Transactional
    public TrainingPlan generateInitialPlan(UUID userId, UUID goalId, TrainingEnvironment environment) {
        if (planRepository.findByUserIdAndStatus(userId, TrainingPlanStatus.ACTIVE).isPresent()) {
            throw ApiException.businessRule("An active training plan already exists");
        }

        TrainingPlan plan = planRepository.save(new TrainingPlan(userId, goalId));
        TrainingPlanVersion version = versionRepository.save(new TrainingPlanVersion(plan.getId(), 1, environment, "Initial plan"));
        plan.setCurrentVersionId(version.getId());

        Map<String, ExerciseLibrary> library = indexLibraryByName();
        for (DayOfWeek day : DEFAULT_TRAINING_DAYS) {
            Workout workout = workoutRepository.save(new Workout(version.getId(), day, "FULL_BODY"));
            int order = 0;
            for (String exerciseName : DEFAULT_HOME_EXERCISES) {
                ExerciseLibrary exercise = library.get(exerciseName);
                if (exercise == null) {
                    continue;
                }
                workoutExerciseRepository.save(new WorkoutExercise(workout.getId(), exercise.getId(), 3, 12, null, 45, "MODERATE", order++));
            }
        }

        return plan;
    }

    /**
     * Creates a new, lighter version of the plan — e.g. in response to a
     * detected low-adherence behavior pattern (docs/MEMORY.md §10 Day-60
     * example: "restart with 3 short sessions" rather than escalating).
     * Never mutates the existing version; always versions forward.
     */
    @Transactional
    public TrainingPlan reduceLoad(UUID userId, String reason) {
        TrainingPlan plan = requireActivePlan(userId);
        List<TrainingPlanVersion> versions = versionRepository.findByPlanIdOrderByVersionNoDesc(plan.getId());
        TrainingPlanVersion latest = versions.get(0);
        List<Workout> currentWorkouts = workoutRepository.findByPlanVersionId(latest.getId());

        int newVersionNo = latest.getVersionNo() + 1;
        TrainingPlanVersion newVersion = versionRepository.save(new TrainingPlanVersion(plan.getId(), newVersionNo, latest.getEnvironment(), reason));

        // Halve the number of training days (rounded up, minimum 1) and
        // shorten each session by dropping the last third of exercises.
        int reducedDayCount = Math.max(1, (currentWorkouts.size() + 1) / 2);
        for (Workout workout : currentWorkouts.subList(0, Math.min(reducedDayCount, currentWorkouts.size()))) {
            Workout newWorkout = workoutRepository.save(new Workout(newVersion.getId(), workout.getDayOfWeek(), workout.getWorkoutType()));
            List<WorkoutExercise> exercises = workoutExerciseRepository.findByWorkoutIdOrderByOrderIndexAsc(workout.getId());
            int keepCount = Math.max(1, (exercises.size() * 2) / 3);
            for (WorkoutExercise exercise : exercises.subList(0, Math.min(keepCount, exercises.size()))) {
                workoutExerciseRepository.save(new WorkoutExercise(
                        newWorkout.getId(), exercise.getExerciseId(), exercise.getSets(), exercise.getReps(),
                        exercise.getDurationSec(), 45, "LIGHT", exercise.getOrderIndex()
                ));
            }
        }

        plan.setCurrentVersionId(newVersion.getId());
        return plan;
    }

    @Transactional(readOnly = true)
    public Optional<WorkoutResponse> todayWorkout(UUID userId, DayOfWeek today) {
        TrainingPlan plan = requireActivePlan(userId);
        return workoutRepository.findByPlanVersionIdAndDayOfWeek(plan.getCurrentVersionId(), today.name())
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<WorkoutResponse> currentPlanWorkouts(UUID userId) {
        TrainingPlan plan = requireActivePlan(userId);
        return workoutRepository.findByPlanVersionId(plan.getCurrentVersionId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public WorkoutSession logSession(UUID userId, LogWorkoutSessionRequest request) {
        return sessionRepository.save(new WorkoutSession(userId, request.workoutId(), Instant.now(), request.perceivedEffort(), request.notes()));
    }

    @Transactional(readOnly = true)
    public List<WorkoutSession> recentSessions(UUID userId) {
        return sessionRepository.findTop20ByUserIdOrderByCompletedAtDesc(userId);
    }

    TrainingPlan requireActivePlan(UUID userId) {
        return planRepository.findByUserIdAndStatus(userId, TrainingPlanStatus.ACTIVE)
                .orElseThrow(() -> ApiException.notFound("No active training plan"));
    }

    private WorkoutResponse toResponse(Workout workout) {
        Map<UUID, ExerciseLibrary> byId = exerciseLibraryRepository.findAllById(
                workoutExerciseRepository.findByWorkoutIdOrderByOrderIndexAsc(workout.getId()).stream()
                        .map(WorkoutExercise::getExerciseId).toList()
        ).stream().collect(java.util.stream.Collectors.toMap(ExerciseLibrary::getId, e -> e));

        List<WorkoutExerciseResponse> exercises = workoutExerciseRepository.findByWorkoutIdOrderByOrderIndexAsc(workout.getId()).stream()
                .map(we -> new WorkoutExerciseResponse(
                        Optional.ofNullable(byId.get(we.getExerciseId())).map(ExerciseLibrary::getName).orElse("Unknown"),
                        we.getSets(), we.getReps(), we.getDurationSec(), null
                ))
                .toList();

        return new WorkoutResponse(workout.getId(), workout.getDayOfWeek(), workout.getWorkoutType(), exercises);
    }

    private Map<String, ExerciseLibrary> indexLibraryByName() {
        return exerciseLibraryRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(ExerciseLibrary::getName, e -> e, (a, b) -> a));
    }
}
