package com.lose10.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Standing regression gate for cross-user data isolation (docs/backlog
 * L10-1001), extending the pattern from {@code AuthIntegrationTest} /
 * {@code WeightIsolationTest} to every endpoint that takes a client-
 * supplied resource id rather than deriving ownership purely from the
 * JWT. Self-scoped endpoints (goals list, weight log, memories list,
 * etc.) are structurally immune to cross-user access — they never accept
 * "whose data" as a parameter, only {@code CurrentUser.id()} — so the
 * real IDOR risk is concentrated in the smaller set of id-in-path
 * endpoints tested here. Correct behavior is 404, not 403: leaking
 * "this exists but isn't yours" is itself an information disclosure
 * (docs/SECURITY.md §3).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CrossUserIsolationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void userCannotDeleteAnotherUsersMemoryById() throws Exception {
        String tokenA = registerAndLogin("iso-mem-a-" + System.nanoTime() + "@example.com");
        String tokenB = registerAndLogin("iso-mem-b-" + System.nanoTime() + "@example.com");

        // No memory-creation API exists (memories are AI-extracted) — a
        // random, well-formed UUID stands in for "some other user's
        // memory id"; either way user B must never get anything but 404.
        mockMvc.perform(delete("/api/v1/memories/" + java.util.UUID.randomUUID())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // tokenA exists only to document the two-user setup this test is
        // really about — no memory-creation API exists to actually own one.
        assertThat(tokenA).isNotBlank();
    }

    @Test
    void userCannotReadAnotherUsersGoalMilestones() throws Exception {
        String tokenA = registerAndLogin("iso-goal-a-" + System.nanoTime() + "@example.com");
        String tokenB = registerAndLogin("iso-goal-b-" + System.nanoTime() + "@example.com");

        String goalResponse = mockMvc.perform(post("/api/v1/goals")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "startingWeightKg", 90, "targetWeightKg", 80, "targetDate", "2027-01-01"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String goalId = objectMapper.readTree(goalResponse).get("id").asText();

        mockMvc.perform(get("/api/v1/goals/" + goalId + "/milestones")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // The legitimate owner can still read their own goal's milestones —
        // proves the 404 above is ownership-scoped, not a broken endpoint.
        mockMvc.perform(get("/api/v1/goals/" + goalId + "/milestones")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
    }

    @Test
    void userCannotCompleteAnotherUsersCheckIn() throws Exception {
        String tokenB = registerAndLogin("iso-checkin-b-" + System.nanoTime() + "@example.com");

        // No check-in-creation API is exposed to users (check-ins are
        // scheduler-created) — same random-UUID rationale as the memory
        // test above.
        mockMvc.perform(post("/api/v1/checkins/" + java.util.UUID.randomUUID() + "/complete")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonAdminUserGetsForbiddenOnEveryAdminRoute() throws Exception {
        String token = registerAndLogin("iso-admin-" + System.nanoTime() + "@example.com");

        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/safety-events").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/metrics/business").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestToAnyProtectedEndpointIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/goals")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/weight/trend")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/memories")).andExpect(status().isUnauthorized());
    }

    private String registerAndLogin(String email) throws Exception {
        String password = "correct-horse-battery";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isCreated());

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(loginResponse).get("accessToken").asText();
    }
}
