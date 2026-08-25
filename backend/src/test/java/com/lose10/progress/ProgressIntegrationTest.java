package com.lose10.progress;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lose10.platform.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers L10-310's acceptance criterion directly: given the product-spec
 * example (82kg -> 77kg current -> 72kg target), the summary must report
 * "10kg to go, 5kg lost, 5kg remaining, 50% progress".
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProgressIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void summaryMatchesProductSpecExample() throws Exception {
        String email = "progress-" + System.nanoTime() + "@example.com";
        String password = "correct-horse-battery";

        mockMvc.perform(authed(post("/api/v1/auth/register"), Map.of("email", email, "password", password)))
                .andExpect(status().isCreated());

        String loginResponse = mockMvc.perform(authed(post("/api/v1/auth/login"), Map.of("email", email, "password", password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(loginResponse).get("accessToken").asText();

        mockMvc.perform(authed(post("/api/v1/goals"), Map.of("startingWeightKg", 82.0, "targetWeightKg", 72.0))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated());

        mockMvc.perform(authed(post("/api/v1/weight"), Map.of("weightKg", 77.0))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/progress/summary").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startingWeightKg").value(82.0))
                .andExpect(jsonPath("$.currentWeightKg").value(77.0))
                .andExpect(jsonPath("$.targetWeightKg").value(72.0))
                .andExpect(jsonPath("$.weightLostKg").value(5.0))
                .andExpect(jsonPath("$.weightRemainingKg").value(5.0))
                .andExpect(jsonPath("$.progressPercent").value(50));
    }

    private MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder builder, Object body) throws Exception {
        return builder.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
    }
}
