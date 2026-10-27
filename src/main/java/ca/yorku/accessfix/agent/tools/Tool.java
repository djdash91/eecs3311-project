package ca.yorku.accessfix.agent.tools;

import ca.yorku.accessfix.agent.AgentSession;
import ca.yorku.accessfix.llm.ToolDefinition;
import java.util.Map;

/**
 * Something the agent can do (view B). The model only ever acts through tools that our code defines,
 * checks and runs. ToolRegistry finds every Tool bean.
 */
public interface Tool {

    /** The name the model calls, for example "search_wcag". */
    String name();

    /** Tells the model when and how to use the tool. Tune it in Stage 3. */
    String description();

    /** JSON Schema of the input, built with JsonSchemas. */
    Map<String, Object> inputSchema();

    /**
     * Runs the tool. Return ToolResult.error(...) for bad input instead of throwing, so the model gets
     * a useful message. ToolRegistry turns unexpected exceptions into errors too.
     */
    ToolResult execute(Map<String, Object> input, AgentSession session);

    default ToolDefinition definition() {
        return new ToolDefinition(name(), description(), inputSchema());
    }
}
