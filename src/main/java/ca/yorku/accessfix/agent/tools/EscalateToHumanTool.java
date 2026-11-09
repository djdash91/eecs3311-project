package ca.yorku.accessfix.agent.tools;

import ca.yorku.accessfix.agent.AgentSession;
import ca.yorku.accessfix.agent.MemoryManager;
import ca.yorku.accessfix.agent.events.AgentEvent;
import ca.yorku.accessfix.agent.events.AgentEventPublisher;
import ca.yorku.accessfix.domain.Audit;
import ca.yorku.accessfix.persistence.AuditRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** The agent hands an issue it can't fix to the user (UC08 2b). */
@Component
public class EscalateToHumanTool implements Tool {

    public static final String NAME = "escalate_to_human";

    private final MemoryManager memory;
    private final AuditRepository repository;
    private final AgentEventPublisher events;

    public EscalateToHumanTool(MemoryManager memory, AuditRepository repository, AgentEventPublisher events) {
        this.memory = memory;
        this.repository = repository;
        this.events = events;
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
     * Records the issue as escalated, with the reason, after checking it is in the latest scan (F08,
     * UC08 2b). The reason is stored on the audit next to the escalated ID, so the report shows it.
     */
    @Override
    public ToolResult execute(Map<String, Object> input, AgentSession session) {
        Optional<Audit> audit = repository.findById(session.getAuditId());
        if (audit.isEmpty()) {
            return ToolResult.error("The audit " + session.getAuditId() + " no longer exists.");
        }
        String issueId = String.valueOf(input.get("issueId")).trim();
        String reason = String.valueOf(input.get("reason")).trim();
        if (!audit.get().latestScan().contains(issueId)) {
            return ToolResult.error("Issue " + issueId + " isn't in the latest scan. Use list_issues.");
        }
        if (reason.isEmpty()) {
            return ToolResult.error("Give the reason a person needs to fix " + issueId + ".");
        }
        if (audit.get().isEscalated(issueId)) {
            return ToolResult.ok("Issue " + issueId + " was already escalated. Move on to the next issue.");
        }
        memory.recordEscalation(session.getAuditId(), issueId, reason);
        events.publish(AgentEvent.of(session.getAuditId(), AgentEvent.Type.ESCALATED, issueId + ": " + reason));
        return ToolResult.ok("Issue " + issueId + " is escalated and will be listed in the report for a person "
                + "to fix. Move on to the next issue.");
    }
}
