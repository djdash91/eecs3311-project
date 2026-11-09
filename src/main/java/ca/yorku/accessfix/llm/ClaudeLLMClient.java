package ca.yorku.accessfix.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicException;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.NoCredentialsException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.RedactedThinkingBlockParam;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredTextBlock;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.ThinkingBlockParam;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUseBlockParam;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adapter from LLMClient to the Anthropic Java SDK. This is the ONLY class that may import
 * com.anthropic.* (Stage 1, Adapter pattern), so check your imports when an AI tool edits other files.
 *
 * <p>The SDK client is created on first use, not at start-up, so the application starts and all
 * deterministic features work without an API key.
 *
 * <p>Every request turns on the API's server-side fallback: if the model declines a request
 * (stop reason "refusal"), the API retries it on another model instead of failing. A request that
 * is still declined throws LLMException, like any other failure.
 *
 * <p>Active when {@code accessfix.llm=claude}, or when the setting is missing. GeminiLLMClient is the
 * other adapter; the rest of AccessFix only uses LLMClient, so it works with either.
 */
@Component
@ConditionalOnProperty(name = "accessfix.llm", havingValue = "claude", matchIfMissing = true)
public class ClaudeLLMClient implements LLMClient {

    /** The server-side fallback (beta). "default" lets the API choose the fallback model. */
    private static final String FALLBACK_BETA = "server-side-fallback-2026-07-01";

    private final String model;
    private final long defaultMaxTokens;
    private AnthropicClient client;

