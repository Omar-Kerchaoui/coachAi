package com.lose10.ai.dto;

import java.util.Map;

/**
 * {@code inputSchema} is a JSON Schema object (as a Map, matching the
 * Messages API's tool definition shape directly).
 */
public record ToolDefinition(String name, String description, Map<String, Object> inputSchema) {
}
