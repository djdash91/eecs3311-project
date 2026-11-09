package ca.yorku.accessfix.agent.tools;

import ca.yorku.accessfix.agent.AgentSession;
import ca.yorku.accessfix.domain.Audit;
import ca.yorku.accessfix.domain.Issue;
import ca.yorku.accessfix.domain.RuleIds;
import ca.yorku.accessfix.domain.ScanResult;
import ca.yorku.accessfix.persistence.AuditRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Lists the issues in the audit's latest scan (read-only). Issue counts the agent reports must come
 * from here, because they come from the rule engine.
 */
@Component
public class ListIssuesTool implements Tool {

    public static final String NAME = "list_issues";

    /** The rule IDs the optional filter accepts. */
    static final List<String> RULE_IDS = List.of(RuleIds.IMAGE_ALT, RuleIds.FORM_LABEL, RuleIds.LINK_NAME,
            RuleIds.BUTTON_NAME, RuleIds.PAGE_LANG, RuleIds.PAGE_TITLE);

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
        // Optional filter: with many issues, the agent can look at one rule at a time.
        return JsonSchemas.object(Map.of("ruleId", JsonSchemas.stringEnum("Optional: only issues of this rule", RULE_IDS)));
    }

    /**
     * The latest scan's issues as compact text, one line each, with escalated issues marked and the
     * rules that didn't run (F04). Snippets are left out to keep it short, because the text stays in
     * the conversation for every later step: inspect_element shows an element's code.
     */
    @Override
    public ToolResult execute(Map<String, Object> input, AgentSession session) {
        Optional<Audit> found = repository.findById(session.getAuditId());
        if (found.isEmpty()) {
            return ToolResult.error("The audit " + session.getAuditId() + " no longer exists.");
        }
        Audit audit = found.get();
        ScanResult scan = audit.latestScan();
        String ruleId = input.get("ruleId") instanceof String text && !text.isBlank() ? text.trim() : null;
        if (ruleId != null && !RULE_IDS.contains(ruleId)) {
            return ToolResult.error("Unknown rule \"" + ruleId + "\". Rules: " + String.join(", ", RULE_IDS) + ".");
        }
        List<Issue> issues = scan.issues().stream()
                .filter(issue -> ruleId == null || issue.ruleId().equals(ruleId))
                .toList();

        StringBuilder text = new StringBuilder("The latest scan found ")
                .append(scan.issueCount()).append(scan.issueCount() == 1 ? " issue" : " issues");
        if (ruleId != null) {
            text.append(", ").append(issues.size()).append(" of them for ").append(ruleId);
        }
        text.append(".\nIssue ID | rule | WCAG | element ID | message\n");
        for (Issue issue : issues) {
            text.append(issue.id()).append(" | ").append(issue.ruleId()).append(" | ").append(issue.criterion())
                    .append(" | ").append(issue.elementId()).append(" | ").append(issue.message());
            if (audit.isEscalated(issue.id())) {
                text.append(" | ESCALATED: a person will fix it, don't propose fixes");
            }
            text.append('\n');
        }
        if (!scan.allRulesRan()) {
            text.append("These rules didn't run, so their issues aren't listed: ")
                    .append(String.join(", ", scan.rulesNotRun())).append(".\n");
        }
        return ToolResult.ok(text.toString().trim());
    }
}
