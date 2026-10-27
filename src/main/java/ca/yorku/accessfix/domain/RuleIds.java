package ca.yorku.accessfix.domain;

/**
 * IDs of the six rules (F02). Kept in one place because issue IDs, FixType, plan validation
 * and the prompts all refer to them.
 */
public final class RuleIds {
    public static final String IMAGE_ALT = "image-alt";
    public static final String FORM_LABEL = "form-label";
    public static final String LINK_NAME = "link-name";
    public static final String BUTTON_NAME = "button-name";
    public static final String PAGE_LANG = "page-lang";
    public static final String PAGE_TITLE = "page-title";

    private RuleIds() {
    }
}
