package ca.yorku.accessfix.domain;

/**
 * Whether a fix was kept or rolled back, and why (F09).
 *
 * @param scanAfter the scan used to decide; useful for the UI and the agent
 */
public record VerificationResult(boolean kept, String reason, ScanResult scanAfter) {

    public static VerificationResult verified(ScanResult scanAfter) {
        return new VerificationResult(true, "Verified: the issue is gone and no new issues appeared.", scanAfter);
    }

    public static VerificationResult rolledBack(String reason, ScanResult scanAfter) {
        return new VerificationResult(false, reason, scanAfter);
    }
}
