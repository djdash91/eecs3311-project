package ca.yorku.accessfix.domain;

/**
 * The user's answer to a fix proposal. Only rejections are stored on the audit and given to the agent (F11).
 */
public record Decision(FixProposal proposal, boolean approved, String reason) {

    public static final String NO_REASON = "no reason given";

    public static Decision approve(FixProposal proposal) {
        return new Decision(proposal, true, null);
    }

    public static Decision reject(FixProposal proposal, String reason) {
        String cleaned = (reason == null || reason.isBlank()) ? NO_REASON : reason.trim();
        return new Decision(proposal, false, cleaned);
    }
}
