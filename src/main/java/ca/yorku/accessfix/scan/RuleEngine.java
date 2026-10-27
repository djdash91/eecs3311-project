package ca.yorku.accessfix.scan;

import ca.yorku.accessfix.domain.PageSnapshot;
import ca.yorku.accessfix.domain.ScanResult;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Runs every Rule on a snapshot (Strategy pattern context, F02, SD01).
 * Spring passes in every bean that implements Rule, so adding a rule means adding one class.
 */
@Service
public class RuleEngine {

    private final List<Rule> rules;

    public RuleEngine(List<Rule> rules) {
        this.rules = List.copyOf(rules);
    }

    public List<Rule> getRules() {
        return rules;
    }

    /**
     * TODO(F02, SD01): implement.
     * <ol>
     *   <li>Call check() on each rule.</li>
     *   <li>If a rule throws, record its id() in rulesNotRun and continue with the others.</li>
     *   <li>Sort the issues so the result doesn't depend on the order Spring gives you the rules
     *       (for example by ruleId, then elementId).</li>
     *   <li>Return a new ScanResult.</li>
     * </ol>
     * Test idea: a fake Rule that always throws, plus ImageAltRule. Expect ImageAltRule's issues
     * and the fake rule's ID in rulesNotRun.
     */
    public ScanResult scan(PageSnapshot snapshot) {
        throw new UnsupportedOperationException("TODO: RuleEngine.scan");
    }
}
