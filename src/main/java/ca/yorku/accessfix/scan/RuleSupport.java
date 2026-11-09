package ca.yorku.accessfix.scan;

import ca.yorku.accessfix.domain.PageSnapshot;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

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

    /**
     * True if the element has a role attribute set to something other than this role, so another
     * rule owns it. For example, a[href] with role="button" is a button, not a link.
     */
    public static boolean hasRoleOtherThan(Element element, String role) {
        String actual = element.attr("role").trim();
        return !actual.isEmpty() && !actual.equalsIgnoreCase(role);
    }

    /** True if the element has a non-blank aria-label or aria-labelledby. */
    public static boolean hasAriaName(Element element) {
        return !element.attr("aria-label").isBlank() || !element.attr("aria-labelledby").isBlank();
    }

    /**
     * True if the element's content gives it a name, as for links and buttons: non-blank text, an
     * image with non-blank alt, or a child with an aria-label. Hidden and aria-hidden children don't count.
     */
    public static boolean hasNameFromContent(Element element, PageSnapshot snapshot) {
        for (Node child : element.childNodes()) {
            if (child instanceof TextNode text && !text.isBlank()) {
                return true;
            }
            if (child instanceof Element childElement && givesName(childElement, snapshot)) {
                return true;
            }
        }
        return false;
    }

    private static boolean givesName(Element child, PageSnapshot snapshot) {
        if (isAriaHidden(child) || snapshot.isHidden(child.attr(PageSnapshot.ID_ATTR))) {
            return false;
        }
        if (!child.attr("aria-label").isBlank()) {
            return true;
        }
        if (child.normalName().equals("img")) {
            return !child.attr("alt").isBlank();
        }
        return hasNameFromContent(child, snapshot);
    }
}
