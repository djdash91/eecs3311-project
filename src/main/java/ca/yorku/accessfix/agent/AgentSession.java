package ca.yorku.accessfix.agent;

import ca.yorku.accessfix.agent.mode.AgentMode;
import ca.yorku.accessfix.domain.FixProposal;
import ca.yorku.accessfix.llm.ContentBlock;
import ca.yorku.accessfix.llm.Message;
import java.util.ArrayList;
import java.util.List;

/**
 * The conversation and run state for one audit (State pattern context). MemoryManager keeps one per
 * audit, so sessions can't overwrite each other's mode.
 */
public class AgentSession {

    private final String auditId;
    private final List<Message> messages = new ArrayList<>();
    private AgentMode mode;
    private boolean running;
    private FixProposal pendingProposal;

    /**
     * The ID of the propose_fix tool call waiting for the user (SD05). When the user decides, the
     * decision is sent as the tool_result for exactly this ID.
     */
    private String pendingToolUseId;

    /**
     * Results of other tool calls from the same model turn. SD05 says they are held back and sent
     * together with the decision, because Claude expects all results of one turn in one message.
     */
    private final List<ContentBlock.ToolResultBlock> heldToolResults = new ArrayList<>();

    public AgentSession(String auditId) {
        this.auditId = auditId;
    }

    public String getAuditId() {
        return auditId;
    }

    /** The audit is busy while a run is in progress or a fix waits for approval (Stage 1, 1.1 step 4). */
    public boolean isBusy() {
        return running || pendingProposal != null;
    }

    public AgentMode getMode() {
        return mode;
    }

    public void setMode(AgentMode mode) {
        this.mode = mode;
    }

    public List<Message> getMessages() {
        return List.copyOf(messages);
    }

    public void addMessage(Message message) {
        messages.add(message);
    }

    /** Starts a fresh conversation, for example when the user switches mode. Decide when to call it. */
    public void clearMessages() {
        messages.clear();
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public FixProposal getPendingProposal() {
        return pendingProposal;
    }

    public void setPendingProposal(FixProposal pendingProposal) {
        this.pendingProposal = pendingProposal;
    }

    public String getPendingToolUseId() {
        return pendingToolUseId;
    }

    public void setPendingToolUseId(String pendingToolUseId) {
        this.pendingToolUseId = pendingToolUseId;
    }

    public List<ContentBlock.ToolResultBlock> getHeldToolResults() {
        return List.copyOf(heldToolResults);
    }

    public void holdToolResult(ContentBlock.ToolResultBlock result) {
        heldToolResults.add(result);
    }

    public void clearHeldToolResults() {
        heldToolResults.clear();
    }
}
