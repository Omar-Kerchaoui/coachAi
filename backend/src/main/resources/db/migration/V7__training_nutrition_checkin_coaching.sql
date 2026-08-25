-- TRAINING domain
CREATE TABLE exercise_library (
    id            UUID PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    muscle_group  VARCHAR(50),
    equipment     VARCHAR(50),
    difficulty    VARCHAR(20) NOT NULL DEFAULT 'BEGINNER',
    video_url     VARCHAR(500),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE training_plans (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    goal_id             UUID REFERENCES goals (id) ON DELETE SET NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    current_version_id  UUID,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_training_plans_one_active_per_user ON training_plans (user_id) WHERE status = 'ACTIVE';

CREATE TABLE training_plan_versions (
    id                  UUID PRIMARY KEY,
    plan_id             UUID NOT NULL REFERENCES training_plans (id) ON DELETE CASCADE,
    version_no          INTEGER NOT NULL,
    environment         VARCHAR(20) NOT NULL DEFAULT 'HOME',
    reason_for_change   TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_training_plan_versions_plan_version ON training_plan_versions (plan_id, version_no);

ALTER TABLE training_plans ADD CONSTRAINT fk_training_plans_current_version
    FOREIGN KEY (current_version_id) REFERENCES training_plan_versions (id) ON DELETE SET NULL;

CREATE TABLE workouts (
    id                UUID PRIMARY KEY,
    plan_version_id   UUID NOT NULL REFERENCES training_plan_versions (id) ON DELETE CASCADE,
    day_of_week       VARCHAR(10) NOT NULL,
    workout_type      VARCHAR(30) NOT NULL DEFAULT 'FULL_BODY',
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_workouts_plan_version ON workouts (plan_version_id);

CREATE TABLE workout_exercises (
    id            UUID PRIMARY KEY,
    workout_id    UUID NOT NULL REFERENCES workouts (id) ON DELETE CASCADE,
    exercise_id   UUID NOT NULL REFERENCES exercise_library (id),
    sets          INTEGER,
    reps          INTEGER,
    duration_sec  INTEGER,
    rest_sec      INTEGER,
    intensity     VARCHAR(20),
    notes         TEXT,
    order_index   INTEGER NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_workout_exercises_workout_id ON workout_exercises (workout_id, order_index);

CREATE TABLE workout_sessions (
    id               UUID PRIMARY KEY,
    user_id          UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    workout_id       UUID NOT NULL REFERENCES workouts (id),
    completed_at     TIMESTAMPTZ NOT NULL,
    perceived_effort INTEGER,
    notes            TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_workout_sessions_user_completed ON workout_sessions (user_id, completed_at DESC);

-- NUTRITION domain
CREATE TABLE nutrition_profiles (
    id                    UUID PRIMARY KEY,
    user_id               UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    dietary_restrictions  JSONB,
    disliked_foods        JSONB,
    target_calories       INTEGER,
    target_protein_g      INTEGER,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_nutrition_profiles_user_id ON nutrition_profiles (user_id);

CREATE TABLE nutrition_logs (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    log_date   DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_nutrition_logs_user_date ON nutrition_logs (user_id, log_date);

CREATE TABLE meals (
    id                 UUID PRIMARY KEY,
    nutrition_log_id   UUID NOT NULL REFERENCES nutrition_logs (id) ON DELETE CASCADE,
    meal_time          TIMESTAMPTZ NOT NULL,
    meal_type          VARCHAR(20) NOT NULL DEFAULT 'SNACK',
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_meals_nutrition_log_id ON meals (nutrition_log_id);

CREATE TABLE meal_items (
    id                     UUID PRIMARY KEY,
    meal_id                UUID NOT NULL REFERENCES meals (id) ON DELETE CASCADE,
    raw_text               TEXT NOT NULL,
    estimated_calories     INTEGER,
    estimated_protein_g    INTEGER,
    source                 VARCHAR(20) NOT NULL DEFAULT 'USER_TEXT',
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_meal_items_meal_id ON meal_items (meal_id);

-- CHECK-INS domain
CREATE TABLE check_ins (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    check_in_type   VARCHAR(20) NOT NULL,
    scheduled_for   TIMESTAMPTZ NOT NULL,
    completed_at    TIMESTAMPTZ,
    responses       JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_check_ins_user_scheduled ON check_ins (user_id, scheduled_for DESC);

CREATE TABLE daily_check_ins (
    id                     UUID PRIMARY KEY,
    user_id                UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    check_in_date          DATE NOT NULL,
    mood                   VARCHAR(20),
    energy                 VARCHAR(20),
    adherence_self_report  VARCHAR(20),
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_daily_check_ins_user_date ON daily_check_ins (user_id, check_in_date);

CREATE TABLE weekly_reviews (
    id            UUID PRIMARY KEY,
    user_id       UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    week_start    DATE NOT NULL,
    summary_text  TEXT NOT NULL,
    generated_by  VARCHAR(10) NOT NULL DEFAULT 'AI',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_weekly_reviews_user_week ON weekly_reviews (user_id, week_start);

-- COACHING domain
CREATE TABLE coaching_states (
    id             UUID PRIMARY KEY,
    user_id        UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    mode           VARCHAR(20) NOT NULL DEFAULT 'AI_ACTIVE',
    style          VARCHAR(20) NOT NULL DEFAULT 'BALANCED',
    paused_reason  TEXT,
    updated_by     UUID,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_coaching_states_user_id ON coaching_states (user_id);

CREATE TABLE coaching_events (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    payload    JSONB,
    actor      VARCHAR(10) NOT NULL DEFAULT 'SYSTEM',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_coaching_events_user_id ON coaching_events (user_id, created_at DESC);

CREATE TABLE coach_preferences (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    key        VARCHAR(50) NOT NULL,
    value      TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_coach_preferences_user_key ON coach_preferences (user_id, key);
