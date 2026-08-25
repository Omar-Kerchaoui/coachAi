package com.lose10.goal;

/**
 * Only LOSE_WEIGHT ships in v1 (docs/product spec §89), but the field is
 * modeled as an enum from day one so future programs (maintenance, muscle
 * gain, etc.) don't require a schema change — just a new value and the
 * logic to support it.
 */
public enum GoalType {
    LOSE_WEIGHT
}
