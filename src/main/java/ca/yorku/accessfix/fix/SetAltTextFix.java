package ca.yorku.accessfix.fix;

/**
 * Sets an image's alt text (FixType.SET_ALT_TEXT). An empty value marks a decorative image.
 *
 * <p>TODO(F08): SetAriaLabelFix ("aria-label") and SetLanguageFix ("lang" on the html element)
 * are the same shape. Create them yourself following this class.
 */
public class SetAltTextFix extends AbstractAttributeFix {

    public SetAltTextFix(String issueId, String elementId, String altText) {
        super(issueId, elementId, "alt", altText);
    }
}
