package ca.yorku.accessfix.agent.tools;

import ca.yorku.accessfix.agent.AgentSession;
import ca.yorku.accessfix.persistence.AuditRepository;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Returns an element's code and, for an image, its screenshot (read-only), so the agent can write
 * alt text in Fix mode.
 *
 * <p>Design question: ToolResult and ToolResultBlock only carry text right now. Claude tool results can
 * also contain images. Extend the types so the screenshot reaches Claude, and record the change.
 */
@Component
public class InspectElementTool implements Tool {

    public static final String NAME = "inspect_element";

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

    /** TODO(F04, F08): find the element in the snapshot; unknown ID returns an error. */
    @Override
    public ToolResult execute(Map<String, Object> input, AgentSession session) {
        throw new UnsupportedOperationException("TODO: InspectElementTool.execute");
    }
}
