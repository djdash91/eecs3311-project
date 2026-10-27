package ca.yorku.accessfix.scan;

import ca.yorku.accessfix.domain.PageSnapshot;
import org.jsoup.nodes.Element;

/** Small helpers shared by the rules. Add more here as you find repeated code in your rules. */
public final class RuleSupport {

    private static final int MAX_SNIPPET_LENGTH = 300;

    private RuleSupport() {
    }

    /** The element's code for display, without AccessFix's stable ID attributes, shortened if long. */
    public static String snippet(Element element) {
        Element copy = element.clone();
        copy.removeAttr(PageSnapshot.ID_ATTR);
        copy.select("[" + PageSnapshot.ID_ATTR + "]").removeAttr(PageSnapshot.ID_ATTR);
        String html = copy.outerHtml();
        return html.length() <= MAX_SNIPPET_LENGTH ? html : html.substring(0, MAX_SNIPPET_LENGTH) + "...";
    }

    /** True if the element is explicitly removed from the accessibility tree by its author. */
    public static boolean isAriaHidden(Element element) {
        return "true".equalsIgnoreCase(element.attr("aria-hidden").trim());
    }

    /** True if the element has a non-blank aria-label or aria-labelledby. */
    public static boolean hasAriaName(Element element) {
        return !element.attr("aria-label").isBlank() || !element.attr("aria-labelledby").isBlank();
    }
}
