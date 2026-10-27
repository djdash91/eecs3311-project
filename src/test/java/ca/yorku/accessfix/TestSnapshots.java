package ca.yorku.accessfix;

import ca.yorku.accessfix.domain.PageSnapshot;
import java.util.Map;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Builds PageSnapshots for tests without a browser. IDs are given in document order ("e0", "e1"...),
 * the same idea SnapshotService should use. Rules and fixes can then be unit tested quickly.
 */
public final class TestSnapshots {

    private TestSnapshots() {
    }

    public static PageSnapshot fromHtml(String html) {
        return withHidden(html);
    }

    /** Elements matching any of the CSS selectors are recorded as hidden, as Playwright would report. */
    public static PageSnapshot withHidden(String html, String... hiddenSelectors) {
        Document doc = Jsoup.parse(html);
        doc.outputSettings().prettyPrint(false);
        int next = 0;
        for (Element element : doc.getAllElements()) {
            if (element instanceof Document) {
                continue;
            }
            element.attr(PageSnapshot.ID_ATTR, "e" + next++);
        }
        java.util.Set<String> hidden = new java.util.HashSet<>();
        for (String selector : hiddenSelectors) {
            for (Element element : doc.select(selector)) {
                hidden.add(element.attr(PageSnapshot.ID_ATTR));
            }
        }
        return new PageSnapshot(doc.outerHtml(), Set.copyOf(hidden), Map.of());
    }

    /** The stable ID of the first element matching the selector. */
    public static String idOf(PageSnapshot snapshot, String cssSelector) {
        Element element = snapshot.parse().selectFirst(cssSelector);
        if (element == null) {
            throw new IllegalArgumentException("No element matches " + cssSelector);
        }
        return element.attr(PageSnapshot.ID_ATTR);
    }
}
