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
 * Flags a page whose html element has no lang attribute (F02).
 *
 * <p>The issue's element is the html element, which is also where SetLanguageFix adds lang (F08).
 * A blank lang counts as missing. Whether a non-blank value is a real language isn't checked:
 * Stage 1 defines this rule as "missing page language".
 */
@Component
public class PageLanguageRule implements Rule {

    @Override
    public String id() {
        return RuleIds.PAGE_LANG;
    }

    @Override
    public String criterion() {
        return "3.1.1"; // Language of Page
    }

    @Override
    public List<Issue> check(PageSnapshot snapshot) {
        Document doc = snapshot.parse();
        Element html = doc.selectFirst("html");
        if (html == null) {
            return List.of();
        }
        String elementId = html.attr(PageSnapshot.ID_ATTR);
        if (elementId.isEmpty() || snapshot.isHidden(elementId) || !html.attr("lang").isBlank()) {
            return List.of();
        }
        return List.of(Issue.of(id(), criterion(), elementId, RuleSupport.snippet(html.shallowClone()),
                "Page has no language: add a lang attribute to the html element, for example lang=\"en\"."));
    }
}
