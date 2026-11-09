package ca.yorku.accessfix.fix;

/**
 * Sets an image's alt text (FixType.SET_ALT_TEXT). An empty value marks a decorative image.
 * SetAriaLabelFix and SetLanguageFix follow this class.
 */
public class SetAltTextFix extends AbstractAttributeFix {

    public SetAltTextFix(String issueId, String elementId, String altText) {
        super(issueId, elementId, "alt", altText);
    }

    /** For JSON libraries only. */
    protected SetAltTextFix() {
        this(null, null, null);
    }
}
