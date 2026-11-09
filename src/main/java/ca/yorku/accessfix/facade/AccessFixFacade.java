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
import ca.yorku.accessfix.agent.tools.ToolResult;
import ca.yorku.accessfix.common.AccessFixException;
import ca.yorku.accessfix.common.AuditBusyException;
import ca.yorku.accessfix.common.InvalidInputException;
import ca.yorku.accessfix.common.NotFoundException;
import ca.yorku.accessfix.domain.Audit;
import ca.yorku.accessfix.domain.Decision;
import ca.yorku.accessfix.domain.Explanation;
import ca.yorku.accessfix.domain.FixProposal;
import ca.yorku.accessfix.domain.Issue;
import ca.yorku.accessfix.domain.PageSnapshot;
import ca.yorku.accessfix.domain.PlanItem;
import ca.yorku.accessfix.domain.RemediationPlan;
import ca.yorku.accessfix.domain.RuleIds;
import ca.yorku.accessfix.domain.ScanResult;
import ca.yorku.accessfix.domain.VerificationResult;
import ca.yorku.accessfix.fix.FixService;
import ca.yorku.accessfix.llm.ContentBlock;
import ca.yorku.accessfix.llm.Message;
import ca.yorku.accessfix.persistence.AuditRepository;
import ca.yorku.accessfix.report.ReportService;
import ca.yorku.accessfix.scan.RuleEngine;
import ca.yorku.accessfix.snapshot.SnapshotService;
import ca.yorku.accessfix.wcag.WcagCriterion;
import ca.yorku.accessfix.wcag.WcagKnowledgeBase;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
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

    /**
     * Loads a page, scans it and saves the new audit (F01, UC01, SD01). The input is validated before
     * anything is loaded: an invalid URL or an empty HTML file throws InvalidInputException. A site
     * that can't be reached or times out throws PageLoadException, and nothing is saved.
     */
    public Audit loadPage(PageInput input) {
        if (input == null || (input.url() == null && input.html() == null)) {
            throw new InvalidInputException("Enter a URL or choose an HTML file.");
        }
        String source;
        PageSnapshot snapshot;
        if (input.isUrl()) {
            source = validUrl(input.url());
            snapshot = snapshotService.captureUrl(source);
        } else {
            if (input.html().isBlank()) {
                throw new InvalidInputException("The HTML file is empty.");
            }
            source = input.name() == null || input.name().isBlank() ? "Uploaded HTML file" : input.name();
            snapshot = snapshotService.captureHtml(input.html());
        }
        Audit audit = Audit.create(source, snapshot);
        audit.addScan(ruleEngine.scan(snapshot));
        repository.save(audit);
        return audit;
    }

    /**
     * The trimmed URL if it's a web address with a host (F01: "Invalid URL: an error is shown").
     * Only http and https are allowed, so a file: URL can't read files from the server's disk.
     */
    private static String validUrl(String url) {
        String trimmed = url.trim();
        try {
            URI uri = new URI(trimmed);
            boolean web = "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
            if (web && uri.getHost() != null) {
                return trimmed;
            }
        } catch (URISyntaxException e) {
            // Falls through to the same message as any other invalid URL.
        }
        throw new InvalidInputException("\"" + trimmed + "\" isn't a valid URL. Enter a web address that starts with http:// or https://.");
    }

    /**
     * Re-scans the stored snapshot, adds the result to the audit and saves it (F02, UC02, SD01).
     * The page isn't loaded again: all work uses the stored snapshot. It isn't refused while the
     * audit is busy, because it doesn't change the page.
     */
    public ScanResult scan(String auditId) {
        Audit audit = findAudit(auditId);
        ScanResult result = ruleEngine.scan(audit.getSnapshot());
        audit.addScan(result);
        repository.save(audit);
        return result;
    }

    /**
     * Searches the WCAG knowledge base (F05, UC05, SD07). No match gives an empty list (UC05 1a).
     * It isn't refused while the audit is busy: it doesn't use an audit at all.
     */
    public List<WcagCriterion> searchWcag(String query) {
        return wcag.search(query);
    }

    /**
     * Undoes the most recent applied fix and re-scans (F09, UC09, SD07). It is refused while the audit
     * is busy, because the agent may be working on the page. No fixes applied: InvalidInputException.
     */
    public ScanResult undoLastFix(String auditId) {
        findAudit(auditId);
        requireNotBusy(auditId);
        return fixService.undoLast(auditId);
    }

    /** The audit's Markdown report (F10, UC10, SD07). Nothing is saved: exporting doesn't change the audit. */
    public String exportReport(String auditId) {
        return reportService.generate(findAudit(auditId));
    }

    /** The fixed page's HTML, without stable ID attributes (F10, UC10, SD07). */
    public String exportPatchedPage(String auditId) {
        return reportService.exportPatchedPage(findAudit(auditId));
    }

    // ---------- Single model requests ----------

    /**
     * Explains an issue from the latest scan (F03, UC03, SD02). An issue that isn't in the latest
     * scan, for example one already fixed, throws NotFoundException. If Claude can't help, the
     * explanation is the issue's rule message.
     */
    public Explanation explainIssue(String auditId, String issueId) {
        return issueAssistant.explain(findLatestIssue(findAudit(auditId), issueId));
    }

    /**
     * Suggests alt text for an image issue in the latest scan (F07, UC07, SD02). Empty means no
     * suggestion could be made, and the user writes the alt text. Only image-alt issues have an
     * image to describe; any other issue throws InvalidInputException.
     */
    public Optional<String> suggestAltText(String auditId, String issueId) {
        Audit audit = findAudit(auditId);
        Issue issue = findLatestIssue(audit, issueId);
        if (!issue.ruleId().equals(RuleIds.IMAGE_ALT)) {
            throw new InvalidInputException("Alt text can only be suggested for an image issue, not " + issueId + ".");
        }
        return issueAssistant.suggestAltText(issue, audit.getSnapshot());
    }

    /**
     * Applies the user's own fix, for example edited alt text, and verifies it (F07, F09, UC07, SD02).
     * Refused while the audit is busy (UC07 3a). A fix that can't be allowed throws
     * InvalidInputException; one that fails verification is undone and returned as rolled back.
     * The user's own fixes don't count towards the agent's retry limit.
     */
    public VerificationResult applyFix(String auditId, FixProposal proposal) {
        findAudit(auditId);
        requireNotBusy(auditId);
        if (proposal == null || proposal.issueId() == null || proposal.fixType() == null || proposal.value() == null) {
            throw new InvalidInputException("A fix needs an issue, a fix type and a value.");
        }
        return fixService.applyVerified(auditId, proposal);
    }

    // ---------- Agent ----------

    /**
     * Answers a question about the audit in Ask mode (F04, UC04, SD03). Refused while the audit is
     * busy (UC04 1a). Follow-up questions continue the Ask conversation; coming from another mode
     * starts a new one, so a plan or fix session doesn't confuse the answers. LLMException reaches the
     * caller, so the user sees the error with Retry (UC04 1d).
     */
    public AgentResult sendMessage(String auditId, String text) {
        findAudit(auditId);
        if (text == null || text.isBlank()) {
            throw new InvalidInputException("Type a question first.");
        }
        AgentSession session = requireNotBusy(auditId);
        if (session.getMode() != askMode) {
            session.clearMessages();
        }
        session.setMode(askMode);
        return agent.run(session, text.trim());
    }

    /**
     * Has the agent create a ranked plan, and saves it, not yet approved (F06, UC06, SD04). A new plan
     * replaces the old one. Refused while busy (UC06 1a), or when the latest scan has no issues (the
     * use case needs open issues). If no valid plan was submitted before the step limit, it throws, so
     * the user sees an error with Retry (UC06 2a).
     */
    public RemediationPlan createPlan(String auditId, String goal) {
        Audit audit = findAudit(auditId);
        if (audit.latestScan().issues().isEmpty()) {
            throw new InvalidInputException("The latest scan found no issues, so there is nothing to plan.");
        }
        AgentSession session = requireNotBusy(auditId);
        session.clearMessages();
        session.setMode(planMode);
        String message = "Create a remediation plan for this page."
                + (goal == null || goal.isBlank() ? "" : "\nThe user's goal: " + goal.trim());
        RemediationPlan plan = agent.run(session, message).plan();
        if (plan == null) {
            throw new AccessFixException("The agent didn't submit a valid plan. Select Retry to try again.");
        }
        Audit latest = findAudit(auditId);
        latest.setPlan(plan);
        repository.save(latest);
        return plan;
    }

    /**
     * Marks the audit's plan approved and saves it (F06, UC06, SD04). No plan: the user is asked to
     * create one. Refused while busy, so the plan can't change under a running fix session.
     */
    public void approvePlan(String auditId) {
        Audit audit = findAudit(auditId);
        requireNotBusy(auditId);
        RemediationPlan plan = audit.getPlan()
                .orElseThrow(() -> new InvalidInputException("There is no plan to approve. Create a plan first."));
        plan.approve();
        repository.save(audit);
    }

    /**
     * Starts a fix session that works through the approved plan (F08, UC08, SD05). Both checks come
     * before anything changes: the audit isn't busy (UC08 1a), and an approved plan exists. Then the
     * plan's manual items are recorded as escalated (UC08 step 1), and the agent runs with the plan
     * as its first message. Each start is a new conversation, so after an API failure the user can
     * start again (UC08 *a), and verified fixes are kept.
     */
    public AgentResult startFixSession(String auditId) {
        Audit audit = findAudit(auditId);
        AgentSession session = requireNotBusy(auditId);
        RemediationPlan plan = audit.getPlan().filter(RemediationPlan::isApproved)
                .orElseThrow(() -> new InvalidInputException("Create and approve a plan before starting a fix session."));
        for (PlanItem item : plan.getItems()) {
            if (item.isManual() && audit.latestScan().contains(item.issueId()) && !audit.isEscalated(item.issueId())) {
                memory.recordEscalation(auditId, item.issueId(), "Marked manual in the plan: " + item.reason());
            }
        }
        session.clearMessages();
        session.setMode(fixMode);
        return withCountsIfFinished(auditId, agent.run(session, planMessage(plan, audit.latestScan())));
    }

    /**
     * Passes the user's decision on the pending fix to the agent, and continues the run (F08, UC08,
     * SD06). Stage 1 passes a Decision; this takes approved and reason, because the proposal is
     * already stored in the session (a recorded change). The decision's result and the turn's held
     * results go back as one message, in the order of the model's calls. When the session finishes,
     * the result has the issue counts from the latest scan.
     */
    public AgentResult submitDecision(String auditId, boolean approved, String reason) {
        findAudit(auditId);
        AgentSession session = memory.sessionFor(auditId);
        FixProposal proposal = session.getPendingProposal();
        if (proposal == null) {
            throw new InvalidInputException("No fix is waiting for a decision.");
        }
        if (session.isRunning()) {
            throw new AuditBusyException(auditId);
        }
        Decision decision = approved ? Decision.approve(proposal) : Decision.reject(proposal, reason);
        ToolResult result = approvalGate.decide(session, decision);

        String pendingId = session.getPendingToolUseId();
        List<ContentBlock.ToolResultBlock> results = new ArrayList<>(session.getHeldToolResults());
        results.add(new ContentBlock.ToolResultBlock(pendingId, result.content(), result.isError()));
        List<String> order = callOrder(session);
        results.sort(Comparator.comparingInt(block -> order.indexOf(block.toolUseId())));
        session.addUserContent(results);
        session.clearHeldToolResults();
        session.setPendingToolUseId(null);
        return withCountsIfFinished(auditId, agent.run(session, null));
    }

    /** The IDs of the tool calls in the model's last reply, in the order it made them. */
    private static List<String> callOrder(AgentSession session) {
        List<Message> messages = session.getMessages();
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).role() == Message.Role.ASSISTANT) {
                return messages.get(i).content().stream()
                        .filter(block -> block instanceof ContentBlock.ToolUse)
                        .map(block -> ((ContentBlock.ToolUse) block).id())
                        .toList();
            }
        }
        return List.of();
    }

    /**
     * The approved plan as the fix session's first message, most urgent first. Items no longer in the
     * latest scan (fixed since the plan was made, for example with applyFix) and manual items are
     * marked, so the agent skips them without spending steps.
     */
    static String planMessage(RemediationPlan plan, ScanResult latestScan) {
        StringBuilder text = new StringBuilder("Fix the issues in this approved plan, in priority order.\n");
        plan.getItems().stream()
                .sorted(Comparator.comparingInt(PlanItem::priority))
                .forEach(item -> text.append("- ").append(item.issueId())
                        .append(" (priority ").append(item.priority()).append("): ")
                        .append(!latestScan.contains(item.issueId()) ? "already fixed, skip"
                                : item.isManual() ? "manual, already escalated" : item.fixType().toolName())
                        .append(". ").append(item.reason()).append('\n'));
        return text.toString().trim();
    }

    /** When a fix session finishes (no fix waiting), adds the issue counts by rule from the latest scan (SD06). */
    private AgentResult withCountsIfFinished(String auditId, AgentResult result) {
        if (result.fixPending()) {
            return result;
        }
        Map<String, Integer> counts = new TreeMap<>();
        for (Issue issue : findAudit(auditId).latestScan().issues()) {
            counts.merge(issue.ruleId(), 1, Integer::sum);
        }
        return result.withIssueCounts(counts);
    }

    // ---------- Small helpers you will use in many methods ----------

    Audit findAudit(String auditId) {
        return repository.findById(auditId)
                .orElseThrow(() -> new NotFoundException("No audit with ID " + auditId));
    }

    Issue findLatestIssue(Audit audit, String issueId) {
        return audit.latestScan().find(issueId)
                .orElseThrow(() -> new NotFoundException("No issue " + issueId + " in the latest scan of audit " + audit.getId()));
    }

    AgentSession requireNotBusy(String auditId) {
        AgentSession session = memory.sessionFor(auditId);
        if (session.isBusy()) {
            throw new AuditBusyException(auditId);
        }
        return session;
    }
}
