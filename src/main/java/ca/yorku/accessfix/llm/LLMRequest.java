package ca.yorku.accessfix.llm;

import java.util.List;

/**
 * Everything needed for one call to the model.
 *
 * @param system    the system prompt (mode instructions plus remembered decisions, F11)
 * @param maxTokens 0 means "use the configured default"
 */
public record LLMRequest(String system, List<Message> messages, List<ToolDefinition> tools, int maxTokens) {

    public LLMRequest {
        messages = List.copyOf(messages);
        tools = List.copyOf(tools);
    }

    /** A request with no tools, for one-off calls such as explanations (F03). */
    public static LLMRequest of(String system, List<Message> messages) {
        return new LLMRequest(system, messages, List.of(), 0);
    }
}
