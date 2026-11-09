package ca.yorku.accessfix.agent;

import ca.yorku.accessfix.agent.events.AgentEvent;
import ca.yorku.accessfix.agent.events.AgentEventPublisher;
import ca.yorku.accessfix.agent.tools.ToolResult;
import ca.yorku.accessfix.common.InvalidInputException;
import ca.yorku.accessfix.domain.Decision;
import ca.yorku.accessfix.domain.FixProposal;
import ca.yorku.accessfix.domain.VerificationResult;
import ca.yorku.accessfix.fix.FixService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Holds a fix proposal until the user decides, then applies or records it (F08, SD05, SD06).
 */
@Service
public class ApprovalGate {

    /**
     * What the GUI or CLI shows for a proposal: the fix, and the affected code before and after it.
     * It is the data of the FIX_PROPOSED event.
     */
    public record ProposedFix(FixProposal proposal, String beforeAndAfter) {
    }

    private final FixService fixService;
    private final MemoryManager memory;
    private final AgentEventPublisher events;
    private final int retryLimit;

    public ApprovalGate(FixService fixService, MemoryManager memory, AgentEventPublisher events,
                        @Value("${accessfix.agent.retry-limit:2}") int retryLimit) {
        this.fixService = fixService;
        this.memory = memory;
        this.events = events;
        this.retryLimit = retryLimit;
    }

    /** Failed fixes allowed per issue before it is escalated (F08: two). ProposeFixTool refuses fixes beyond it. */
    public int retryLimit() {
        return retryLimit;
    }

    /**
     * Stores the proposal in the session and publishes it with the affected code before and after,
     * so the user can review it (F08, SD05). The preview tries the fix on a copy of the page, so
     * nothing changes yet.
     *
     * @throws InvalidInputException if the fix can't be applied (from FixService.preview); nothing is stored
     */
    public void requestApproval(AgentSession session, FixProposal proposal) {
        String beforeAndAfter = fixService.preview(session.getAuditId(), proposal);
        session.setPendingProposal(proposal);
        events.publish(new AgentEvent(session.getAuditId(), AgentEvent.Type.FIX_PROPOSED,
                "Fix proposed for " + proposal.issueId() + ": " + proposal.fixType().toolName()
                        + " \"" + proposal.value() + "\"",
                new ProposedFix(proposal, beforeAndAfter)));
    }

    /**
     * Carries out the user's decision on the pending proposal and returns what the model is told
     * (F08, F09, F11, SD06). A rejection is remembered with its reason. An approved fix is applied
     * and verified; a fix that fails verification counts towards the retry limit, and reaching the
     * limit escalates the issue. Either way, the proposal stops being pending.
     */
    public ToolResult decide(AgentSession session, Decision decision) {
        String auditId = session.getAuditId();
        FixProposal proposal = decision.proposal();
        try {
            if (!decision.approved()) {
                memory.recordDecision(auditId, decision);
                return ToolResult.error("The user rejected this fix. Reason: " + decision.reason()
                        + ". Don't propose it again. Try a different fix for this issue, escalate it, "
                        + "or move on to the next issue.");
            }
            VerificationResult result;
            try {
                result = fixService.applyVerified(auditId, proposal);
            } catch (InvalidInputException e) {
                return ToolResult.error("The fix was refused and not applied: " + e.getMessage());
            }
            if (result.kept()) {
                events.publish(new AgentEvent(auditId, AgentEvent.Type.FIX_VERIFIED,
                        "Verified: " + proposal.issueId(), result));
                return ToolResult.ok(result.reason() + " The latest scan has "
                        + result.scanAfter().issueCount() + " issues.");
            }
            events.publish(new AgentEvent(auditId, AgentEvent.Type.FIX_ROLLED_BACK,
                    proposal.issueId() + ": " + result.reason(), result));
            return ToolResult.error(result.reason() + " " + recordFailure(auditId, proposal.issueId(), result.reason()));
        } finally {
            session.setPendingProposal(null);
        }
    }

    /** Counts the failure and escalates the issue at the retry limit. Returns what the model is told. */
    private String recordFailure(String auditId, String issueId, String reason) {
        int failures = memory.recordFailure(auditId, issueId);
        if (failures < retryLimit) {
            return "This was failed fix " + failures + " of " + retryLimit + " allowed for " + issueId + ".";
        }
        memory.recordEscalation(auditId, issueId,
                failures + " fixes failed verification. The last one: " + reason);
        events.publish(AgentEvent.of(auditId, AgentEvent.Type.ESCALATED,
                issueId + " was escalated after " + failures + " failed fixes"));
        return issueId + " has now failed " + failures + " times, so it was escalated for a person to fix. "
                + "Don't propose more fixes for it.";
    }
}
