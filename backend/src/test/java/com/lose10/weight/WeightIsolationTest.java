package com.lose10.weight;

import com.lose10.platform.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the ownership-scoped repository lookup pattern mandated by
 * docs/SECURITY.md §3 actually holds: fetching an entity by (id, userId)
 * where the id belongs to a different user returns nothing, rather than
 * leaking the row. This is the pattern every user-owned repository in the
 * codebase must follow (docs/backlog L10-211 / L10-1001).
 */
@SpringBootTest
@ActiveProfiles("test")
class WeightIsolationTest extends AbstractIntegrationTest {

    @Autowired
    private WeightEntryRepository repository;

    @Test
    void userCannotFetchAnotherUsersWeightEntryByOwnershipScopedLookup() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        WeightEntry entryOwnedByA = repository.save(
                new WeightEntry(userA, BigDecimal.valueOf(80.0), Instant.now(), WeightSource.USER_REPORTED));

        assertThat(repository.findByIdAndUserId(entryOwnedByA.getId(), userA)).isPresent();
        assertThat(repository.findByIdAndUserId(entryOwnedByA.getId(), userB)).isEmpty();
    }
}
