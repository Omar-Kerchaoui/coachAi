package com.lose10.ai.tools;

import com.lose10.goal.GoalService;
import com.lose10.goal.dto.CreateGoalRequest;
import com.lose10.platform.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * docs/AI_ARCHITECTURE.md §4: "The server determines the authenticated
 * user... Never accept arbitrary userId from an LLM tool call." This test
 * proves it structurally — a tool call carrying a different user's id
 * inside the model-controlled arguments map has zero effect; only the
 * server-supplied userId parameter is ever consulted.
 */
@SpringBootTest
@ActiveProfiles("test")
class ToolOwnershipTest extends AbstractIntegrationTest {

    @Autowired
    private GetGoalTool getGoalTool;

    @Autowired
    private GoalService goalService;

    @Test
    void toolIgnoresForgedUserIdInsideModelSuppliedArguments() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        goalService.createGoal(userA, new CreateGoalRequest(BigDecimal.valueOf(80), BigDecimal.valueOf(70), null));
        goalService.createGoal(userB, new CreateGoalRequest(BigDecimal.valueOf(95), BigDecimal.valueOf(85), null));

        // The "arguments" map simulates what a compromised or hallucinating
        // model might send — including an attempted userId override. The
        // AiTool interface doesn't even expose a way to read it; the real
        // caller-supplied userId (userA) is what must win.
        ToolResult result = getGoalTool.execute(userA, Map.of("userId", userB.toString(), "unexpectedField", "ignored"));

        assertThat(result.isError()).isFalse();
        assertThat(result.content()).contains("80").contains("70");
        assertThat(result.content()).doesNotContain("95").doesNotContain("85");
    }
}
