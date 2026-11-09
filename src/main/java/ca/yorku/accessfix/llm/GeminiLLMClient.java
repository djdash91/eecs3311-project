package ca.yorku.accessfix.llm;

import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.errors.ClientException;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.errors.ServerException;
import com.google.genai.types.AutomaticFunctionCallingConfig;
import com.google.genai.types.Blob;
import com.google.genai.types.Candidate;
import com.google.genai.types.Content;
import com.google.genai.types.FinishReason;
import com.google.genai.types.FunctionCall;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.FunctionResponse;
import com.google.genai.types.FunctionResponseBlob;
import com.google.genai.types.FunctionResponsePart;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import com.google.genai.types.Part;
import com.google.genai.types.Tool;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adapter from LLMClient to Google's Gen AI Java SDK, for Gemini models from Google AI Studio. This
 * is the ONLY class that may import com.google.genai.* (Adapter pattern, as ClaudeLLMClient is for
 * com.anthropic.*). Active when {@code accessfix.llm=gemini}.
 *
 * <p>The SDK client is created on first use, so the application starts and all deterministic
 * features work without an API key. The key comes from the GEMINI_API_KEY environment variable.
 *
 * <p>Three Gemini differences are handled here, so the rest of AccessFix doesn't see them:
 * <ul>
 *   <li>A tool call can carry a thought signature that must be sent back unchanged. It is returned
 *       as a ContentBlock.Thinking just before the block it belongs to, and attached to that block
 *       again when the conversation is sent back.</li>
 *   <li>A tool result is matched to its call by the tool's name, so the name is looked up from the
 *       ToolUse with the same ID earlier in the conversation.</li>
 *   <li>Structured outputs need a JSON schema, which is built from the record's fields.</li>
 * </ul>
 * Stop reasons are translated to the same values ClaudeLLMClient returns ("end_turn", "tool_use",
 * "max_tokens"), so the agent loop works the same with either model.
 */
@Component
@ConditionalOnProperty(name = "accessfix.llm", havingValue = "gemini")
public class GeminiLLMClient implements LLMClient {

    /** Marks IDs made up here for tool calls that came without one, so they aren't sent to Gemini. */
    static final String GENERATED_ID_PREFIX = "gemini-call-";

    /**
     * How long one request may take. The SDK has no limit by default, so a request Google never
     * answers would leave the GUI waiting forever.
     */
    static final int TIMEOUT_MILLIS = 60_000;

    /**
     * One try and one retry. The SDK retries several times by default, and every try counts toward
     * the free tier's daily limit, so a busy model could use it up in a few calls.
     */
    static final int ATTEMPTS = 2;

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final String model;
    private final int defaultMaxTokens;
    private Client client;

    public GeminiLLMClient(@Value("${accessfix.gemini.model}") String model,
                           @Value("${accessfix.gemini.max-tokens:16000}") int defaultMaxTokens) {
        this.model = model;
        this.defaultMaxTokens = defaultMaxTokens;
    }

    /** Reads GEMINI_API_KEY from the environment the first time it is needed. */
    private synchronized Client client() {
        if (client == null) {
            String key = System.getenv("GEMINI_API_KEY");
            if (key == null || key.isBlank()) {
                throw new LLMException("The Gemini API key is missing. Set GEMINI_API_KEY and restart AccessFix.");
            }
            client = Client.builder()
                    .apiKey(key)
                    .httpOptions(HttpOptions.builder()
                            .timeout(TIMEOUT_MILLIS)
                            .retryOptions(HttpRetryOptions.builder().attempts(ATTEMPTS))
                            .build())
                    .build();
        }
        return client;
    }

    @Override
    public LLMResponse send(LLMRequest request) {
        GenerateContentResponse reply = call(() -> client().models.generateContent(
                model, contents(request), config(request).build()));
        Candidate candidate = firstCandidate(reply);

        List<ContentBlock> content = new ArrayList<>();
        for (Part part : candidate.content().flatMap(Content::parts).orElse(List.of())) {
            if (part.thought().orElse(false)) {
                continue;
            }
            part.thoughtSignature().ifPresent(signature -> content.add(
                    new ContentBlock.Thinking("", Base64.getEncoder().encodeToString(signature))));
            part.text().ifPresent(text -> content.add(new ContentBlock.Text(text)));
            part.functionCall().ifPresent(call -> content.add(new ContentBlock.ToolUse(
                    call.id().filter(id -> !id.isBlank()).orElse(GENERATED_ID_PREFIX + UUID.randomUUID()),
                    call.name().orElse(""),
                    call.args().orElse(Map.of()))));
        }
        boolean toolCalls = content.stream().anyMatch(block -> block instanceof ContentBlock.ToolUse);
        return new LLMResponse(content, stopReason(candidate, toolCalls));
    }

