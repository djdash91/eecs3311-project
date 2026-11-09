package ca.yorku.accessfix.fix;

/**
 * Sets an element's aria-label (FixType.SET_ARIA_LABEL). It can fix unlabelled form fields, links
 * with no name and buttons with no name (F08), because all three rules accept a non-blank aria-label.
 */
public class SetAriaLabelFix extends AbstractAttributeFix {

    public SetAriaLabelFix(String issueId, String elementId, String label) {
        super(issueId, elementId, "aria-label", label);
    }

    /** For JSON libraries only. */
    protected SetAriaLabelFix() {
        this(null, null, null);
    }
}
