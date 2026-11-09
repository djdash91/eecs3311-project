package ca.yorku.accessfix.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** A ranked plan the agent submits in Plan mode (F06, SD04). It validates itself against a scan. */
public class RemediationPlan {

    private List<PlanItem> items;
    private boolean approved;

    public RemediationPlan(List<PlanItem> items) {
        this.items = List.copyOf(items);
        this.approved = false;
    }

    /** For JSON libraries only. */
    protected RemediationPlan() {
        this(List.of());
    }

    /**
     * Checks this plan against the latest scan and returns readable error messages; an empty list
     * means the plan is valid (F06). The errors go back to the model, so each names the issue.
     *
     * <p>Stage 1 says every issue must exist, appear once and have an allowed fix type. The open
     * questions are answered like this:
     * <ul>
     *   <li>Every issue in the latest scan must appear: F06 says "each issue gets a priority", and
     *       Fix mode works through the plan, so an issue left out would never be fixed or escalated.</li>
     *   <li>Priorities must be 1 or more, and may be shared: several issues can be equally urgent,
     *       for example all images without alt text.</li>
     *   <li>"manual" is allowed for any issue, even one with a fix type: the right text may depend on
     *       facts only the site owner knows. Manual items are escalated when Fix mode starts.</li>
     *   <li>Every item needs a reason, because the user approves the plan from the reasons.</li>
     * </ul>
     */
    public List<String> validate(ScanResult scan) {
        List<String> errors = new ArrayList<>();
        Map<String, Integer> timesListed = new LinkedHashMap<>();
        for (PlanItem item : items) {
            if (item.issueId() == null || item.issueId().isBlank()) {
                errors.add("An item has no issue ID.");
                continue;
            }
            String id = item.issueId();
            timesListed.merge(id, 1, Integer::sum);
            Optional<Issue> issue = scan.find(id);
            if (issue.isEmpty()) {
                errors.add("Issue " + id + " isn't in the latest scan.");
                continue;
            }
            if (item.priority() < 1) {
                errors.add("Issue " + id + ": priority must be 1 or more (1 is the most urgent).");
            }
            if (item.reason() == null || item.reason().isBlank()) {
                errors.add("Issue " + id + " has no reason.");
            }
            String ruleId = issue.get().ruleId();
            if (!item.isManual() && !item.fixType().canFix(ruleId)) {
                errors.add("Issue " + id + ": " + item.fixType().toolName() + " can't fix " + ruleId
                        + " issues. Use " + allowedNames(ruleId) + ".");
            }
        }
        timesListed.forEach((id, times) -> {
            if (times > 1) {
                errors.add("Issue " + id + " appears " + times + " times; list each issue once.");
            }
        });
        List<String> missing = scan.issues().stream()
                .map(Issue::id)
                .filter(id -> !timesListed.containsKey(id))
                .toList();
        if (!missing.isEmpty()) {
            errors.add("These issues are missing from the plan: " + String.join(", ", missing) + ".");
        }
        return errors;
    }

    /** The fix types allowed for a rule, plus "manual", for error messages. */
    private static String allowedNames(String ruleId) {
        List<String> names = new ArrayList<>(FixType.allowedFor(ruleId).stream().map(FixType::toolName).toList());
        names.add("manual");
        return String.join(" or ", names);
    }

    public List<PlanItem> getItems() {
        return items;
    }

    public boolean isApproved() {
        return approved;
    }

    public void approve() {
        this.approved = true;
    }
}
