package ca.yorku.accessfix.agent;

import ca.yorku.accessfix.agent.events.AgentEventPublisher;
import ca.yorku.accessfix.agent.tools.ToolRegistry;
import ca.yorku.accessfix.llm.LLMClient;
import org.springframework.stereotype.Service;

/**
 * Runs the agent loop that all three modes share (F04, SD03). The mode decides the instructions,
 * tools, step limit and when to stop; this class decides nothing mode-specific (State pattern).
 */
@Service
public class AgentController {

    private final LLMClient llm;
    private final ToolRegistry tools;
    private final MemoryManager memory;
    private final AgentEventPublisher events;

    public AgentController(LLMClient llm, ToolRegistry tools, MemoryManager memory, AgentEventPublisher events) {
        this.llm = llm;
        this.tools = tools;
        this.memory = memory;
        this.events = events;
    }

    /**
     * TODO(F04, SD03): implement the loop. Suggested steps:
     * <ol>
     *   <li>Mark the session running (and make sure it is unmarked in a finally block).</li>
     *   <li>If message isn't null, add it to the session as a user message.</li>
     *   <li>Load the audit's decisions with memory.loadDecisions() and add them to the system prompt,
     *       so remembered rejections reach Claude in every run (F11).</li>
     *   <li>Loop up to mode.stepLimit() times:
     *     <ul>
     *       <li>Build an LLMRequest from mode.systemPrompt(), the session's messages and
     *           tools.toolsFor(mode); call llm.send(); add the reply to the session; publish a PROGRESS event.</li>
     *       <li>No tool calls: the model has answered. Return.</li>
     *       <li>Otherwise run each call with tools.execute(call, session). A PENDING result means a
     *           fix waits for the user: remember the call's ID in the session, hold the other results,
     *           and don't send anything back yet.</li>
     *       <li>Send the results back as one user message of ToolResultBlocks (unless one is pending).</li>
     *       <li>Ask mode.onTurnEnd(session); false means stop.</li>
     *     </ul>
     *   </li>
     *   <li>Step limit reached: return a result that says so (UC04 1c, UC08 *b).</li>
     * </ol>
     * LLMException: let it propagate after unmarking running, so the user sees an error with Retry.
     *
     * <p>Design note: SD06 resumes a paused run with a tool result, not text. One option: the facade
     * adds the tool_result message to the session, then calls run(session, null) meaning "continue".
     * Record whichever approach you choose in docs/DESIGN-CHANGES.md.
     *
     * <p>Write AgentControllerTest (with FakeLLMClient) before or alongside this method.
     */
    public AgentResult run(AgentSession session, String message) {
        throw new UnsupportedOperationException("TODO: AgentController.run");
    }
}
