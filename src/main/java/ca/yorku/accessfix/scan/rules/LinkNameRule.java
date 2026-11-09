package ca.yorku.accessfix.scan.rules;

import ca.yorku.accessfix.domain.Issue;
import ca.yorku.accessfix.domain.PageSnapshot;
import ca.yorku.accessfix.domain.RuleIds;
import ca.yorku.accessfix.scan.Rule;
import ca.yorku.accessfix.scan.RuleSupport;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/**
 * Flags links with no accessible name (F02).
 *
 * <p>Links are a[href] and elements with role="link". An a element without href isn't a link, and
 * an a[href] with another role, such as role="button", is left to the rule for that role, so one
 * element isn't reported by two rules. Form fields with role="link" are left to FormLabelRule or
 * ButtonNameRule too, because they're named by a label, value or alt, never by their content.
 *
 * <p>A link is named by aria-label, aria-labelledby or title, or by its content: non-blank text,
 * an image with non-blank alt, or a child with an aria-label. Whitespace-only text, icons with
 * aria-hidden and images with empty alt don't name it.
 */
@Component
public class LinkNameRule implements Rule {

    @Override
    public String id() {
        return RuleIds.LINK_NAME;
    }

    @Override
    public String criterion() {
        return "2.4.4";
    }

    @Override
    public List<Issue> check(PageSnapshot snapshot) {
        Document doc = snapshot.parse();
        List<Issue> issues = new ArrayList<>();

        for (Element link : doc.select("a[href], [role=link]")) {
            String elementId = link.attr(PageSnapshot.ID_ATTR);
            if (elementId.isEmpty() || snapshot.isHidden(elementId) || RuleSupport.isAriaHidden(link)
                    || RuleSupport.hasRoleOtherThan(link, "link") || link.is("input, select, textarea")) {
                continue;
            }
            if (!hasName(link, snapshot)) {
                issues.add(Issue.of(id(), criterion(), elementId, RuleSupport.snippet(link),
                        "Link has no name: add link text, or an aria-label that says where the link goes."));
            }
        }
        return issues;
    }

    private static boolean hasName(Element link, PageSnapshot snapshot) {
        return RuleSupport.hasAriaName(link)
                || !link.attr("title").isBlank()
                || RuleSupport.hasNameFromContent(link, snapshot);
    }
}
