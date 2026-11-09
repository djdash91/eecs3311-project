package ca.yorku.accessfix.scan.rules;

import ca.yorku.accessfix.domain.Issue;
import ca.yorku.accessfix.domain.PageSnapshot;
import ca.yorku.accessfix.domain.RuleIds;
import ca.yorku.accessfix.scan.Rule;
import ca.yorku.accessfix.scan.RuleSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/**
 * Flags buttons with no accessible name (F02).
 *
 * <p>Buttons are button elements, input type=button, input type=image and elements with
 * role="button". Input type=image is checked here because FormLabelRule leaves it to this rule.
 * Input type=submit and type=reset aren't selected: the browser gives them a default name.
 *
 * <p>A button is named by aria-label, aria-labelledby or title, or else by its own content:
 * non-blank value for input type=button, non-blank alt for input type=image and img, and for
 * other elements non-blank text, an image with alt or a child with an aria-label.
 *
 * <p>Each element is checked by one rule. Form fields that need a label are left to FormLabelRule,
 * whatever their role. Any other element with a role other than button, such as
 * {@code <button role="link">}, is left to the rule for that role.
 */
@Component
public class ButtonNameRule implements Rule {

    @Override
    public String id() {
        return RuleIds.BUTTON_NAME;
    }

    @Override
    public String criterion() {
        return "4.1.2";
    }

    @Override
    public List<Issue> check(PageSnapshot snapshot) {
        Document doc = snapshot.parse();
        List<Issue> issues = new ArrayList<>();

        for (Element button : doc.select("button, input[type=button], input[type=image], [role=button]")) {
            String elementId = button.attr(PageSnapshot.ID_ATTR);
            if (elementId.isEmpty() || snapshot.isHidden(elementId) || RuleSupport.isAriaHidden(button)
                    || FormLabelRule.isFormField(button) || hasOtherRole(button)) {
                continue;
            }
            if (!hasName(button, snapshot)) {
                issues.add(Issue.of(id(), criterion(), elementId, RuleSupport.snippet(button),
                        "Button has no name: add button text, or an aria-label that says what the button does."));
            }
        }
        return issues;
    }

    /** Inputs belong to a rule by their type, so only other elements are handed over by role. */
    private static boolean hasOtherRole(Element button) {
        return !button.normalName().equals("input") && RuleSupport.hasRoleOtherThan(button, "button");
    }

    private static boolean hasName(Element button, PageSnapshot snapshot) {
        if (RuleSupport.hasAriaName(button) || !button.attr("title").isBlank()) {
            return true;
        }
        return switch (button.normalName()) {
            case "input" -> inputHasName(button);
            case "img" -> !button.attr("alt").isBlank();
            default -> RuleSupport.hasNameFromContent(button, snapshot);
        };
    }

    private static boolean inputHasName(Element input) {
        return switch (input.attr("type").trim().toLowerCase(Locale.ROOT)) {
            case "submit", "reset" -> true;
            case "image" -> !input.attr("alt").isBlank();
            default -> !input.attr("value").isBlank();
        };
    }
}
