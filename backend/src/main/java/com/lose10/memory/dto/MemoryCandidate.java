package com.lose10.memory.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lose10.memory.MemoryCategory;
import com.lose10.memory.MemoryImportance;

public record MemoryCandidate(
        @JsonProperty("worth_remembering") boolean worthRemembering,
        MemoryCategory category,
        String key,
        String content,
        MemoryImportance importance,
        double confidence
) {
    public static final MemoryCandidate NONE = new MemoryCandidate(false, null, null, null, null, 0.0);
}
