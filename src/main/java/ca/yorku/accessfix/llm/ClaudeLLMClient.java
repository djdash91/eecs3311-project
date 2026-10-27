package ca.yorku.accessfix.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Adapter from LLMClient to the Anthropic Java SDK. This is the ONLY class that may import
 * com.anthropic.* (Stage 1, Adapter pattern), so check your imports when an AI tool edits other files.
 *
 * <p>The SDK client is created on first use, not at start-up, so the application starts and all
 * deterministic features work without an API key.
 *
 * <p>Useful SDK examples (in the anthropics/anthropic-sdk-java repository, folder
 * anthropic-java-example, tag v2.70.0):
 * <ul>
 *   <li>MessagesConversationExample: building a multi-turn request.</li>
 *   <li>BetaMessagesToolsRawExample: defining tools with a raw JSON schema (ideas only: use the
 *       non-beta {@code client.messages()} API, and don't use the "ToolRunner" examples, which run
 *       tools themselves and would bypass ToolRegistry).</li>
 *   <li>StructuredOutputsExample: {@code .outputConfig(SomeClass.class)} for sendStructured.</li>
 *   <li>MessagesImageExample: sending an image (F07).</li>
 * </ul>
 */
@Component
public class ClaudeLLMClient implements LLMClient {

    private final String model;
    private final long defaultMaxTokens;
    private AnthropicClient client;

    public ClaudeLLMClient(@Value("${accessfix.anthropic.model}") String model,
                           @Value("${accessfix.anthropic.max-tokens:4096}") long defaultMaxTokens) {
        this.model = model;
        this.defaultMaxTokens = defaultMaxTokens;
    }

    /** Reads ANTHROPIC_API_KEY from the environment the first time it is needed. */
    private synchronized AnthropicClient client() {
        if (client == null) {
            client = AnthropicOkHttpClient.fromEnv();
        }
        return client;
    }

    /**
     * TODO(F04): implement.
     * <ol>
     *   <li>Start from {@code MessageCreateParams.builder().model(model).maxTokens(...)}, and add
     *       the system prompt with {@code .system(request.system())}.</li>
     *   <li>Convert each AccessFix Message and ContentBlock into the SDK's message params
     *       (text, tool_use, tool_result, image blocks).</li>
     *   <li>Convert each ToolDefinition into an SDK Tool with its input schema.</li>
     *   <li>Call {@code client().messages().create(params)}.</li>
     *   <li>Convert the SDK's response content back into ContentBlocks (Text and ToolUse).</li>
     *   <li>Wrap every SDK exception in LLMException, so callers never see SDK types.</li>
     * </ol>
     * Ask your AI tool to help with the conversion code, then read every line: the conversions are
     * where subtle bugs (lost tool_use IDs, wrong roles) hide.
     */
    @Override
    public LLMResponse send(LLMRequest request) {
        throw new UnsupportedOperationException("TODO: ClaudeLLMClient.send");
    }

    /** TODO(F03, F07): structured outputs, as in the SDK's StructuredOutputsExample. */
    @Override
    public <T> T sendStructured(LLMRequest request, Class<T> type) {
        throw new UnsupportedOperationException("TODO: ClaudeLLMClient.sendStructured");
    }

    String model() {
        return model;
    }

    long maxTokensFor(LLMRequest request) {
        return request.maxTokens() > 0 ? request.maxTokens() : defaultMaxTokens;
    }
}
