package ca.yorku.accessfix.scan;

import ca.yorku.accessfix.domain.Issue;
import ca.yorku.accessfix.domain.PageSnapshot;
import ca.yorku.accessfix.domain.ScanResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Runs every Rule on a snapshot (Strategy pattern context, F02, SD01).
 * Spring passes in every bean that implements Rule, so adding a rule means adding one class.
 */
@Service
public class RuleEngine {

    private static final Logger LOG = LoggerFactory.getLogger(RuleEngine.class);

    /** Sorts by rule, then element, so the order doesn't depend on the order Spring gives the rules. */
    private static final Comparator<Issue> ISSUE_ORDER =
            Comparator.comparing(Issue::ruleId).thenComparing(Issue::elementId);

    private final List<Rule> rules;

    public RuleEngine(List<Rule> rules) {
        this.rules = List.copyOf(rules);
    }

    public List<Rule> getRules() {
        return rules;
    }

    /**
     * Calls check() on each rule and collects the issues (F02, SD01). A rule that throws is
     * recorded in rulesNotRun, none of its issues are kept, and the other rules carry on.
     */
    public ScanResult scan(PageSnapshot snapshot) {
        List<Issue> issues = new ArrayList<>();
        List<String> rulesNotRun = new ArrayList<>();

        for (Rule rule : rules) {
            try {
                issues.addAll(rule.check(snapshot));
            } catch (RuntimeException e) {
                LOG.warn("Rule {} failed; marking it not run", rule.id(), e);
                rulesNotRun.add(rule.id());
            }
        }
        issues.sort(ISSUE_ORDER);
        rulesNotRun.sort(Comparator.naturalOrder());
        return new ScanResult(issues, rulesNotRun);
    }
}
