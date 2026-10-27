package ca.yorku.accessfix.facade;

import ca.yorku.accessfix.agent.AgentController;
import ca.yorku.accessfix.agent.AgentResult;
import ca.yorku.accessfix.agent.AgentSession;
import ca.yorku.accessfix.agent.ApprovalGate;
import ca.yorku.accessfix.agent.IssueAssistant;
import ca.yorku.accessfix.agent.MemoryManager;
import ca.yorku.accessfix.agent.mode.AskMode;
import ca.yorku.accessfix.agent.mode.FixMode;
import ca.yorku.accessfix.agent.mode.PlanMode;
import ca.yorku.accessfix.common.AuditBusyException;
import ca.yorku.accessfix.common.NotFoundException;
import ca.yorku.accessfix.domain.Audit;
import ca.yorku.accessfix.domain.Explanation;
import ca.yorku.accessfix.domain.FixProposal;
import ca.yorku.accessfix.domain.RemediationPlan;
import ca.yorku.accessfix.domain.ScanResult;
import ca.yorku.accessfix.domain.VerificationResult;
import ca.yorku.accessfix.fix.FixService;
import ca.yorku.accessfix.persistence.AuditRepository;
import ca.yorku.accessfix.report.ReportService;
import ca.yorku.accessfix.scan.RuleEngine;
import ca.yorku.accessfix.snapshot.SnapshotService;
import ca.yorku.accessfix.wcag.WcagCriterion;
import ca.yorku.accessfix.wcag.WcagKnowledgeBase;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * The one entry point for both the GUI and the CLI (Facade pattern, view A). Each method coordinates
 * subsystems; keep real logic in the services so it can be tested without the facade.
 *
 * <p>Every TODO names the sequence diagram that shows its steps.
 */
@Service
public class AccessFixFacade {

    private final SnapshotService snapshotService;
    private final RuleEngine ruleEngine;
    private final FixService fixService;
    private final ReportService reportService;
    private final AuditRepository repository;
    private final WcagKnowledgeBase wcag;
    private final IssueAssistant issueAssistant;
    private final MemoryManager memory;
    private final AgentController agent;
    private final ApprovalGate approvalGate;
    private final AskMode askMode;
    private final PlanMode planMode;
    private final FixMode fixMode;

    public AccessFixFacade(SnapshotService snapshotService, RuleEngine ruleEngine, FixService fixService,
                           ReportService reportService, AuditRepository repository, WcagKnowledgeBase wcag,
                           IssueAssistant issueAssistant, MemoryManager memory, AgentController agent,
                           ApprovalGate approvalGate, AskMode askMode, PlanMode planMode, FixMode fixMode) {
        this.snapshotService = snapshotService;
        this.ruleEngine = ruleEngine;
        this.fixService = fixService;
        this.reportService = reportService;
        this.repository = repository;
        this.wcag = wcag;
        this.issueAssistant = issueAssistant;
        this.memory = memory;
        this.agent = agent;
        this.approvalGate = approvalGate;
        this.askMode = askMode;
        this.planMode = planMode;
        this.fixMode = fixMode;
    }

    // ---------- Deterministic features ----------

    /** TODO(F01, SD01): validate (InvalidInputException), capture, Audit.create, scan, save, return. */
    public Audit loadPage(PageInput input) {
        throw new UnsupportedOperationException("TODO: AccessFixFacade.loadPage");
    }

    /** TODO(F02, SD01): re-scan the stored snapshot, add the result to the audit, save. */
    public ScanResult scan(String auditId) {
        throw new UnsupportedOperationException("TODO: AccessFixFacade.scan");
    }

    /** TODO(F05, SD07): one line. */
    public List<WcagCriterion> searchWcag(String query) {
        throw new UnsupportedOperationException("TODO: AccessFixFacade.searchWcag");
    }

    /** TODO(F09, SD07): refuse while busy, then fixService.undoLast(). */
    public ScanResult undoLastFix(String auditId) {
        throw new UnsupportedOperationException("TODO: AccessFixFacade.undoLastFix");
    }

    /** TODO(F10, SD07). */
    public String exportReport(String auditId) {
        throw new UnsupportedOperationException("TODO: AccessFixFacade.exportReport");
    }

    /** TODO(F10, SD07). */
    public String exportPatchedPage(String auditId) {
        throw new UnsupportedOperationException("TODO: AccessFixFacade.exportPatchedPage");
    }

    // ---------- Single model requests ----------

    /** TODO(F03, SD02): find the issue in the latest scan (NotFoundException if absent), then explain. */
    public Explanation explainIssue(String auditId, String issueId) {
        throw new UnsupportedOperationException("TODO: AccessFixFacade.explainIssue");
    }

    /** TODO(F07, SD02). */
    public Optional<String> suggestAltText(String auditId, String issueId) {
        throw new UnsupportedOperationException("TODO: AccessFixFacade.suggestAltText");
    }

    /** TODO(F07, F09, SD02): refuse while busy, then fixService.applyVerified(). */
    public VerificationResult applyFix(String auditId, FixProposal proposal) {
        throw new UnsupportedOperationException("TODO: AccessFixFacade.applyFix");
    }

    // ---------- Agent ----------

    /** TODO(F04, SD03): session, busy check, setMode(askMode), agent.run(). */
    public AgentResult sendMessage(String auditId, String text) {
        throw new UnsupportedOperationException("TODO: AccessFixFacade.sendMessage");
    }

    /** TODO(F06, SD04): as sendMessage with planMode; save the plan (not yet approved). No plan: error. */
    public RemediationPlan createPlan(String auditId, String goal) {
        throw new UnsupportedOperationException("TODO: AccessFixFacade.createPlan");
    }

    /** TODO(F06, SD04): mark the plan approved and save. No plan: tell the user to create one. */
    public void approvePlan(String auditId) {
        throw new UnsupportedOperationException("TODO: AccessFixFacade.approvePlan");
    }

    /**
     * TODO(F08, SD05): busy check and approved-plan check BEFORE changing anything; escalate manual
     * items; setMode(fixMode); run with the approved plan as the first message.
     */
    public AgentResult startFixSession(String auditId) {
        throw new UnsupportedOperationException("TODO: AccessFixFacade.startFixSession");
    }

    /**
     * TODO(F08, SD06): build a Decision from the session's pending proposal (none pending: error),
     * call approvalGate.decide(), send the result plus held results back as one tool_result message,
     * and run again. When the session finishes, add issue counts from the latest scan.
     *
     * <p>Stage 1 passes a Decision here; the starter takes approved and reason instead, because the
     * proposal lives in the session. Keep or change this, and note it.
     */
    public AgentResult submitDecision(String auditId, boolean approved, String reason) {
        throw new UnsupportedOperationException("TODO: AccessFixFacade.submitDecision");
    }

    // ---------- Small helpers you will use in many methods ----------

    Audit findAudit(String auditId) {
        return repository.findById(auditId)
                .orElseThrow(() -> new NotFoundException("No audit with ID " + auditId));
    }

    AgentSession requireNotBusy(String auditId) {
        AgentSession session = memory.sessionFor(auditId);
        if (session.isBusy()) {
            throw new AuditBusyException(auditId);
        }
        return session;
    }
}
