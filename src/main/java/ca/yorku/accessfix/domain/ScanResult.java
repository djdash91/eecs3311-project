package ca.yorku.accessfix.domain;

import java.util.List;
import java.util.Optional;

/**
 * The result of one scan (F02). Immutable: a re-scan creates a new ScanResult.
 *
 * @param issues      the issues found, sorted so the same snapshot always gives the same order
 * @param rulesNotRun IDs of rules that threw an exception and were marked "not run"
 */
public record ScanResult(List<Issue> issues, List<String> rulesNotRun) {

    public ScanResult {
        issues = List.copyOf(issues);
        rulesNotRun = List.copyOf(rulesNotRun);
    }

    public boolean contains(String issueId) {
        return issues.stream().anyMatch(issue -> issue.id().equals(issueId));
    }

    public Optional<Issue> find(String issueId) {
        return issues.stream().filter(issue -> issue.id().equals(issueId)).findFirst();
    }

    public boolean allRulesRan() {
        return rulesNotRun.isEmpty();
    }

    public int issueCount() {
        return issues.size();
    }
}
