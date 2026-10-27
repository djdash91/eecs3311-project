package ca.yorku.accessfix.llm;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Checks your API key and network with one tiny real call. Skipped unless ACCESSFIX_LIVE_TESTS=true,
 * so normal test runs cost nothing. It uses the SDK directly on purpose: it tests your setup, not
 * your code. Run it once after setup, and again if ClaudeLLMClient starts failing.
 */
class ClaudeConnectionLiveTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "ACCESSFIX_LIVE_TESTS", matches = "true")
    void claudeAnswers() {
        AnthropicClient client = AnthropicOkHttpClient.fromEnv();
        MessageCreateParams params = MessageCreateParams.builder()
                .model("claude-opus-5-5")
                .maxTokens(20)
                .addUserMessage("Reply with the single word: ready")
                .build();

        String reply = client.messages().create(params).content().stream()
                .flatMap(block -> block.text().stream())
                .map(textBlock -> textBlock.text())
                .collect(Collectors.joining());

        System.out.println("Claude replied: " + reply);
        assertFalse(reply.isBlank());
    }
}
