package ca.yorku.accessfix.agent.tools;

import ca.yorku.accessfix.agent.AgentSession;
import ca.yorku.accessfix.agent.ApprovalGate;
import ca.yorku.accessfix.agent.IssueAssistant;
import ca.yorku.accessfix.common.InvalidInputException;
import ca.yorku.accessfix.domain.Audit;
import ca.yorku.accessfix.domain.FixProposal;
import ca.yorku.accessfix.domain.FixType;
import ca.yorku.accessfix.domain.Issue;
import ca.yorku.accessfix.persistence.AuditRepository;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** The agent proposes one fix for the user to approve (F08, SD05). */
@Component
public class ProposeFixTool implements Tool {

    public static final String NAME = "propose_fix";

    private final AuditRepository repository;
    private final ApprovalGate approvalGate;
    private final IssueAssistant issueAssistant;

    public ProposeFixTool(AuditRepository repository, ApprovalGate approvalGate, IssueAssistant issueAssistant) {
        this.repository = repository;
        this.approvalGate = approvalGate;
        this.issueAssistant = issueAssistant;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Propose one fix for one issue. The user reviews a before-and-after view and approves or "
                + "rejects it; you then receive the decision and the verification result.";
    }

    /** issueId, fixType (one of the FixType names) and value, all required. */
    @Override
    public Map<String, Object> inputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("issueId", JsonSchemas.string("The issue ID from list_issues"));
        properties.put("fixType", JsonSchemas.stringEnum("The kind of fix",
                Arrays.stream(FixType.values()).map(FixType::toolName).toList()));
        properties.put("value", JsonSchemas.string(
                "The alt text (empty only for a decorative image), label text, aria-label, language code or page title"));
        return JsonSchemas.object(properties, "issueId", "fixType", "value");
    }

    /**
     * Checks the proposal, then hands it to ApprovalGate and returns PENDING, so the run pauses until
     * the user decides (F08, SD05). It is refused with a reason the model can act on when another fix
     * is already waiting, the issue isn't in the latest scan, is escalated or has reached the retry
     * limit, the fix type can't fix the issue's rule, the value fails its checks, or the user already
     * rejected the same fix.
     */
    @Override
    public ToolResult execute(Map<String, Object> input, AgentSession session) {
        if (session.getPendingProposal() != null) {
            return ToolResult.error("Another fix is waiting for the user. Wait for that decision first.");
        }
        Optional<Audit> found = repository.findById(session.getAuditId());
        if (found.isEmpty()) {
            return ToolResult.error("The audit " + session.getAuditId() + " no longer exists.");
        }
        Audit audit = found.get();
        String issueId = String.valueOf(input.get("issueId")).trim();
        Optional<Issue> issue = audit.latestScan().find(issueId);
        if (issue.isEmpty()) {
            return ToolResult.error("Issue " + issueId + " isn't in the latest scan: it may be fixed already, "
                    + "or the ID is wrong. Use list_issues.");
        }
        if (audit.isEscalated(issueId)) {
            return ToolResult.error("Issue " + issueId + " is escalated: a person will fix it. Move on to another issue.");
        }
        if (audit.failuresFor(issueId) >= approvalGate.retryLimit()) {
            return ToolResult.error("Issue " + issueId + " has reached the limit of " + approvalGate.retryLimit()
                    + " failed fixes. Escalate it with escalate_to_human.");
        }
        Optional<FixType> fixType = FixType.fromToolName(String.valueOf(input.get("fixType")));
        String ruleId = issue.get().ruleId();
        if (fixType.isEmpty() || !fixType.get().canFix(ruleId)) {
            return ToolResult.error("That fix type can't fix " + ruleId + " issues. Use one of: "
                    + FixType.allowedFor(ruleId).stream().map(FixType::toolName).collect(Collectors.joining(", ")) + ".");
        }
        String value = String.valueOf(input.get("value"));
        List<String> problems = valueProblems(fixType.get(), value);
        if (!problems.isEmpty()) {
            return ToolResult.error("The value isn't acceptable: " + String.join(" ", problems));
        }
        FixProposal proposal = new FixProposal(issueId, fixType.get(), value.trim());
        boolean rejectedBefore = audit.getDecisions().stream()
                .anyMatch(decision -> !decision.approved() && decision.proposal().equals(proposal));
        if (rejectedBefore) {
            return ToolResult.error("The user already rejected this exact fix. Propose something different, or escalate the issue.");
        }
        try {
            approvalGate.requestApproval(session, proposal);
        } catch (InvalidInputException e) {
            return ToolResult.error(e.getMessage());
        }
        return ToolResult.pending();
    }

    /**
     * Alt text gets the F07 checks, where empty passes because it marks a decorative image (F08).
     * Every other fix type needs some text.
     */
    private List<String> valueProblems(FixType fixType, String value) {
        if (fixType == FixType.SET_ALT_TEXT) {
            return issueAssistant.checkAltText(value);
        }
        return value.isBlank() ? List.of("The value can't be empty for " + fixType.toolName() + ".") : List.of();
    }
}
