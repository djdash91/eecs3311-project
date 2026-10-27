package ca.yorku.accessfix.llm;

import java.util.Map;

/**
 * What the model sees about a tool: its name, a description of when to use it, and a JSON Schema for
 * its input. Good descriptions are one of the main things you'll tune in Stage 3.
 */
public record ToolDefinition(String name, String description, Map<String, Object> inputSchema) {
}
