package ca.yorku.accessfix.fix;

import ca.yorku.accessfix.domain.FixProposal;
import ca.yorku.accessfix.domain.Issue;
import ca.yorku.accessfix.domain.ScanResult;
import ca.yorku.accessfix.domain.VerificationResult;
import ca.yorku.accessfix.persistence.AuditRepository;
import ca.yorku.accessfix.scan.RuleEngine;
import org.springframework.stereotype.Service;

/**
 * Applies, verifies and undoes fixes (Command pattern invoker, F09, SD06, SD07).
 * "The rules decide, and the AI proposes": only this class decides whether a fix is kept.
 */
@Service
public class FixService {

    private final RuleEngine ruleEngine;
    private final AuditRepository repository;

    public FixService(RuleEngine ruleEngine, AuditRepository repository) {
        this.ruleEngine = ruleEngine;
        this.repository = repository;
    }

    /**
     * TODO(F08): return the affected code before and after the change, WITHOUT changing the real
     * snapshot. Hint: PageSnapshot.copy(), then createCommand(...).execute(copy).
     */
    public String preview(String auditId, FixProposal proposal) {
        throw new UnsupportedOperationException("TODO: FixService.preview");
    }

    /**
     * TODO(F09, SD06): implement.
     * <ol>
     *   <li>Load the audit. Refuse (return a rolled-back result with the reason, or throw: decide
     *       which and be consistent) if the issue isn't in the latest scan or the fix type can't fix
     *       its rule. This check applies to the user's own fixes too.</li>
     *   <li>createCommand, then command.execute(snapshot).</li>
     *   <li>ruleEngine.scan(snapshot).</li>
     *   <li>Undo if the issue is still there, any new issue ID appeared, or a rule didn't run.</li>
     *   <li>Otherwise push the command and add the new scan to the audit, and save it.</li>
     * </ol>
     * This is the most important deterministic class to unit test. Use InMemoryAuditRepository.
     */
    public VerificationResult applyVerified(String auditId, FixProposal proposal) {
        throw new UnsupportedOperationException("TODO: FixService.applyVerified");
    }

    /** TODO(F09, SD07): undo the most recent fix, re-scan, save. Nothing to undo: tell the user. */
    public ScanResult undoLast(String auditId) {
        throw new UnsupportedOperationException("TODO: FixService.undoLast");
    }

    /**
     * TODO(F08): turn a proposal into the right FixCommand (a switch on proposal.fixType()).
     * This is the only place that knows which class goes with which FixType.
     */
    FixCommand createCommand(Issue issue, FixProposal proposal) {
        throw new UnsupportedOperationException("TODO: FixService.createCommand");
    }
}
