package com.lose10.ai.tools;

import com.lose10.ai.dto.ToolDefinition;
import com.lose10.platform.audit.AuditLog;
import com.lose10.platform.audit.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Every tool call funnels through here, which is what makes the shared
 * security rules in docs/AI_ARCHITECTURE.md §4 (ownership binding,
 * audit logging) structural rather than something each tool has to
 * remember to do. New tools register themselves automatically by existing
 * as an {@link AiTool} Spring bean — no change needed here.
 */
@Component
public class ToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);

    private final Map<String, AiTool> toolsByName;
    private final AuditLogService auditLogService;

    public ToolRegistry(List<AiTool> tools, AuditLogService auditLogService) {
        this.toolsByName = tools.stream().collect(Collectors.toMap(AiTool::name, t -> t));
        this.auditLogService = auditLogService;
    }

    public List<ToolDefinition> availableToolDefinitions() {
        return toolsByName.values().stream()
                .map(t -> new ToolDefinition(t.name(), t.description(), t.inputSchema()))
                .toList();
    }

    public ToolResult execute(String toolName, UUID userId, Map<String, Object> arguments) {
        AiTool tool = toolsByName.get(toolName);
        if (tool == null) {
            return ToolResult.error("Unknown tool: " + toolName);
        }
        try {
            ToolResult result = tool.execute(userId, arguments);
            auditLogService.record(userId, AuditLog.ActorType.USER, "AI_TOOL_CALL:" + toolName, "AiTool", null,
                    Map.of("success", !result.isError()));
            return result;
        } catch (Exception e) {
            log.error("Tool '{}' threw an exception for user {}", toolName, userId, e);
            return ToolResult.error("Tool execution failed");
        }
    }
}
