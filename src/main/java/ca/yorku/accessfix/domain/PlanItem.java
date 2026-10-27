package ca.yorku.accessfix.domain;

/**
 * One entry in a remediation plan (F06).
 *
 * @param fixType the proposed fix, or null for "manual"
 */
public record PlanItem(String issueId, int priority, String reason, FixType fixType) {

    public boolean isManual() {
        return fixType == null;
    }
}
