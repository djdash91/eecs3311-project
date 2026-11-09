package ca.yorku.accessfix.fix;

/**
 * Sets the page language (FixType.SET_LANGUAGE). The element is the html element, which is where
 * PageLanguageRule points its issue, so the issue's element ID is passed straight through.
 */
public class SetLanguageFix extends AbstractAttributeFix {

    public SetLanguageFix(String issueId, String elementId, String language) {
        super(issueId, elementId, "lang", language);
    }

    /** For JSON libraries only. */
    protected SetLanguageFix() {
        this(null, null, null);
    }
}
