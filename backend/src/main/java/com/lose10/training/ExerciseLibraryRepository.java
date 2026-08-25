package com.lose10.training;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExerciseLibraryRepository extends JpaRepository<ExerciseLibrary, UUID> {
}
