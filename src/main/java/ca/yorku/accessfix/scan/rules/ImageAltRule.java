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
 * WORKED EXAMPLE of a Rule (Strategy pattern). Use it as the model for the other five rules,
 * and read ImageAltRuleTest to see how a rule is tested without a browser.
 *
 * <p>Flags images that have no alt attribute and no other accessible name.
 * {@code alt=""} is fine: it marks a decorative image.
 */
@Component
public class ImageAltRule implements Rule {

    @Override
    public String id() {
        return RuleIds.IMAGE_ALT;
    }

    @Override
    public String criterion() {
        return "1.1.1";
    }

    @Override
    public List<Issue> check(PageSnapshot snapshot) {
        Document doc = snapshot.parse();
        List<Issue> issues = new ArrayList<>();

        for (Element img : doc.select("img[" + PageSnapshot.ID_ATTR + "]")) {
            String elementId = img.attr(PageSnapshot.ID_ATTR);
            if (snapshot.isHidden(elementId) || isPresentational(img)) {
                continue;
            }
            if (!img.hasAttr("alt") && !RuleSupport.hasAriaName(img)) {
                issues.add(Issue.of(id(), criterion(), elementId, RuleSupport.snippet(img),
                        "Image has no text alternative: add an alt attribute (empty if the image is decorative)."));
            }
        }
        return issues;
    }

    private static boolean isPresentational(Element img) {
        String role = img.attr("role").trim();
        return RuleSupport.isAriaHidden(img) || role.equals("presentation") || role.equals("none");
    }
}
