package ca.yorku.accessfix.agent;

import ca.yorku.accessfix.agent.events.AgentEventPublisher;
import ca.yorku.accessfix.agent.tools.ToolResult;
import ca.yorku.accessfix.domain.Decision;
import ca.yorku.accessfix.domain.FixProposal;
import ca.yorku.accessfix.fix.FixService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Holds a fix proposal until the user decides, then applies or records it (F08, SD05, SD06).
 */
@Service
public class ApprovalGate {

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

    /**
     * TODO(F08, SD05): get the before-and-after code from fixService.preview(), store the proposal
     * in the session, and publish a FIX_PROPOSED event with the preview as data.
     */
    public void requestApproval(AgentSession session, FixProposal proposal) {
        throw new UnsupportedOperationException("TODO: ApprovalGate.requestApproval");
    }

    /**
     * TODO(F08, F09, F11, SD06): implement.
     * <ul>
     *   <li>Rejected: memory.recordDecision(); return a ToolResult telling the model it was rejected and why.</li>
     *   <li>Approved: fixService.applyVerified(). Rolled back: memory.recordFailure(), and when the count
     *       reaches retryLimit, memory.recordEscalation(). Publish FIX_VERIFIED or FIX_ROLLED_BACK.</li>
     *   <li>Either way, clear the pending proposal from the session.</li>
     * </ul>
     */
    public ToolResult decide(AgentSession session, Decision decision) {
        throw new UnsupportedOperationException("TODO: ApprovalGate.decide");
    }
}
