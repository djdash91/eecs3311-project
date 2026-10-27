package ca.yorku.accessfix.fix;

import ca.yorku.accessfix.domain.PageSnapshot;

/**
 * Adds a visible label element for a form field (FixType.ADD_LABEL). This doesn't fit
 * AbstractAttributeFix because it adds an element, so it implements FixCommand directly.
 *
 * <p>TODO(F08): implement.
 * <ul>
 *   <li>execute: the label needs to point at the field. If the field has no id attribute, you
 *       must add one too, and undo must remove both.</li>
 *   <li>undo: remove exactly what execute added. Your round-trip test proves it.</li>
 * </ul>
 */
public class AddLabelFix implements FixCommand {

    private final String issueId;
    private final String elementId;
    private final String labelText;

    public AddLabelFix(String issueId, String elementId, String labelText) {
        this.issueId = issueId;
        this.elementId = elementId;
        this.labelText = labelText;
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
        throw new UnsupportedOperationException("TODO: AddLabelFix.execute");
    }

    @Override
    public void undo(PageSnapshot snapshot) {
        throw new UnsupportedOperationException("TODO: AddLabelFix.undo");
    }
}
