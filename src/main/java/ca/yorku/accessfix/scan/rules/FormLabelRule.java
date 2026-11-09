package ca.yorku.accessfix.scan.rules;

import ca.yorku.accessfix.domain.Issue;
import ca.yorku.accessfix.domain.PageSnapshot;
import ca.yorku.accessfix.domain.RuleIds;
import ca.yorku.accessfix.scan.Rule;
import ca.yorku.accessfix.scan.RuleSupport;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/**
 * Flags form fields with no label (F02).
 *
 * <p>Form fields are input, select and textarea. Inputs of type hidden, submit, reset, button and
 * image are skipped: hidden ones aren't shown, submit and reset get a default name from the browser,
 * and button-like inputs are named by their value or alt, not by a label.
 *
 * <p>A field counts as labelled by a non-blank label[for=its id], a wrapping label with text,
 * aria-label, aria-labelledby or title. A placeholder is not a label, and a label the snapshot
 * records as hidden doesn't count, because the fix this rule expects adds a visible label (F08).
 *
 * <p>Form fields belong to this rule whatever their role, so an input with role="button" is checked
 * here and not by ButtonNameRule: its name still comes from a label, not from content.
 */
@Component
public class FormLabelRule implements Rule {

    private static final String FORM_FIELDS = "input, select, textarea";

    private static final Set<String> UNLABELLED_INPUT_TYPES = Set.of("hidden", "submit", "reset", "button", "image");

    @Override
    public String id() {
        return RuleIds.FORM_LABEL;
    }

    @Override
    public String criterion() {
        return "4.1.2";
    }

    @Override
    public List<Issue> check(PageSnapshot snapshot) {
        Document doc = snapshot.parse();
        List<Issue> issues = new ArrayList<>();
        Set<String> labelledIds = labelledFieldIds(doc, snapshot);

        for (Element field : doc.select(FORM_FIELDS)) {
            String elementId = field.attr(PageSnapshot.ID_ATTR);
            if (elementId.isEmpty() || snapshot.isHidden(elementId) || RuleSupport.isAriaHidden(field)
                    || !isFormField(field)) {
                continue;
            }
            if (!hasLabel(field, labelledIds, snapshot)) {
                issues.add(Issue.of(id(), criterion(), elementId, RuleSupport.snippet(field),
                        "Form field has no label: add a visible label, or an aria-label if a visible one isn't possible."));
            }
        }
        return issues;
    }

    /**
     * True if this rule checks the element: a select, a textarea, or an input that needs a label.
     * ButtonNameRule uses it too, so the two rules never check the same element.
     */
    static boolean isFormField(Element element) {
        if (!element.is(FORM_FIELDS)) {
            return false;
        }
        if (!element.normalName().equals("input")) {
            return true;
        }
        return !UNLABELLED_INPUT_TYPES.contains(element.attr("type").trim().toLowerCase(Locale.ROOT));
    }

    private static boolean hasLabel(Element field, Set<String> labelledIds, PageSnapshot snapshot) {
        if (RuleSupport.hasAriaName(field) || !field.attr("title").isBlank()) {
            return true;
        }
        String fieldId = field.id();
        if (!fieldId.isEmpty() && labelledIds.contains(fieldId)) {
            return true;
        }
        Element wrappingLabel = field.closest("label");
        return wrappingLabel != null && isVisibleLabel(wrappingLabel, snapshot);
    }

    /** The ids named by every visible label[for] that has text, collected once per scan. */
    private static Set<String> labelledFieldIds(Document doc, PageSnapshot snapshot) {
        Set<String> ids = new HashSet<>();
        for (Element label : doc.select("label[for]")) {
            if (isVisibleLabel(label, snapshot)) {
                ids.add(label.attr("for"));
            }
        }
        return ids;
    }

    private static boolean isVisibleLabel(Element label, PageSnapshot snapshot) {
        return !snapshot.isHidden(label.attr(PageSnapshot.ID_ATTR)) && !label.text().isBlank();
    }
}
