package com.lose10.goal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GoalEventRepository extends JpaRepository<GoalEvent, UUID> {
}
