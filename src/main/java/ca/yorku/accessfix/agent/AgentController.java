package ca.yorku.accessfix.agent;

import ca.yorku.accessfix.agent.events.AgentEvent;
import ca.yorku.accessfix.agent.events.AgentEventPublisher;
import ca.yorku.accessfix.agent.mode.AgentMode;
import ca.yorku.accessfix.agent.tools.Tool;
import ca.yorku.accessfix.agent.tools.ToolRegistry;
import ca.yorku.accessfix.agent.tools.ToolResult;
import ca.yorku.accessfix.domain.Decision;
import ca.yorku.accessfix.domain.RemediationPlan;
import ca.yorku.accessfix.llm.ContentBlock;
import ca.yorku.accessfix.llm.LLMClient;
import ca.yorku.accessfix.llm.LLMException;
import ca.yorku.accessfix.llm.LLMRequest;
import ca.yorku.accessfix.llm.LLMResponse;
import ca.yorku.accessfix.llm.Message;
import ca.yorku.accessfix.llm.ToolDefinition;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Runs the agent loop that all three modes share (F04, SD03). The mode decides the instructions,
 * tools, step limit and when to stop; this class decides nothing mode-specific (State pattern).
 */
@Service
public class AgentController {

    /**
     * Added to the instructions for the last step, so that when the step limit is reached the agent
     * still says what it found and what it couldn't check (UC04 1c, UC08 *b).
     */
    static final String LAST_STEP_NOTE = """

            This is the last step of this run: don't call any more tools. Answer with what you have found \
            or done so far, and say what you couldn't check or what is left to do.""";

    /** Sent back for tool calls made on the last step, so the conversation stays valid for the next run. */
    static final String NOT_RUN = "Not run: the step limit for this run was reached.";

    /** Shown when the step limit is reached and the model gave no text either. */
    static final String STEP_LIMIT_ANSWER = "I reached the step limit for this run before I could finish.";

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
     * Runs the loop for the session's current mode until the model answers, the mode stops the run,
     * or the step limit is reached (F04, SD03). The audit is busy while it runs.
     *
     * <p>A null message continues the conversation instead of starting a new turn: the facade adds the
     * user's decision as a tool result, then calls run(session, null) (SD06).
     *
     * <p>A PENDING tool result means a fix waits for the user (SD05). Its call ID is stored on the
     * session, the turn's other results are held there, and nothing is sent until the user decides.
     *
     * @throws LLMException if the model can't be reached; an ERROR event is published first, so the
     *                      GUI can show the error with Retry (UC04 1d)
     */
    public AgentResult run(AgentSession session, String message) {
        AgentMode mode = session.getMode();
        if (mode == null) {
            throw new IllegalStateException("Set the session's mode before running the agent.");
        }
        String auditId = session.getAuditId();
        session.setRunning(true);
        try {
            if (message != null) {
                session.addUserContent(List.of(new ContentBlock.Text(message)));
            }
            String instructions = mode.systemPrompt() + decisionsText(memory.loadDecisions(auditId));
            List<ToolDefinition> definitions = tools.toolsFor(mode).stream().map(Tool::definition).toList();
            RemediationPlan plan = null;

            for (int step = 1; step <= mode.stepLimit(); step++) {
                boolean lastStep = step == mode.stepLimit();
                String system = lastStep ? instructions + LAST_STEP_NOTE : instructions;
                LLMResponse reply = send(auditId, new LLMRequest(system, session.getMessages(), definitions, 0));
                session.addMessage(Message.assistant(reply.content()));
                events.publish(AgentEvent.of(auditId, AgentEvent.Type.PROGRESS,
                        "Step " + step + " of at most " + mode.stepLimit() + " (" + mode.name() + " mode)"));

                if (!reply.hasToolCalls()) {
                    return finish(session, new AgentResult(reply.text(), plan, session.getPendingProposal(), lastStep, null));
                }
                if (lastStep) {
                    session.addUserContent(reply.toolCalls().stream()
                            .map(call -> new ContentBlock.ToolResultBlock(call.id(), NOT_RUN, true))
                            .toList());
                    break;
                }

                List<ContentBlock.ToolResultBlock> results = new ArrayList<>();
                boolean pending = false;
                for (ContentBlock.ToolUse call : reply.toolCalls()) {
                    events.publish(new AgentEvent(auditId, AgentEvent.Type.TOOL_CALL, call.name(), call.input()));
                    ToolResult result = tools.execute(call, session);
                    if (result.status() == ToolResult.Status.PENDING && !pending) {
                        pending = true;
                        session.setPendingToolUseId(call.id());
                        continue;
                    }
                    if (result.status() == ToolResult.Status.PENDING) {
                        result = ToolResult.error("Only one fix can wait for the user at a time. "
                                + "Propose this one again after the user decides on the first.");
                    }
                    if (result.status() == ToolResult.Status.ACCEPTED && result.payload() instanceof RemediationPlan accepted) {
                        plan = accepted;
                    }
                    ContentBlock.Image image = result.payload() instanceof ContentBlock.Image attached ? attached : null;
                    results.add(new ContentBlock.ToolResultBlock(call.id(), result.content(), result.isError(), image));
                }
                if (pending) {
                    results.forEach(session::holdToolResult);
                } else {
                    session.addUserContent(results);
                }
                if (!mode.onTurnEnd(session)) {
                    return finish(session, new AgentResult(reply.text(), plan, session.getPendingProposal(), false, null));
                }
            }
            String text = lastText(session);
            return finish(session, new AgentResult(text.isBlank() ? STEP_LIMIT_ANSWER : text, plan,
                    session.getPendingProposal(), true, null));
        } finally {
            session.setRunning(false);
        }
    }

    /**
     * Publishes SESSION_FINISHED when the run ends without a fix waiting for the user (SD06), so the
     * GUI knows the agent has stopped. A run paused for a decision hasn't finished.
     */
    private AgentResult finish(AgentSession session, AgentResult result) {
        if (!result.fixPending()) {
            events.publish(AgentEvent.of(session.getAuditId(), AgentEvent.Type.SESSION_FINISHED,
                    result.stepLimitReached() ? "Stopped at the step limit" : "Finished"));
        }
        return result;
    }

    private LLMResponse send(String auditId, LLMRequest request) {
        try {
            return llm.send(request);
        } catch (LLMException e) {
            events.publish(AgentEvent.of(auditId, AgentEvent.Type.ERROR, e.getMessage()));
            throw e;
        }
    }

    /**
     * The user's earlier rejections, for the instructions (F11). The reasons are the user's own words,
     * so the agent is told to respect them, not to follow them as instructions about other things.
     */
    static String decisionsText(List<Decision> decisions) {
        if (decisions.isEmpty()) {
            return "";
        }
        StringBuilder text = new StringBuilder("""


                The user rejected these fixes earlier. Don't propose them again, and take the reasons into \
                account in your answers and plans:""");
        for (Decision decision : decisions) {
            text.append("\n- ").append(decision.proposal().fixType().toolName())
                    .append(" on ").append(decision.proposal().issueId())
                    .append(" with \"").append(decision.proposal().value()).append("\"")
                    .append(". Reason: ").append(decision.reason());
        }
        return text.toString();
    }

    /** The text of the model's last reply, if it had any. */
    private static String lastText(AgentSession session) {
        List<Message> messages = session.getMessages();
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).role() == Message.Role.ASSISTANT) {
                return new LLMResponse(messages.get(i).content(), "").text();
            }
        }
        return "";
    }
}