    public ClaudeLLMClient(@Value("${accessfix.anthropic.model}") String model,
                           @Value("${accessfix.anthropic.max-tokens:16000}") long defaultMaxTokens) {
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
     * Sends the request and converts the reply into AccessFix's own types (F04). Thinking blocks are
     * returned too, so the agent loop can keep them in the conversation unchanged.
     */
    @Override
    public LLMResponse send(LLMRequest request) {
        com.anthropic.models.messages.Message reply = call(() -> client().messages().create(params(request).build()));
        String stopReason = checkStopReason(reply.stopReason());

        List<ContentBlock> content = new ArrayList<>();
        for (com.anthropic.models.messages.ContentBlock block : reply.content()) {
            block.text().ifPresent(text -> content.add(new ContentBlock.Text(text.text())));
            block.toolUse().ifPresent(toolUse -> content.add(
                    new ContentBlock.ToolUse(toolUse.id(), toolUse.name(), toMap(toolUse._input()))));
            block.thinking().ifPresent(thinking -> content.add(
                    new ContentBlock.Thinking(thinking.thinking(), thinking.signature())));
            block.redactedThinking().ifPresent(redacted -> content.add(new ContentBlock.RedactedThinking(redacted.data())));
        }
        return new LLMResponse(content, stopReason);
    }

    /**
     * Asks for an answer in the shape of {@code type}, for example Explanation (F03). The API's
     * structured outputs build a JSON schema from the class and hold the answer to it.
     */
    @Override
    public <T> T sendStructured(LLMRequest request, Class<T> type) {
        StructuredMessage<T> reply = call(() -> client().messages().create(params(request).outputConfig(type).build()));
        checkStopReason(reply.stopReason());
        return reply.content().stream()
                .flatMap(block -> block.text().stream())
                .map(StructuredTextBlock::text)
                .findFirst()
                .orElseThrow(() -> new LLMException("Claude didn't return an answer."));
    }

    String model() {
        return model;
    }

    long maxTokensFor(LLMRequest request) {
        return request.maxTokens() > 0 ? request.maxTokens() : defaultMaxTokens;
    }

    // ---------- AccessFix types to SDK types ----------

    private MessageCreateParams.Builder params(LLMRequest request) {
        MessageCreateParams.Builder params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(maxTokensFor(request))
                .putAdditionalHeader("anthropic-beta", FALLBACK_BETA)
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"));
        if (request.system() != null && !request.system().isBlank()) {
            params.system(request.system());
        }
        for (Message message : request.messages()) {
            params.addMessage(MessageParam.builder()
                    .role(message.role() == Message.Role.USER ? MessageParam.Role.USER : MessageParam.Role.ASSISTANT)
                    .contentOfBlockParams(message.content().stream().map(ClaudeLLMClient::toParam).toList())
                    .build());
        }
        for (ToolDefinition tool : request.tools()) {
            params.addTool(toTool(tool));
        }
        return params;
    }

    private static ContentBlockParam toParam(ContentBlock block) {
        return switch (block) {
            case ContentBlock.Text text -> ContentBlockParam.ofText(TextBlockParam.builder().text(text.text()).build());
            case ContentBlock.ToolUse toolUse -> ContentBlockParam.ofToolUse(ToolUseBlockParam.builder()
                    .id(toolUse.id())
                    .name(toolUse.name())
                    .input(ToolUseBlockParam.Input.builder().additionalProperties(toJson(toolUse.input())).build())
                    .build());
            case ContentBlock.ToolResultBlock result -> {
                ToolResultBlockParam.Builder param = ToolResultBlockParam.builder()
                        .toolUseId(result.toolUseId())
                        .isError(result.isError());
                if (result.image() == null) {
                    param.content(result.content());
                } else {
                    param.contentOfBlocks(List.of(
                            ToolResultBlockParam.Content.Block.ofText(result.content()),
                            ToolResultBlockParam.Content.Block.ofImage(toImage(result.image()))));
                }
                yield ContentBlockParam.ofToolResult(param.build());
            }
            case ContentBlock.Image image -> ContentBlockParam.ofImage(toImage(image));
            case ContentBlock.Thinking thinking -> ContentBlockParam.ofThinking(ThinkingBlockParam.builder()
                    .thinking(thinking.thinking())
                    .signature(thinking.signature())
                    .build());
            case ContentBlock.RedactedThinking redacted -> ContentBlockParam.ofRedactedThinking(
                    RedactedThinkingBlockParam.builder().data(redacted.data()).build());
        };
    }

    private static ImageBlockParam toImage(ContentBlock.Image image) {
        return ImageBlockParam.builder()
                .source(Base64ImageSource.builder()
                        .mediaType(Base64ImageSource.MediaType.of(image.mediaType()))
                        .data(Base64.getEncoder().encodeToString(image.data()))
                        .build())
                .build();
    }

    /** A tool's JSON Schema: "properties" and "required" have their own builder methods, anything else is copied. */
    private static Tool toTool(ToolDefinition definition) {
        Tool.InputSchema.Builder schema = Tool.InputSchema.builder();
        definition.inputSchema().forEach((key, value) -> {
            switch (key) {
                case "type" -> schema.type(JsonValue.from(value));
                case "properties" -> schema.properties(Tool.InputSchema.Properties.builder()
                        .additionalProperties(toJson(asMap(value)))
                        .build());
                case "required" -> schema.required(((List<?>) value).stream().map(String::valueOf).toList());
                default -> schema.putAdditionalProperty(key, JsonValue.from(value));
            }
        });
        return Tool.builder()
                .name(definition.name())
                .description(definition.description())
                .inputSchema(schema.build())
                .build();
    }

    private static Map<String, JsonValue> toJson(Map<String, Object> map) {
        Map<String, JsonValue> json = new LinkedHashMap<>();
        map.forEach((key, value) -> json.put(key, JsonValue.from(value)));
        return json;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return (Map<String, Object>) value;
    }

    // ---------- SDK types to AccessFix types, and errors ----------

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toMap(JsonValue input) {
        Map<String, Object> map = input.convert(Map.class);
        return map == null ? Map.of() : map;
    }

    /** The stop reason as text, or LLMException if the model declined the request even after the fallback. */
    private static String checkStopReason(Optional<StopReason> stopReason) {
        if (stopReason.isPresent() && stopReason.get().equals(StopReason.REFUSAL)) {
            throw new LLMException("Claude declined this request.");
        }
        return stopReason.map(StopReason::asString).orElse("");
    }

    /**
     * Runs one SDK call and turns every SDK exception into LLMException, so callers never see SDK
     * types. The message says what the user can do about it.
     */
    private static <T> T call(Supplier<T> sdkCall) {
        try {
            return sdkCall.get();
        } catch (NoCredentialsException | UnauthorizedException e) {
            throw new LLMException("The Claude API key is missing or not valid. Set ANTHROPIC_API_KEY and restart AccessFix.", e);
        } catch (RateLimitException e) {
            throw new LLMException("The Claude API is receiving too many requests. Wait a moment, then try again.", e);
        } catch (AnthropicServiceException e) {
            throw new LLMException("The Claude API returned an error (HTTP " + e.statusCode() + "): " + e.getMessage(), e);
        } catch (AnthropicIoException e) {
            throw new LLMException("Couldn't reach the Claude API. Check the internet connection, then try again.", e);
        } catch (AnthropicException e) {
            throw new LLMException("The Claude API request failed: " + e.getMessage(), e);
        }
    }
}
