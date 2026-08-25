package com.lose10.ai.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lose10.platform.error.ApiException;
import com.lose10.profile.ProfileService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class GetUserProfileTool implements AiTool {

    private final ProfileService profileService;
    private final ObjectMapper objectMapper;

    public GetUserProfileTool(ProfileService profileService, ObjectMapper objectMapper) {
        this.profileService = profileService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "getUserProfile";
    }

    @Override
    public String description() {
        return "Returns the user's profile: display name, timezone, locale, units, and coach style preference.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of("type", "object", "properties", Map.of(), "required", java.util.List.of());
    }

    @Override
    public ToolResult execute(UUID userId, Map<String, Object> arguments) {
        try {
            return ToolResult.ok(objectMapper.writeValueAsString(profileService.getProfile(userId)));
        } catch (ApiException e) {
            return ToolResult.error(e.getMessage());
        } catch (Exception e) {
            return ToolResult.error("Failed to read profile");
        }
    }
}
