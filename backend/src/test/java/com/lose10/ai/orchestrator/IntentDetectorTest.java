package com.lose10.ai.orchestrator;

import com.lose10.ai.FakeAIProvider;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IntentDetectorTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void parsesEachIntentFromModelResponse() {
        for (Intent intent : Intent.values()) {
            FakeAIProvider fake = new FakeAIProvider().queueTextResponse(intent.name());
            IntentDetector detector = new IntentDetector(fake);

            assertThat(detector.detect(USER_ID, "some message")).isEqualTo(intent);
        }
    }

    @Test
    void defaultsToQuestionOnUnparseableResponse() {
        FakeAIProvider fake = new FakeAIProvider().queueTextResponse("I'm not sure how to classify that");
        IntentDetector detector = new IntentDetector(fake);

        assertThat(detector.detect(USER_ID, "some message")).isEqualTo(Intent.QUESTION);
    }
}