    /**
     * Asks for an answer in the shape of {@code type}, a record such as Explanation (F03). Gemini is
     * given a JSON schema built from the record's fields, and the JSON it returns is read back into
     * the record.
     */
    @Override
    public <T> T sendStructured(LLMRequest request, Class<T> type) {
        GenerateContentConfig config = config(request)
                .responseMimeType("application/json")
                .responseJsonSchema(schemaFor(type))
                .build();
        GenerateContentResponse reply = call(() -> client().models.generateContent(model, contents(request), config));
        Candidate candidate = firstCandidate(reply);
        stopReason(candidate, false);

        StringBuilder json = new StringBuilder();
        for (Part part : candidate.content().flatMap(Content::parts).orElse(List.of())) {
            if (!part.thought().orElse(false)) {
                part.text().ifPresent(json::append);
            }
        }
        try {
            return JSON.readValue(json.toString(), type);
        } catch (JacksonException e) {
            throw new LLMException("Gemini's answer wasn't in the expected format.", e);
        }
    }

    String model() {
        return model;
    }

    int maxTokensFor(LLMRequest request) {
        return request.maxTokens() > 0 ? request.maxTokens() : defaultMaxTokens;
    }

    // ---------- AccessFix types to SDK types ----------

    private GenerateContentConfig.Builder config(LLMRequest request) {
        GenerateContentConfig.Builder config = GenerateContentConfig.builder()
                .maxOutputTokens(maxTokensFor(request))
                // The SDK can run Java methods as tools itself; AccessFix runs tools through ToolRegistry.
                .automaticFunctionCalling(AutomaticFunctionCallingConfig.builder().disable(true));
        if (request.system() != null && !request.system().isBlank()) {
            config.systemInstruction(Content.fromParts(Part.fromText(request.system())));
        }
        if (!request.tools().isEmpty()) {
            config.tools(List.of(Tool.builder()
                    .functionDeclarations(request.tools().stream().map(GeminiLLMClient::toDeclaration).toList())
                    .build()));
        }
        return config;
    }

    private static FunctionDeclaration toDeclaration(ToolDefinition tool) {
        return FunctionDeclaration.builder()
                .name(tool.name())
                .description(tool.description())
                .parametersJsonSchema(tool.inputSchema())
                .build();
    }

    static List<Content> contents(LLMRequest request) {
        Map<String, String> toolNames = new HashMap<>();
        for (Message message : request.messages()) {
            for (ContentBlock block : message.content()) {
                if (block instanceof ContentBlock.ToolUse toolUse) {
                    toolNames.put(toolUse.id(), toolUse.name());
                }
            }
        }
        List<Content> contents = new ArrayList<>();
        for (Message message : request.messages()) {
            contents.add(Content.builder()
                    .role(message.role() == Message.Role.USER ? "user" : "model")
                    .parts(toParts(message.content(), toolNames))
                    .build());
        }
        return contents;
    }

    /** A Thinking block's signature is attached to the part that follows it, where Gemini put it. */
    private static List<Part> toParts(List<ContentBlock> blocks, Map<String, String> toolNames) {
        List<Part> parts = new ArrayList<>();
        byte[] signature = null;
        for (ContentBlock block : blocks) {
            Part.Builder part = Part.builder();
            switch (block) {
                case ContentBlock.Thinking thinking -> {
                    signature = decode(thinking.signature());
                    continue;
                }
                case ContentBlock.RedactedThinking redacted -> {
                    continue;
                }
                case ContentBlock.Text text -> part.text(text.text());
                case ContentBlock.Image image -> part.inlineData(Blob.builder().mimeType(image.mediaType()).data(image.data()));
                case ContentBlock.ToolUse toolUse -> {
                    FunctionCall.Builder call = FunctionCall.builder().name(toolUse.name()).args(toolUse.input());
                    if (!toolUse.id().startsWith(GENERATED_ID_PREFIX)) {
                        call.id(toolUse.id());
                    }
                    part.functionCall(call.build());
                }
                case ContentBlock.ToolResultBlock result -> {
                    String name = toolNames.get(result.toolUseId());
                    if (name == null) {
                        throw new LLMException("A tool result has no matching tool call: " + result.toolUseId());
                    }
                    FunctionResponse.Builder response = FunctionResponse.builder()
                            .name(name)
                            .response(Map.of(result.isError() ? "error" : "output", result.content()));
                    if (!result.toolUseId().startsWith(GENERATED_ID_PREFIX)) {
                        response.id(result.toolUseId());
                    }
                    if (result.image() != null) {
                        response.parts(FunctionResponsePart.builder().inlineData(FunctionResponseBlob.builder()
                                .mimeType(result.image().mediaType())
                                .data(result.image().data())));
                    }
                    part.functionResponse(response.build());
                }
            }
            if (signature != null) {
                part.thoughtSignature(signature);
                signature = null;
            }
            parts.add(part.build());
        }
        return parts;
    }

