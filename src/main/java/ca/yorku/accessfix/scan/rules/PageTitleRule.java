package ca.yorku.accessfix.scan.rules;

import ca.yorku.accessfix.domain.Issue;
import ca.yorku.accessfix.domain.PageSnapshot;
import ca.yorku.accessfix.domain.RuleIds;
import ca.yorku.accessfix.scan.Rule;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Flags a page with no title, or a blank one (F02).
 *
 * <p>TODO(F02): implement check(). When the title element is missing there is no element to point
 * at. Which element's stable ID will you use (html? head?) so the issue ID stays stable, and so
 * SetTitleFix knows where to add a title?
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
        throw new UnsupportedOperationException("TODO: PageTitleRule.check");
    }
}
