package ca.yorku.accessfix.agent.tools;

import ca.yorku.accessfix.agent.AgentSession;
import ca.yorku.accessfix.persistence.AuditRepository;
import java.util.Map;
import org.springframework.stereotype.Component;

/** The agent submits its remediation plan here (F06, SD04). Our code validates it. */
@Component
public class SubmitPlanTool implements Tool {

    public static final String NAME = "submit_plan";

    private final AuditRepository repository;

    public SubmitPlanTool(AuditRepository repository) {
        this.repository = repository;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Submit the ranked remediation plan. Every issue gets a priority, a short reason and a fix type, "
                + "or 'manual'. The plan is validated; if it is invalid you get the errors and can resubmit.";
    }

    /**
     * TODO(F06): design the schema: an array "items" of objects with issueId, priority, reason and
     * fixType. Use JsonSchemas.stringEnum for fixType: every FixType.toolName() plus "manual".
     */
    @Override
    public Map<String, Object> inputSchema() {
        return Map.of("type", "object");
    }

    /**
     * TODO(F06, SD04): parse the items into PlanItems (an unknown fixType or a missing field is an
     * error for the model, not an exception), build a RemediationPlan, call validate(latest scan).
     * Errors: ToolResult.error with every message. Valid: ToolResult.accepted(summary, plan).
     */
    @Override
    public ToolResult execute(Map<String, Object> input, AgentSession session) {
        throw new UnsupportedOperationException("TODO: SubmitPlanTool.execute");
    }
}
