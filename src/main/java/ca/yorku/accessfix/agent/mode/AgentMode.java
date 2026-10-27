package ca.yorku.accessfix.agent.mode;

import ca.yorku.accessfix.agent.AgentSession;
import java.util.Set;

/**
 * One agent mode (State pattern, view B). AgentController asks the session's current mode for its
 * instructions, tools, step limit and whether to keep going, so the controller has no mode checks.
 */
public interface AgentMode {

    /** "ask", "plan" or "fix": for logs, events and the UI. */
    String name();

    /** The mode's instructions. Remembered decisions (F11) are added by AgentController. */
    String systemPrompt();

    /** Names of the tools this mode may call. ToolRegistry refuses all others. */
    Set<String> allowedTools();

    /** Maximum model calls in one run. */
    int stepLimit();

    /**
     * Called after each turn in which tools ran. Return true to keep looping, false to stop the run.
     * (A turn without tool calls always ends the run: the model has answered.)
     */
    boolean onTurnEnd(AgentSession session);
}
