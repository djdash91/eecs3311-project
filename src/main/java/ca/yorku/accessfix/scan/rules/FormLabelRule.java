package ca.yorku.accessfix.scan.rules;

import ca.yorku.accessfix.domain.Issue;
import ca.yorku.accessfix.domain.PageSnapshot;
import ca.yorku.accessfix.domain.RuleIds;
import ca.yorku.accessfix.scan.Rule;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Flags form fields with no label (F02).
 *
 * <p>TODO(F02): implement check(), following ImageAltRule. Questions to answer first:
 * <ul>
 *   <li>Which elements count as form fields? (input, select, textarea... but not input type=hidden,
 *       and what about submit, reset, button and image inputs?)</li>
 *   <li>Which ways of naming a field count? (label[for=id], a wrapping label, aria-label,
 *       aria-labelledby, title?)</li>
 * </ul>
 * Write the answers in your test names, then make the tests pass.
 */
@Component
public class FormLabelRule implements Rule {

    @Override
    public String id() {
        return RuleIds.FORM_LABEL;
    }

    @Override
    public String criterion() {
        // Suggested: "4.1.2" (Name, Role, Value). "1.3.1" and "3.3.2" are also defensible.
        // Pick one, match your Stage 1 report, and make sure it is in wcag-criteria.json.
        return "4.1.2";
    }

    @Override
    public List<Issue> check(PageSnapshot snapshot) {
        throw new UnsupportedOperationException("TODO: FormLabelRule.check");
    }
}
