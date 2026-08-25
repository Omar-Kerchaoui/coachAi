package com.lose10.coaching;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CoachingEventRepository extends JpaRepository<CoachingEvent, UUID> {
}
