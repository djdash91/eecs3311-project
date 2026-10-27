package ca.yorku.accessfix.agent;

import ca.yorku.accessfix.domain.FixProposal;
import ca.yorku.accessfix.domain.RemediationPlan;
import java.util.Map;

/**
 * What one run returns to the facade (and then to the GUI or CLI).
 *
 * @param answer           the model's final text, if any
 * @param plan             the accepted plan, in Plan mode
 * @param pendingProposal  the fix waiting for approval, in Fix mode; null if none
 * @param stepLimitReached true if the run stopped at the mode's step limit
 * @param issueCounts      issue counts by rule from the latest scan, set when a fix session ends
 */
public record AgentResult(String answer, RemediationPlan plan, FixProposal pendingProposal,
                          boolean stepLimitReached, Map<String, Integer> issueCounts) {

    public boolean fixPending() {
        return pendingProposal != null;
    }

    public AgentResult withIssueCounts(Map<String, Integer> counts) {
        return new AgentResult(answer, plan, pendingProposal, stepLimitReached, counts);
    }
}
