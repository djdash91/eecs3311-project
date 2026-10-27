package ca.yorku.accessfix.agent.tools;

import ca.yorku.accessfix.agent.AgentSession;
import ca.yorku.accessfix.persistence.AuditRepository;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Lists the issues in the audit's latest scan (read-only). Issue counts the agent reports must come
 * from here, because they come from the rule engine.
 */
@Component
public class ListIssuesTool implements Tool {

    public static final String NAME = "list_issues";

    private final AuditRepository repository;

    public ListIssuesTool(AuditRepository repository) {
        this.repository = repository;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "List the accessibility issues found in the latest scan of this page, with each issue's ID, "
                + "rule, WCAG criterion and message. Also lists escalated issues and rules that didn't run.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        // Optional filter. Decide whether you want it, and test both cases.
        return JsonSchemas.object(Map.of("ruleId", JsonSchemas.string("Optional: only issues of this rule")));
    }

    /**
     * TODO(F04): load the audit (session.getAuditId()), format the latest scan's issues as compact text,
     * include escalated issue IDs and rulesNotRun. Keep the output short: it uses context on every turn.
     */
    @Override
    public ToolResult execute(Map<String, Object> input, AgentSession session) {
        throw new UnsupportedOperationException("TODO: ListIssuesTool.execute");
    }
}
