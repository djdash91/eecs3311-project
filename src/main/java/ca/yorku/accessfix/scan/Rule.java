package ca.yorku.accessfix.scan;

import ca.yorku.accessfix.domain.Issue;
import ca.yorku.accessfix.domain.PageSnapshot;
import java.util.List;

/**
 * One accessibility check (Strategy pattern, view C). RuleEngine runs every Rule bean.
 *
 * <p>Contract for implementations:
 * <ul>
 *   <li>Read only the snapshot, so the same snapshot always gives the same issues.</li>
 *   <li>Skip elements the snapshot records as hidden.</li>
 *   <li>Build issues with {@link Issue#of}, so issue IDs stay stable.</li>
 *   <li>Throwing is allowed: RuleEngine marks the rule "not run" and carries on.</li>
 * </ul>
 */
public interface Rule {

    /** One of the RuleIds constants. */
    String id();

    /** The WCAG success criterion this rule checks, for example "1.1.1". */
    String criterion();

    List<Issue> check(PageSnapshot snapshot);
}
