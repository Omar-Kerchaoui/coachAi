package com.lose10.training;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "exercise_library")
public class ExerciseLibrary extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "muscle_group", length = 50)
    private String muscleGroup;

    @Column(length = 50)
    private String equipment;

    @Column(nullable = false, length = 20)
    private String difficulty = "BEGINNER";

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    protected ExerciseLibrary() {
    }

    public ExerciseLibrary(String name, String muscleGroup, String equipment, String difficulty) {
        this.name = name;
        this.muscleGroup = muscleGroup;
        this.equipment = equipment;
        this.difficulty = difficulty;
    }

    public String getName() {
        return name;
    }

    public String getMuscleGroup() {
        return muscleGroup;
    }

    public String getEquipment() {
        return equipment;
    }

    public String getDifficulty() {
        return difficulty;
    }
}
