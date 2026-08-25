package com.lose10.ai.orchestrator;

import com.lose10.ai.FakeAIProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IntentDetectorTest {

    @Test
    void parsesEachIntentFromModelResponse() {
        for (Intent intent : Intent.values()) {
            FakeAIProvider fake = new FakeAIProvider().queueTextResponse(intent.name());
            IntentDetector detector = new IntentDetector(fake);

            assertThat(detector.detect("some message")).isEqualTo(intent);
        }
    }

    @Test
    void defaultsToQuestionOnUnparseableResponse() {
        FakeAIProvider fake = new FakeAIProvider().queueTextResponse("I'm not sure how to classify that");
        IntentDetector detector = new IntentDetector(fake);

        assertThat(detector.detect("some message")).isEqualTo(Intent.QUESTION);
    }
}
