package ca.yorku.accessfix.scan.rules;

import ca.yorku.accessfix.domain.Issue;
import ca.yorku.accessfix.domain.PageSnapshot;
import ca.yorku.accessfix.domain.RuleIds;
import ca.yorku.accessfix.scan.Rule;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Flags links with no accessible name (F02).
 *
 * <p>TODO(F02): implement check(). A link usually gets its name from its text. Think about a link
 * that only contains an image (the image's alt text names it), an icon with aria-hidden, and
 * whitespace-only text.
 */
@Component
public class LinkNameRule implements Rule {

    @Override
    public String id() {
        return RuleIds.LINK_NAME;
    }

    @Override
    public String criterion() {
        return "2.4.4"; // Suggested: Link Purpose (In Context). Check against your report.
    }

    @Override
    public List<Issue> check(PageSnapshot snapshot) {
        throw new UnsupportedOperationException("TODO: LinkNameRule.check");
    }
}
