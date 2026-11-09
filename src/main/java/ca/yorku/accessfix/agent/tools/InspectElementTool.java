package ca.yorku.accessfix.agent.tools;

import ca.yorku.accessfix.agent.AgentSession;
import ca.yorku.accessfix.agent.IssueAssistant;
import ca.yorku.accessfix.domain.Audit;
import ca.yorku.accessfix.domain.PageSnapshot;
import ca.yorku.accessfix.llm.ContentBlock;
import ca.yorku.accessfix.persistence.AuditRepository;
import ca.yorku.accessfix.scan.RuleSupport;
import java.util.Map;
import java.util.Optional;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/**
 * Returns an element's code and, for an image, its screenshot (read-only), so the agent can write
 * alt text in Fix mode.
 *
 * <p>The screenshot is the result's payload, and ToolResultBlock carries it to the model as an image
 * next to the text (a recorded design change).
 */
@Component
public class InspectElementTool implements Tool {

    public static final String NAME = "inspect_element";

    /** The text around the element is cut to this many characters, so the result stays short. */
    static final int MAX_TEXT_LENGTH = 300;

    private final AuditRepository repository;

    public InspectElementTool(AuditRepository repository) {
        this.repository = repository;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Show the HTML of one element (by element ID from list_issues) and some surrounding text. "
                + "For images, also returns a screenshot of the image when one is available.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return JsonSchemas.object(
                Map.of("elementId", JsonSchemas.string("The element ID, for example 'e12'")),
                "elementId");
    }

    /**
     * The element's code, whether it is hidden, and the text around it (F04, F08). For an image with
     * a screenshot, the screenshot goes with the result as its payload, and AgentController sends it
     * to the model with the text. An unknown element ID is an error.
     */
    @Override
    public ToolResult execute(Map<String, Object> input, AgentSession session) {
        Optional<Audit> found = repository.findById(session.getAuditId());
        if (found.isEmpty()) {
            return ToolResult.error("The audit " + session.getAuditId() + " no longer exists.");
        }
        PageSnapshot snapshot = found.get().getSnapshot();
        String elementId = String.valueOf(input.get("elementId")).trim();
        Document doc = snapshot.parse();
        Element element = PageSnapshot.findElement(doc, elementId);
        if (element == null) {
            return ToolResult.error("There is no element with ID \"" + elementId
                    + "\". Use the element IDs that list_issues shows.");
        }
        String around = IssueAssistant.textAround(doc, element);
        Optional<byte[]> screenshot = snapshot.screenshotOf(elementId);
        String text = "Element " + elementId + ":\n" + RuleSupport.snippet(element)
                + "\nHidden from view: " + (snapshot.isHidden(elementId) ? "yes" : "no")
                + "\nText around it: " + (around.isEmpty() ? "(none)" : cut(around))
                + (screenshot.isPresent() ? "\nA screenshot of the image is attached." : "");
        return screenshot
                .map(png -> new ToolResult(ToolResult.Status.OK, text, new ContentBlock.Image("image/png", png)))
                .orElse(ToolResult.ok(text));
    }

    private static String cut(String text) {
        return text.length() <= MAX_TEXT_LENGTH ? text : text.substring(0, MAX_TEXT_LENGTH) + "…";
    }
}
