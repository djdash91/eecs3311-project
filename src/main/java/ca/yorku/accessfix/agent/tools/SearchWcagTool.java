package ca.yorku.accessfix.agent.tools;

import ca.yorku.accessfix.agent.AgentSession;
import ca.yorku.accessfix.wcag.WcagCriterion;
import ca.yorku.accessfix.wcag.WcagKnowledgeBase;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** WORKED EXAMPLE of a Tool: lets the agent search the WCAG knowledge base (F05, SD03). */
@Component
public class SearchWcagTool implements Tool {

    public static final String NAME = "search_wcag";

    private final WcagKnowledgeBase knowledgeBase;

    public SearchWcagTool(WcagKnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Search the WCAG 2.2 Level A and AA success criteria by keywords or criterion number. "
                + "Use this before stating what a WCAG criterion requires. Returns matching criteria with "
                + "number, title, level, summary and W3C link, or says that nothing matched.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return JsonSchemas.object(
                Map.of("query", JsonSchemas.string("Keywords or a criterion number, for example 'form labels' or '1.1.1'")),
                "query");
    }

    @Override
    public ToolResult execute(Map<String, Object> input, AgentSession session) {
        Object query = input.get("query");
        if (!(query instanceof String text) || text.isBlank()) {
            return ToolResult.error("query must be a non-empty string");
        }
        List<WcagCriterion> matches = knowledgeBase.search(text);
        if (matches.isEmpty()) {
            return ToolResult.ok("No WCAG criteria matched \"" + text + "\". Don't cite a criterion you couldn't find.");
        }
        return ToolResult.ok(matches.stream().map(SearchWcagTool::format).collect(Collectors.joining("\n\n")));
    }

    private static String format(WcagCriterion c) {
        return c.number() + " " + c.title() + " (Level " + c.level() + ")\n"
                + c.summary() + "\nHelps: " + c.whoItHelps() + "\nSource: " + c.url();
    }
}
