-- Starter bodyweight/HOME exercise set — enough to generate a real initial
-- plan (docs/backlog L10-601). Expanding the library (GYM/OUTDOOR
-- equipment-based exercises) is a content task, not an architecture change.
INSERT INTO exercise_library (id, name, muscle_group, equipment, difficulty) VALUES
    (gen_random_uuid(), 'Push-ups', 'CHEST', 'BODYWEIGHT', 'BEGINNER'),
    (gen_random_uuid(), 'Bodyweight Squats', 'LEGS', 'BODYWEIGHT', 'BEGINNER'),
    (gen_random_uuid(), 'Plank', 'CORE', 'BODYWEIGHT', 'BEGINNER'),
    (gen_random_uuid(), 'Jumping Jacks', 'FULL_BODY', 'BODYWEIGHT', 'BEGINNER'),
    (gen_random_uuid(), 'Walking Lunges', 'LEGS', 'BODYWEIGHT', 'BEGINNER'),
    (gen_random_uuid(), 'Mountain Climbers', 'CORE', 'BODYWEIGHT', 'INTERMEDIATE'),
    (gen_random_uuid(), 'Glute Bridges', 'GLUTES', 'BODYWEIGHT', 'BEGINNER'),
    (gen_random_uuid(), 'Incline Walk', 'CARDIO', 'NONE', 'BEGINNER');
