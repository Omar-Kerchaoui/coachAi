package com.lose10;

import com.lose10.platform.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Proves the application context — including Flyway migrations against a
 * real pgvector-enabled Postgres and the Redis connection — starts cleanly.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class Lose10ApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }
}
