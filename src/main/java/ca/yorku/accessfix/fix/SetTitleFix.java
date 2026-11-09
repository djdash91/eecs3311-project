package ca.yorku.accessfix.fix;

import ca.yorku.accessfix.domain.PageSnapshot;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Sets the page title (FixType.SET_TITLE). The title is element text, not an attribute, and the
 * title element may not exist yet, so this implements FixCommand directly.
 *
 * <p>execute changes the first title in the head, which is the one PageTitleRule checks, or adds a
 * title at the end of the head if there is none. An added title gets its own stable ID (the head's
 * plus "-title") so undo can find it. undo removes an added title, or puts back the old text.
 */
public class SetTitleFix implements FixCommand {

    private final String issueId;
    private final String title;
    /** True if execute() added the title element, false if it changed an existing one. */
    private boolean createdTitle;
    /** The stable ID of the title element execute() changed or added. */
    private String titleElementId;
    /** The title's exact text before execute(), when it changed an existing title. */
    private String oldText;
    private boolean executed;

    public SetTitleFix(String issueId, String title) {
        this.issueId = issueId;
        this.title = title;
    }

    /** For JSON libraries only. */
    protected SetTitleFix() {
        this(null, null);
    }

    @Override
    public String issueId() {
        return issueId;
    }

    @Override
    public String description() {
        return "Set page title to \"" + title + "\"";
    }

    @Override
    public void execute(PageSnapshot snapshot) {
        Document doc = snapshot.parse();
        Element head = doc.head();
        Element existing = head.selectFirst("title");

        createdTitle = existing == null;
        if (createdTitle) {
            String headId = head.attr(PageSnapshot.ID_ATTR);
            if (headId.isEmpty()) {
                throw new IllegalStateException("The head element has no " + PageSnapshot.ID_ATTR);
            }
            titleElementId = headId + "-title";
            head.appendElement("title").attr(PageSnapshot.ID_ATTR, titleElementId).text(title);
        } else {
            titleElementId = existing.attr(PageSnapshot.ID_ATTR);
            oldText = existing.wholeText();
            existing.text(title);
        }

        snapshot.update(doc);
        executed = true;
    }

    @Override
    public void undo(PageSnapshot snapshot) {
        if (!executed) {
            throw new IllegalStateException("undo() called before execute()");
        }
        Document doc = snapshot.parse();
        Element titleElement = PageSnapshot.findElement(doc, titleElementId);
        if (titleElement == null) {
            throw new IllegalStateException("No element with " + PageSnapshot.ID_ATTR + "=" + titleElementId);
        }
        if (createdTitle) {
            titleElement.remove();
        } else {
            titleElement.text(oldText);
        }
        snapshot.update(doc);
        executed = false;
    }
}
