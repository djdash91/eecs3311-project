package ca.yorku.accessfix.llm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * A scripted LLMClient for deterministic tests of the agent loop. Queue the replies the "model" should
 * give, run the code under test, then inspect the requests it sent. This is what the LLMClient
 * interface (dependency inversion) buys you.
 */
public class FakeLLMClient implements LLMClient {

    private final Deque<Object> replies = new ArrayDeque<>();
    private final List<LLMRequest> requests = new ArrayList<>();

    public FakeLLMClient reply(LLMResponse response) {
        replies.add(response);
        return this;
    }

    /** The next call throws, to test API failure handling. */
    public FakeLLMClient fail(String message) {
        replies.add(new LLMException(message));
        return this;
    }

    public FakeLLMClient structured(Object value) {
        replies.add(value);
        return this;
    }

    public List<LLMRequest> requests() {
        return List.copyOf(requests);
    }

    @Override
    public LLMResponse send(LLMRequest request) {
        return (LLMResponse) next(request);
    }

    @Override
    public <T> T sendStructured(LLMRequest request, Class<T> type) {
        return type.cast(next(request));
    }

    private Object next(LLMRequest request) {
        requests.add(request);
        if (replies.isEmpty()) {
            throw new AssertionError("FakeLLMClient got more requests than scripted replies");
        }
        Object reply = replies.poll();
        if (reply instanceof LLMException e) {
            throw e;
        }
        return reply;
    }

    // ---- Helpers for building replies ----

    public static LLMResponse text(String text) {
        return new LLMResponse(List.of(new ContentBlock.Text(text)), "end_turn");
    }

    public static LLMResponse toolCall(String id, String toolName, Map<String, Object> input) {
        return new LLMResponse(List.of(new ContentBlock.ToolUse(id, toolName, input)), "tool_use");
    }
}
