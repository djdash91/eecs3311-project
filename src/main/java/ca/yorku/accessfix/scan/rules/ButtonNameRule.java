package ca.yorku.accessfix.scan.rules;

import ca.yorku.accessfix.domain.Issue;
import ca.yorku.accessfix.domain.PageSnapshot;
import ca.yorku.accessfix.domain.RuleIds;
import ca.yorku.accessfix.scan.Rule;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Flags buttons with no accessible name (F02).
 *
 * <p>TODO(F02): implement check(). Consider button elements, input type=button, and elements with
 * role="button". Note that input type=submit and type=reset get a default name from the browser.
 */
@Component
public class ButtonNameRule implements Rule {

    @Override
    public String id() {
        return RuleIds.BUTTON_NAME;
    }

    @Override
    public String criterion() {
        return "4.1.2"; // Suggested: Name, Role, Value. Check against your report.
    }

    @Override
    public List<Issue> check(PageSnapshot snapshot) {
        throw new UnsupportedOperationException("TODO: ButtonNameRule.check");
    }
}
