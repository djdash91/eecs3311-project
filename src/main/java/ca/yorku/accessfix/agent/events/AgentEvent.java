package ca.yorku.accessfix.agent.events;

/**
 * Something the GUI or CLI should show while the agent works (Observer pattern).
 *
 * @param data optional details, for example a FixProposal with its before-and-after code
 */
public record AgentEvent(String auditId, Type type, String message, Object data) {

    public enum Type {
        PROGRESS,
        TOOL_CALL,
        FIX_PROPOSED,
        FIX_VERIFIED,
        FIX_ROLLED_BACK,
        ESCALATED,
        SESSION_FINISHED,
        ERROR
    }

    public static AgentEvent of(String auditId, Type type, String message) {
        return new AgentEvent(auditId, type, message, null);
    }
}
