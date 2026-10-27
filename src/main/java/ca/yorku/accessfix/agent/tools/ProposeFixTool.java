package ca.yorku.accessfix.agent.tools;

import ca.yorku.accessfix.agent.AgentSession;
import ca.yorku.accessfix.agent.ApprovalGate;
import ca.yorku.accessfix.agent.IssueAssistant;
import ca.yorku.accessfix.persistence.AuditRepository;
import java.util.Map;
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

    /** TODO(F08): issueId, fixType (enum of FixType.toolName() values) and value, all required. */
    @Override
    public Map<String, Object> inputSchema() {
        return Map.of("type", "object");
    }

    /**
     * TODO(F08, SD05): refuse with ToolResult.error and a clear reason when:
     * the fix type is unknown or can't fix the issue's rule (FixType.canFix); alt text fails
     * issueAssistant.checkAltText (empty is allowed here for decorative images); the issue isn't in the
     * latest scan; it is escalated; or it has reached the retry limit.
     * Otherwise call approvalGate.requestApproval(session, proposal) and return ToolResult.pending().
     */
    @Override
    public ToolResult execute(Map<String, Object> input, AgentSession session) {
        throw new UnsupportedOperationException("TODO: ProposeFixTool.execute");
    }
}
