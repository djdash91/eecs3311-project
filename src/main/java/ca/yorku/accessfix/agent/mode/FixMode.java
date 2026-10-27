package ca.yorku.accessfix.agent.mode;

import ca.yorku.accessfix.agent.AgentSession;
import ca.yorku.accessfix.agent.tools.EscalateToHumanTool;
import ca.yorku.accessfix.agent.tools.ProposeFixTool;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Proposes one fix at a time and pauses for approval (F08, SD05). */
@Component
public class FixMode implements AgentMode {

    @Override
    public String name() {
        return "fix";
    }

    /**
     * TODO(F08): write the Fix instructions: work through the approved plan, one propose_fix at a time,
     * never repeat a rejected fix, and use escalate_to_human for issues it can't fix.
     */
    @Override
    public String systemPrompt() {
        return "TODO: Fix mode instructions";
    }

    @Override
    public Set<String> allowedTools() {
        Set<String> tools = new HashSet<>(AskMode.READ_ONLY_TOOLS);
        tools.add(ProposeFixTool.NAME);
        tools.add(EscalateToHumanTool.NAME);
        return Set.copyOf(tools);
    }

    @Override
    public int stepLimit() {
        return 20;
    }

    /** TODO(F08, SD05): the run pauses while a proposal is pending. One line, but it is the key to Fix mode. */
    @Override
    public boolean onTurnEnd(AgentSession session) {
        throw new UnsupportedOperationException("TODO: FixMode.onTurnEnd");
    }
}
