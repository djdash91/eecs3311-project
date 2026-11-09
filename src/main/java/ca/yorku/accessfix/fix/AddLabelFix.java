package ca.yorku.accessfix.fix;

import ca.yorku.accessfix.domain.PageSnapshot;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Adds a visible label element for a form field (FixType.ADD_LABEL). This doesn't fit
 * AbstractAttributeFix because it adds an element, so it implements FixCommand directly.
 *
 * <p>execute adds {@code <label for="field id">text</label>} just before the field, or before the
 * label that wraps it, because labels can't be nested. If the field has no id, it gets one
 * ("af-" plus its stable ID). The new label gets its own stable ID (the field's plus "-label") so
 * undo can find exactly that label. undo removes the label and restores the field's old id.
 */
public class AddLabelFix implements FixCommand {

    private final String issueId;
    private final String elementId;
    private final String labelText;
    /** The field's id before execute(), or null if it had no id attribute. Set by execute(). */
    private String oldFieldId;
    private boolean executed;

    public AddLabelFix(String issueId, String elementId, String labelText) {
        this.issueId = issueId;
        this.elementId = elementId;
        this.labelText = labelText;
    }

    /** For JSON libraries only. */
    protected AddLabelFix() {
        this(null, null, null);
    }

    @Override
    public String issueId() {
        return issueId;
    }

    @Override
    public String description() {
        return "Add label \"" + labelText + "\" to element " + elementId;
    }

    @Override
    public void execute(PageSnapshot snapshot) {
        Document doc = snapshot.parse();
        Element field = requireElement(doc, elementId);

        oldFieldId = field.hasAttr("id") ? field.attr("id") : null;
        if (field.id().isBlank()) {
            field.attr("id", unusedId(doc, "af-" + elementId));
        }

        Element label = doc.createElement("label")
                .attr("for", field.id())
                .attr(PageSnapshot.ID_ATTR, labelElementId())
                .text(labelText);
        Element wrappingLabel = field.closest("label");
        (wrappingLabel != null ? wrappingLabel : field).before(label);

        snapshot.update(doc);
        executed = true;
    }

    @Override
    public void undo(PageSnapshot snapshot) {
        if (!executed) {
            throw new IllegalStateException("undo() called before execute()");
        }
        Document doc = snapshot.parse();
        Element field = requireElement(doc, elementId);
        requireElement(doc, labelElementId()).remove();
        if (oldFieldId == null) {
            field.removeAttr("id");
        } else {
            field.attr("id", oldFieldId);
        }
        snapshot.update(doc);
        executed = false;
    }

    private String labelElementId() {
        return elementId + "-label";
    }

    /** The candidate, or the candidate with a number added, so the new id doesn't clash with another element's. */
    private static String unusedId(Document doc, String candidate) {
        String id = candidate;
        for (int n = 2; doc.getElementById(id) != null; n++) {
            id = candidate + "-" + n;
        }
        return id;
    }

    private static Element requireElement(Document doc, String id) {
        Element element = PageSnapshot.findElement(doc, id);
        if (element == null) {
            throw new IllegalStateException("No element with " + PageSnapshot.ID_ATTR + "=" + id);
        }
        return element;
    }
}
