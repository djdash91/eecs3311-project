package ca.yorku.accessfix.domain;

import ca.yorku.accessfix.fix.FixCommand;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The aggregate root for one page (view C): its snapshot, scans, plan, applied fixes, the user's
 * decisions, failure counts and escalations. AuditRepository saves and loads whole Audits.
 *
 * <p>This class only holds state. Rules about that state (retry limit, busy checks, verification)
 * belong in the services, so they can be tested on their own.
 */
public class Audit {

    private String id;
    private String source;
    private PageSnapshot snapshot;
    private final List<ScanResult> scans = new ArrayList<>();
    private RemediationPlan plan;
    private final Deque<FixCommand> appliedFixes = new ArrayDeque<>();
    private final List<Decision> decisions = new ArrayList<>();
    private final Set<String> escalatedIssueIds = new LinkedHashSet<>();
    private final Map<String, Integer> failedAttempts = new HashMap<>();

    public Audit(String id, String source, PageSnapshot snapshot) {
        this.id = id;
        this.source = source;
        this.snapshot = snapshot;
    }

    /** For JSON libraries only. */
    protected Audit() {
    }

    public static Audit create(String source, PageSnapshot snapshot) {
        return new Audit(UUID.randomUUID().toString(), source, snapshot);
    }

    public String getId() {
        return id;
    }

    /** The URL or file name the page came from. */
    public String getSource() {
        return source;
    }

    public PageSnapshot getSnapshot() {
        return snapshot;
    }

    public List<ScanResult> getScans() {
        return List.copyOf(scans);
    }

    public void addScan(ScanResult scan) {
        scans.add(scan);
    }

    public ScanResult firstScan() {
        if (scans.isEmpty()) {
            throw new IllegalStateException("Audit " + id + " has not been scanned yet");
        }
        return scans.get(0);
    }

    public ScanResult latestScan() {
        if (scans.isEmpty()) {
            throw new IllegalStateException("Audit " + id + " has not been scanned yet");
        }
        return scans.get(scans.size() - 1);
    }

    public Optional<RemediationPlan> getPlan() {
        return Optional.ofNullable(plan);
    }

    /** Replaces any earlier plan (F06: "creates a new one, which replaces it"). */
    public void setPlan(RemediationPlan plan) {
        this.plan = plan;
    }

    /** Applied, verified fixes, most recent first. */
    public List<FixCommand> getAppliedFixes() {
        return List.copyOf(appliedFixes);
    }

    public void pushAppliedFix(FixCommand fix) {
        appliedFixes.push(fix);
    }

    public Optional<FixCommand> popAppliedFix() {
        return Optional.ofNullable(appliedFixes.poll());
    }

    public List<Decision> getDecisions() {
        return List.copyOf(decisions);
    }

    public void addDecision(Decision decision) {
        decisions.add(decision);
    }

    public Set<String> getEscalatedIssueIds() {
        return Set.copyOf(escalatedIssueIds);
    }

    public void markEscalated(String issueId) {
        escalatedIssueIds.add(issueId);
    }

    public boolean isEscalated(String issueId) {
        return escalatedIssueIds.contains(issueId);
    }

    public Map<String, Integer> getFailedAttempts() {
        return Map.copyOf(failedAttempts);
    }

    /** Adds one failed fix for the issue and returns the new count. */
    public int incrementFailures(String issueId) {
        return failedAttempts.merge(issueId, 1, Integer::sum);
    }

    public int failuresFor(String issueId) {
        return failedAttempts.getOrDefault(issueId, 0);
    }
}
