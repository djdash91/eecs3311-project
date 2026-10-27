package ca.yorku.accessfix.llm;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The model's reply: text and any tool calls.
 *
 * @param stopReason for example "end_turn", "tool_use" or "max_tokens"
 */
public record LLMResponse(List<ContentBlock> content, String stopReason) {

    public LLMResponse {
        content = List.copyOf(content);
    }

    public String text() {
        return content.stream()
                .filter(block -> block instanceof ContentBlock.Text)
                .map(block -> ((ContentBlock.Text) block).text())
                .collect(Collectors.joining("\n"));
    }

    public List<ContentBlock.ToolUse> toolCalls() {
        return content.stream()
                .filter(block -> block instanceof ContentBlock.ToolUse)
                .map(block -> (ContentBlock.ToolUse) block)
                .toList();
    }

    public boolean hasToolCalls() {
        return !toolCalls().isEmpty();
    }
}
