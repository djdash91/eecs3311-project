package ca.yorku.accessfix.agent.mode;

import ca.yorku.accessfix.agent.AgentSession;
import ca.yorku.accessfix.agent.tools.SubmitPlanTool;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Produces a ranked plan with submit_plan (F06, SD04). */
@Component
public class PlanMode implements AgentMode {

    @Override
    public String name() {
        return "plan";
    }

    /** TODO(F06): write the Plan instructions. Say what a good priority and reason look like. */
    @Override
    public String systemPrompt() {
        return "TODO: Plan mode instructions";
    }

    @Override
    public Set<String> allowedTools() {
        Set<String> tools = new HashSet<>(AskMode.READ_ONLY_TOOLS);
        tools.add(SubmitPlanTool.NAME);
        return Set.copyOf(tools);
    }

    @Override
    public int stepLimit() {
        return 10;
    }

    /**
     * TODO(F06): return false once a valid plan has been accepted. How will this method know? Either
     * look at the latest tool result in the session's messages (as Stage 1 describes), or have
     * SubmitPlanTool store the accepted plan on the session. Choose one and note it.
     */
    @Override
    public boolean onTurnEnd(AgentSession session) {
        throw new UnsupportedOperationException("TODO: PlanMode.onTurnEnd");
    }
}
