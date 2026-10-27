package ca.yorku.accessfix.agent.mode;

import ca.yorku.accessfix.agent.AgentSession;
import ca.yorku.accessfix.agent.tools.InspectElementTool;
import ca.yorku.accessfix.agent.tools.ListIssuesTool;
import ca.yorku.accessfix.agent.tools.SearchWcagTool;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * WORKED EXAMPLE of a mode: answers questions with read-only tools (F04).
 * The prompt is a first draft. Expect to change it after your KUMA tests in Stage 3, and log why.
 */
@Component
public class AskMode implements AgentMode {

    public static final Set<String> READ_ONLY_TOOLS =
            Set.of(ListIssuesTool.NAME, InspectElementTool.NAME, SearchWcagTool.NAME);

    @Override
    public String name() {
        return "ask";
    }

    @Override
    public String systemPrompt() {
        return """
                You are AccessFix, an assistant that answers questions about one web page's accessibility audit.
                Use the tools to look at the audit's issues, inspect elements and search the WCAG reference.
                Base every answer on tool results. If a tool returns nothing relevant, say so instead of guessing.
                Never invent issues, issue counts or WCAG criteria: counts come only from list_issues.
                You can't change the page in this mode. If the user asks for a change, tell them changes are made in Fix mode.
                Automated checks find only some barriers, so don't describe the page as fully accessible.
                Keep answers short and in plain language.
                """;
    }

    @Override
    public Set<String> allowedTools() {
        return READ_ONLY_TOOLS;
    }

    @Override
    public int stepLimit() {
        return 8;
    }

    @Override
    public boolean onTurnEnd(AgentSession session) {
        return true; // Ask mode only stops when the model answers or the step limit is reached.
    }
}
