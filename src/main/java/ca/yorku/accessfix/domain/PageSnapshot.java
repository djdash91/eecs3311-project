package ca.yorku.accessfix.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * A stored copy of a page (F01). Rules read it; FixCommands change its HTML (the Command pattern's receiver).
 *
 * <p>Typical use inside a rule or fix:
 * <pre>{@code
 * Document doc = snapshot.parse();
 * Element el = PageSnapshot.findElement(doc, elementId);
 * el.attr("alt", "A red bicycle");
 * snapshot.update(doc);   // nothing changes until you call update()
 * }</pre>
 */
public class PageSnapshot {

    /** Attribute that SnapshotService adds to every element, so issues and fixes can find it again. */
    public static final String ID_ATTR = "data-af-id";

    private String html;
    private Set<String> hiddenIds;
    private Map<String, byte[]> imageScreenshots;

    public PageSnapshot(String html, Set<String> hiddenIds, Map<String, byte[]> imageScreenshots) {
        this.html = html;
        this.hiddenIds = new HashSet<>(hiddenIds);
        this.imageScreenshots = new HashMap<>(imageScreenshots);
    }

    /** For JSON libraries only. */
    protected PageSnapshot() {
        this("", Set.of(), Map.of());
    }

    public String getHtml() {
        return html;
    }

    public Set<String> getHiddenIds() {
        return Set.copyOf(hiddenIds);
    }

    public Map<String, byte[]> getImageScreenshots() {
        return Map.copyOf(imageScreenshots);
    }

    public boolean isHidden(String elementId) {
        return hiddenIds.contains(elementId);
    }

    public Optional<byte[]> screenshotOf(String elementId) {
        return Optional.ofNullable(imageScreenshots.get(elementId));
    }

    /** Parses the stored HTML into a new jsoup Document. Pretty printing is off so HTML isn't reformatted. */
    public Document parse() {
        Document doc = Jsoup.parse(html);
        doc.outputSettings().prettyPrint(false);
        return doc;
    }

    /** Replaces the stored HTML with the given document. */
    public void update(Document doc) {
        this.html = doc.outerHtml();
    }

    /** A deep copy, so FixService.preview() can try a fix without changing the real snapshot (F08). */
    public PageSnapshot copy() {
        return new PageSnapshot(html, hiddenIds, imageScreenshots);
    }

    /** Finds an element by its stable ID, or returns null. */
    public static Element findElement(Document doc, String elementId) {
        return doc.getElementsByAttributeValue(ID_ATTR, elementId).first();
    }
}
