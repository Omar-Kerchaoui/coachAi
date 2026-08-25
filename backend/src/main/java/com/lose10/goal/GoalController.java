package com.lose10.goal;

import com.lose10.goal.dto.CreateGoalRequest;
import com.lose10.goal.dto.GoalMilestoneResponse;
import com.lose10.goal.dto.GoalResponse;
import com.lose10.goal.dto.UpdateGoalRequest;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @GetMapping
    public ResponseEntity<List<GoalResponse>> list() {
        return ResponseEntity.ok(goalService.listGoals(CurrentUser.id()));
    }

    @PostMapping
    public ResponseEntity<GoalResponse> create(@Valid @RequestBody CreateGoalRequest request) {
        return ResponseEntity.status(201).body(goalService.createGoal(CurrentUser.id(), request));
    }

    @PatchMapping("/{goalId}")
    public ResponseEntity<GoalResponse> update(@PathVariable UUID goalId, @Valid @RequestBody UpdateGoalRequest request) {
        return ResponseEntity.ok(goalService.updateGoal(CurrentUser.id(), goalId, request));
    }

    @GetMapping("/{goalId}/milestones")
    public ResponseEntity<List<GoalMilestoneResponse>> milestones(@PathVariable UUID goalId) {
        List<GoalMilestoneResponse> response = goalService.listMilestones(CurrentUser.id(), goalId).stream()
                .map(GoalMilestoneResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }
}
