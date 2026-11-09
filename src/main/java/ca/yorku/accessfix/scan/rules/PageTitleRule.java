package ca.yorku.accessfix.scan.rules;

import ca.yorku.accessfix.domain.Issue;
import ca.yorku.accessfix.domain.PageSnapshot;
import ca.yorku.accessfix.domain.RuleIds;
import ca.yorku.accessfix.scan.Rule;
import ca.yorku.accessfix.scan.RuleSupport;
import java.util.List;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/**
 * Flags a page with no title, or a blank one (F02).
 *
 * <p>The issue's element is always the head element, whether the title is missing or blank, so the
 * issue ID stays the same and SetTitleFix knows the title belongs in the head (F08). Only the first
 * title in the head counts, as in browsers; a title inside an svg in the body isn't the page title.
 *
 * <p>Unlike the other rules, this one doesn't skip hidden elements. Browsers never display the head,
 * so SnapshotService records it as hidden, but the title is still read out and shown in the tab.
 */
@Component
public class PageTitleRule implements Rule {

    @Override
    public String id() {
        return RuleIds.PAGE_TITLE;
    }

    @Override
    public String criterion() {
        return "2.4.2"; // Page Titled
    }

    @Override
    public List<Issue> check(PageSnapshot snapshot) {
        Document doc = snapshot.parse();
        Element head = doc.head();
        String elementId = head.attr(PageSnapshot.ID_ATTR);
        if (elementId.isEmpty() || hasTitle(head)) {
            return List.of();
        }
        return List.of(Issue.of(id(), criterion(), elementId, RuleSupport.snippet(head),
                "Page has no title: add a title element to the head that describes the page's topic or purpose."));
    }

    private static boolean hasTitle(Element head) {
        Element title = head.selectFirst("title");
        return title != null && !title.text().isBlank();
    }
}
