package ca.yorku.accessfix.agent.tools;

import ca.yorku.accessfix.agent.AgentSession;
import ca.yorku.accessfix.agent.MemoryManager;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** The agent hands an issue it can't fix to the user (UC08 2b). */
@Component
public class EscalateToHumanTool implements Tool {

    public static final String NAME = "escalate_to_human";

    private final MemoryManager memory;

    public EscalateToHumanTool(MemoryManager memory) {
        this.memory = memory;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Record that an issue needs a person to fix it, with the reason. Use this instead of "
                + "proposing a fix you aren't confident about. Escalated issues appear in the report.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("issueId", JsonSchemas.string("The issue ID from list_issues"));
        properties.put("reason", JsonSchemas.string("Why a person needs to fix it"));
        return JsonSchemas.object(properties, "issueId", "reason");
    }

    /**
     * TODO(F08): check the issue exists in the latest scan, then memory.recordEscalation(). Where will
     * the reason be stored, so the report can show it? (Audit currently keeps only the ID.)
     */
    @Override
    public ToolResult execute(Map<String, Object> input, AgentSession session) {
        throw new UnsupportedOperationException("TODO: EscalateToHumanTool.execute");
    }
}
