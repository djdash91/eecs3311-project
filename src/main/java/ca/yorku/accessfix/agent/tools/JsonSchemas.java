package ca.yorku.accessfix.agent.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tiny helpers for writing tool input schemas as Java maps. */
public final class JsonSchemas {

    private JsonSchemas() {
    }

    public static Map<String, Object> string(String description) {
        return Map.of("type", "string", "description", description);
    }

    public static Map<String, Object> stringEnum(String description, List<String> values) {
        return Map.of("type", "string", "description", description, "enum", values);
    }

    public static Map<String, Object> integer(String description) {
        return Map.of("type", "integer", "description", description);
    }

    public static Map<String, Object> arrayOf(Map<String, Object> items, String description) {
        return Map.of("type", "array", "description", description, "items", items);
    }

    /** An object schema. {@code properties} keeps its order if you pass a LinkedHashMap. */
    public static Map<String, Object> object(Map<String, Object> properties, String... required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.of(required));
        return schema;
    }
}
