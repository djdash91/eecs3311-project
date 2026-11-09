package ca.yorku.accessfix.agent.tools;

import ca.yorku.accessfix.agent.AgentSession;
import ca.yorku.accessfix.domain.Audit;
import ca.yorku.accessfix.domain.FixType;
import ca.yorku.accessfix.domain.PlanItem;
import ca.yorku.accessfix.domain.RemediationPlan;
import ca.yorku.accessfix.persistence.AuditRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/** The agent submits its remediation plan here (F06, SD04). Our code validates it. */
@Component
public class SubmitPlanTool implements Tool {

    public static final String NAME = "submit_plan";

    /** The fixType value for an issue a person has to fix. */
    public static final String MANUAL = "manual";

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

    /** An array "items" of objects, each with issueId, priority, reason and fixType, all required. */
    @Override
    public Map<String, Object> inputSchema() {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("issueId", JsonSchemas.string("The issue ID from list_issues"));
        item.put("priority", JsonSchemas.integer("1 is the most urgent. Issues may share a priority"));
        item.put("reason", JsonSchemas.string("One short sentence: who is affected, and why this priority"));
        item.put("fixType", JsonSchemas.stringEnum("The fix to use, or 'manual' if a person has to fix it", fixTypeNames()));
        return JsonSchemas.object(
                Map.of("items", JsonSchemas.arrayOf(
                        JsonSchemas.object(item, "issueId", "priority", "reason", "fixType"),
                        "One item for every issue in the latest scan")),
                "items");
    }

    /** Every FixType's tool name, plus "manual". */
    static List<String> fixTypeNames() {
        return Stream.concat(Arrays.stream(FixType.values()).map(FixType::toolName), Stream.of(MANUAL)).toList();
    }

    /**
     * Turns the items into a RemediationPlan and validates it against the latest scan (F06, SD04).
     * An item that can't be read, for example with an unknown fix type, is an error for the model,
     * like a plan that fails validation. A valid plan is returned as ACCEPTED, with the plan as the
     * payload; the facade saves it, not yet approved.
     */
    @Override
    public ToolResult execute(Map<String, Object> input, AgentSession session) {
        Optional<Audit> audit = repository.findById(session.getAuditId());
        if (audit.isEmpty()) {
            return ToolResult.error("The audit " + session.getAuditId() + " no longer exists.");
        }
        List<?> rawItems = input.get("items") instanceof List<?> list ? list : List.of();
        List<PlanItem> items = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < rawItems.size(); i++) {
            String where = "Item " + (i + 1);
            if (!(rawItems.get(i) instanceof Map<?, ?> raw)) {
                errors.add(where + " isn't an object with issueId, priority, reason and fixType.");
                continue;
            }
            Object issueId = raw.get("issueId");
            Object priority = raw.get("priority");
            Object reason = raw.get("reason");
            Object fixType = raw.get("fixType");
            if (!(issueId instanceof String id) || id.isBlank()) {
                errors.add(where + " has no issueId.");
                continue;
            }
            where += " (" + id + ")";
            if (!(priority instanceof Number number) || number.doubleValue() != Math.rint(number.doubleValue())) {
                errors.add(where + ": priority must be a whole number.");
                continue;
            }
            if (!(fixType instanceof String typeName) || !fixTypeNames().contains(typeName.trim())) {
                errors.add(where + ": fixType must be one of " + fixTypeNames() + ".");
                continue;
            }
            FixType type = typeName.trim().equals(MANUAL) ? null : FixType.fromToolName(typeName).orElseThrow();
            items.add(new PlanItem(id.trim(), number.intValue(), reason instanceof String text ? text : null, type));
        }
        if (!errors.isEmpty()) {
            return ToolResult.error("The plan couldn't be read. Correct these items and submit the whole plan again:\n- "
                    + String.join("\n- ", errors));
        }
        RemediationPlan plan = new RemediationPlan(items);
        List<String> problems = plan.validate(audit.get().latestScan());
        if (!problems.isEmpty()) {
            return ToolResult.error("The plan is invalid. Correct it and submit the whole plan again:\n- "
                    + String.join("\n- ", problems));
        }
        long manual = items.stream().filter(PlanItem::isManual).count();
        return ToolResult.accepted("Plan accepted: " + items.size() + " items, " + manual
                + " of them manual. The user will review it.", plan);
    }
}
