package ca.yorku.accessfix.agent.tools;

import ca.yorku.accessfix.agent.AgentSession;
import ca.yorku.accessfix.agent.mode.AgentMode;
import ca.yorku.accessfix.llm.ContentBlock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Gives each mode its tools and checks every call (view B). "Limits are enforced in code": this class,
 * not the prompt, stops Ask mode from proposing fixes.
 */
@Component
public class ToolRegistry {

    private final Map<String, Tool> toolsByName;

    public ToolRegistry(List<Tool> tools) {
        this.toolsByName = tools.stream().collect(Collectors.toMap(Tool::name, Function.identity()));
    }

    /** The tools the mode allows, sorted by name so requests are repeatable. */
    public List<Tool> toolsFor(AgentMode mode) {
        return toolsByName.values().stream()
                .filter(tool -> mode.allowedTools().contains(tool.name()))
                .sorted((a, b) -> a.name().compareTo(b.name()))
                .toList();
    }

    /**
     * TODO(F04, SD03): check, then run, one tool call. Return ToolResult.error(...) when:
     * <ul>
     *   <li>no tool has that name;</li>
     *   <li>the session's mode doesn't allow it (UC04 1b);</li>
     *   <li>a required input field from the tool's schema is missing or has the wrong type;</li>
     *   <li>the tool throws (catch RuntimeException, and include a short message).</li>
     * </ul>
     * Otherwise return the tool's own result. Unit test each branch with a small fake Tool.
     */
    public ToolResult execute(ContentBlock.ToolUse call, AgentSession session) {
        throw new UnsupportedOperationException("TODO: ToolRegistry.execute");
    }
}
