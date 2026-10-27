package ca.yorku.accessfix.scan.rules;

import ca.yorku.accessfix.domain.Issue;
import ca.yorku.accessfix.domain.PageSnapshot;
import ca.yorku.accessfix.domain.RuleIds;
import ca.yorku.accessfix.scan.Rule;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Flags a page whose html element has no lang attribute (F02).
 *
 * <p>TODO(F02): implement check(). The issue's element is the html element itself, so use its
 * stable ID. Decide whether a blank or obviously invalid value ("xx-nonsense") also counts.
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
        throw new UnsupportedOperationException("TODO: PageLanguageRule.check");
    }
}
