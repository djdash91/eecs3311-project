package ca.yorku.accessfix.agent.tools;

import ca.yorku.accessfix.agent.AgentSession;
import ca.yorku.accessfix.agent.mode.AgentMode;
import ca.yorku.accessfix.llm.ContentBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Gives each mode its tools and checks every call (view B). "Limits are enforced in code": this class,
 * not the prompt, stops Ask mode from proposing fixes.
 */
@Component
public class ToolRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(ToolRegistry.class);

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
     * Checks, then runs, one tool call (F04, SD03). Every refusal is an error result for the model, not
     * an exception, so the model can correct itself (UC04 1b). A call is refused when no tool has that
     * name, the session's mode doesn't allow it, or its input doesn't match the tool's schema. A tool
     * that throws gives an error result too, and the exception is logged, because it is a bug.
     */
    public ToolResult execute(ContentBlock.ToolUse call, AgentSession session) {
        AgentMode mode = session.getMode();
        Tool tool = toolsByName.get(call.name());
        if (tool == null) {
            return ToolResult.error("There is no tool named \"" + call.name() + "\". " + available(mode));
        }
        if (!mode.allowedTools().contains(tool.name())) {
            return ToolResult.error(tool.name() + " isn't available in " + mode.name() + " mode. " + available(mode));
        }
        Map<String, Object> input = call.input() == null ? Map.of() : call.input();
        List<String> problems = inputProblems(tool.inputSchema(), input);
        if (!problems.isEmpty()) {
            return ToolResult.error("Invalid input for " + tool.name() + ": " + String.join("; ", problems));
        }
        try {
            return tool.execute(input, session);
        } catch (RuntimeException e) {
            LOG.warn("Tool {} failed", tool.name(), e);
            return ToolResult.error(tool.name() + " failed: " + e.getMessage());
        }
    }

    private String available(AgentMode mode) {
        return "Tools you can use: " + toolsFor(mode).stream().map(Tool::name).collect(Collectors.joining(", ")) + ".";
    }

    /**
     * Problems with the input, checked against the top level of the tool's JSON Schema: required fields
     * are present, and each field given has the schema's type and, for an enum, one of its values.
     */
    static List<String> inputProblems(Map<String, Object> schema, Map<String, Object> input) {
        Map<?, ?> properties = schema.get("properties") instanceof Map<?, ?> map ? map : Map.of();
        List<?> required = schema.get("required") instanceof List<?> list ? list : List.of();
        List<String> problems = new ArrayList<>();
        for (Object field : required) {
            if (input.get(String.valueOf(field)) == null) {
                problems.add(field + " is required");
            }
        }
        input.forEach((field, value) -> {
            if (value == null || !(properties.get(field) instanceof Map<?, ?> property)) {
                return; // Missing values were reported above; unknown fields are ignored.
            }
            Object type = property.get("type");
            if (type != null && !hasType(value, String.valueOf(type))) {
                problems.add(field + " must be " + (type.equals("integer") ? "an integer" : "a " + type));
            } else if (property.get("enum") instanceof List<?> values && !values.contains(value)) {
                problems.add(field + " must be one of " + values);
            }
        });
        return problems;
    }

    /** JSON types as they arrive in a parsed Map. A whole number may arrive as 2.0, so it counts as an integer. */
    private static boolean hasType(Object value, String type) {
        return switch (type) {
            case "string" -> value instanceof String;
            case "boolean" -> value instanceof Boolean;
            case "integer" -> value instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue());
            case "number" -> value instanceof Number;
            case "array" -> value instanceof List<?>;
            case "object" -> value instanceof Map<?, ?>;
            default -> true;
        };
    }
}