    /** The signature's bytes, or null if it isn't one of Gemini's (for example, a Claude signature). */
    private static byte[] decode(String signature) {
        if (signature == null || signature.isBlank()) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(signature);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * A JSON schema for a record whose fields are text, numbers or true/false, such as Explanation.
     * Every field is required, so the model fills them all in.
     */
    static Map<String, Object> schemaFor(Class<?> type) {
        if (!type.isRecord()) {
            throw new IllegalArgumentException("Structured answers must be records: " + type.getName());
        }
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        for (RecordComponent component : type.getRecordComponents()) {
            properties.put(component.getName(), Map.of("type", jsonType(component.getType())));
            required.add(component.getName());
        }
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("additionalProperties", false);
        return schema;
    }

    private static String jsonType(Class<?> type) {
        if (type == String.class) {
            return "string";
        }
        if (type == boolean.class || type == Boolean.class) {
            return "boolean";
        }
        if (type == int.class || type == Integer.class || type == long.class || type == Long.class) {
            return "integer";
        }
        if (type == double.class || type == Double.class || type == float.class || type == Float.class) {
            return "number";
        }
        throw new IllegalArgumentException("Unsupported field type in a structured answer: " + type.getName());
    }

    // ---------- SDK types to AccessFix types, and errors ----------

    private static Candidate firstCandidate(GenerateContentResponse reply) {
        List<Candidate> candidates = reply.candidates().orElse(List.of());
        if (candidates.isEmpty()) {
            boolean blocked = reply.promptFeedback().flatMap(feedback -> feedback.blockReason()).isPresent();
            throw new LLMException(blocked ? "Gemini declined this request." : "Gemini didn't return an answer.");
        }
        return candidates.get(0);
    }

    /**
     * The stop reason in the same words ClaudeLLMClient uses. A safety stop or a malformed tool call
     * throws LLMException, like a declined request does with Claude.
     */
    private static String stopReason(Candidate candidate, boolean toolCalls) {
        FinishReason.Known reason = candidate.finishReason()
                .map(FinishReason::knownEnum)
                .orElse(FinishReason.Known.STOP);
        return switch (reason) {
            case STOP, FINISH_REASON_UNSPECIFIED -> toolCalls ? "tool_use" : "end_turn";
            case MAX_TOKENS -> "max_tokens";
            case MALFORMED_FUNCTION_CALL, UNEXPECTED_TOOL_CALL, TOO_MANY_TOOL_CALLS ->
                    throw new LLMException("Gemini made a tool call AccessFix couldn't use (" + reason + ").");
            default -> throw new LLMException("Gemini declined this request (" + reason + ").");
        };
    }

    /**
     * Runs one SDK call and turns every SDK exception into LLMException, so callers never see SDK
     * types. The message says what the user can do about it.
     */
    private static <T> T call(Supplier<T> sdkCall) {
        try {
            return sdkCall.get();
        } catch (ClientException e) {
            if (e.code() == 401 || e.code() == 403) {
                throw new LLMException("The Gemini API key isn't valid. Check GEMINI_API_KEY and restart AccessFix.", e);
            }
            if (e.code() == 429 && e.message() != null && e.message().contains("PerDay")) {
                throw new LLMException("Gemini's daily request limit was reached (the free tier allows few requests "
                        + "a day). Try again tomorrow, or turn on billing for the key in Google AI Studio.", e);
            }
            if (e.code() == 429) {
                throw new LLMException("Gemini's rate limit was reached (the free tier allows few requests a minute). "
                        + "Wait a minute, then try again.", e);
            }
            throw new LLMException("Gemini rejected the request (HTTP " + e.code() + "): " + e.message(), e);
        } catch (ServerException e) {
            throw new LLMException("Gemini had a server error (HTTP " + e.code() + "). Try again shortly.", e);
        } catch (ApiException e) {
            throw new LLMException("The Gemini API returned an error (HTTP " + e.code() + "): " + e.message(), e);
        } catch (GenAiIOException e) {
            throw new LLMException("Couldn't reach the Gemini API, or it didn't answer within "
                    + TIMEOUT_MILLIS / 1000 + " seconds. Check the internet connection, then try again.", e);
        }
    }
}
