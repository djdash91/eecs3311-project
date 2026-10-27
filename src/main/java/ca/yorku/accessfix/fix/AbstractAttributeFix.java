package ca.yorku.accessfix.fix;

import ca.yorku.accessfix.domain.PageSnapshot;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * WORKED EXAMPLE of the Command pattern: shared execute and undo logic for fixes that set one
 * attribute on one element. Subclasses only choose the attribute (see SetAltTextFix).
 */
public abstract class AbstractAttributeFix implements FixCommand {

    protected final String issueId;
    protected final String elementId;
    protected final String attribute;
    protected final String newValue;
    /** The value before execute(), or null if the attribute was absent. Set by execute(). */
    protected String oldValue;
    private boolean executed;

    protected AbstractAttributeFix(String issueId, String elementId, String attribute, String newValue) {
        this.issueId = issueId;
        this.elementId = elementId;
        this.attribute = attribute;
        this.newValue = newValue;
    }

    @Override
    public String issueId() {
        return issueId;
    }

    @Override
    public String description() {
        return "Set " + attribute + "=\"" + newValue + "\" on element " + elementId;
    }

    @Override
    public void execute(PageSnapshot snapshot) {
        Document doc = snapshot.parse();
        Element element = requireElement(doc);
        oldValue = element.hasAttr(attribute) ? element.attr(attribute) : null;
        element.attr(attribute, newValue);
        snapshot.update(doc);
        executed = true;
    }

    @Override
    public void undo(PageSnapshot snapshot) {
        if (!executed) {
            throw new IllegalStateException("undo() called before execute()");
        }
        Document doc = snapshot.parse();
        Element element = requireElement(doc);
        if (oldValue == null) {
            element.removeAttr(attribute);
        } else {
            element.attr(attribute, oldValue);
        }
        snapshot.update(doc);
        executed = false;
    }

    private Element requireElement(Document doc) {
        Element element = PageSnapshot.findElement(doc, elementId);
        if (element == null) {
            throw new IllegalStateException("No element with " + PageSnapshot.ID_ATTR + "=" + elementId);
        }
        return element;
    }
}
