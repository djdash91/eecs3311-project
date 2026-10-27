package ca.yorku.accessfix.domain;

import java.util.List;

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
     * Checks this plan against the latest scan and returns readable error messages.
     * An empty list means the plan is valid. The errors are sent back to the model, so make them
     * specific enough for it to correct the plan ("issue image-alt:e12 appears twice").
     *
     * <p>Stage 1 (F06) says every issue must exist, appear once, and have an allowed fix type.
     * Think about the questions this leaves open and write down your answers:
     * <ul>
     *   <li>Must every open issue appear in the plan, or only some?</li>
     *   <li>Are duplicate priorities allowed?</li>
     *   <li>Is "manual" allowed for an issue that has a fix type?</li>
     * </ul>
     *
     * <p>This is deterministic, so it is a good place for many small unit tests (Stage 3, Part A).
     */
    public List<String> validate(ScanResult scan) {
        // TODO(F06): implement the checks above. Use FixType.canFix(ruleId) for "allowed".
        throw new UnsupportedOperationException("TODO: RemediationPlan.validate");
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
