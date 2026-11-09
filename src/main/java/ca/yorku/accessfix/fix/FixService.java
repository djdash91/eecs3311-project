package ca.yorku.accessfix.fix;

import ca.yorku.accessfix.common.InvalidInputException;
import ca.yorku.accessfix.common.NotFoundException;
import ca.yorku.accessfix.domain.Audit;
import ca.yorku.accessfix.domain.FixProposal;
import ca.yorku.accessfix.domain.Issue;
import ca.yorku.accessfix.domain.PageSnapshot;
import ca.yorku.accessfix.domain.ScanResult;
import ca.yorku.accessfix.domain.VerificationResult;
import ca.yorku.accessfix.persistence.AuditRepository;
import ca.yorku.accessfix.scan.RuleEngine;
import ca.yorku.accessfix.scan.RuleSupport;
import java.util.List;
import java.util.Optional;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
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
     * The affected code before and after the fix, for the user's before-and-after diff (F08, SD05).
     * The fix is tried on a copy, so the real snapshot doesn't change. A fix that applyVerified would
     * refuse, or that can't be applied, is refused here too by throwing InvalidInputException.
     */
    public String preview(String auditId, FixProposal proposal) {
        Audit audit = findAudit(auditId);
        Issue issue = allowedIssue(audit, proposal);
        PageSnapshot copy = audit.getSnapshot().copy();
        try {
            createCommand(issue, proposal).execute(copy);
        } catch (RuntimeException e) {
            throw new InvalidInputException("The fix can't be applied. " + e.getMessage());
        }
        return affectedCode(audit.getSnapshot().parse(), copy.parse(), issue.elementId());
    }

    /**
     * Applies a fix, re-scans, and keeps the fix only if the rule engine confirms it (F09, SD06).
     *
     * <p>A fix that can't be allowed is refused by throwing InvalidInputException, before the page
     * changes: the issue isn't in the latest scan, or the fix type can't fix its rule. This applies
     * to the user's own fixes too. Refusals throw, rather than return a rolled-back result, so the
     * caller can tell them apart from fixes that failed verification, which count towards the
     * retry limit (SD06).
     *
     * <p>A fix that was applied is undone if a rule didn't run, the issue is still there, or a new
     * issue appeared. Otherwise it's kept: the command and the new scan are added to the audit, and
     * the audit is saved.
     */
    public VerificationResult applyVerified(String auditId, FixProposal proposal) {
        Audit audit = findAudit(auditId);
        Issue issue = allowedIssue(audit, proposal);
        ScanResult before = audit.latestScan();

        PageSnapshot snapshot = audit.getSnapshot();
        FixCommand command = createCommand(issue, proposal);
        try {
            command.execute(snapshot);
        } catch (RuntimeException e) {
            return VerificationResult.rolledBack("Rolled back: the fix couldn't be applied. " + e.getMessage(), before);
        }
        ScanResult after = ruleEngine.scan(snapshot);

        Optional<String> problem = verificationProblem(issue.id(), before, after);
        if (problem.isPresent()) {
            command.undo(snapshot);
            return VerificationResult.rolledBack("Rolled back: " + problem.get(), after);
        }
        audit.pushAppliedFix(command);
        audit.addScan(after);
        repository.save(audit);
        return VerificationResult.verified(after);
    }

    /** Why a fix fails verification, or empty if it passes (F09). */
    private static Optional<String> verificationProblem(String issueId, ScanResult before, ScanResult after) {
        if (!after.allRulesRan()) {
            return Optional.of("these rules didn't run, so the fix can't be checked: "
                    + String.join(", ", after.rulesNotRun()) + ".");
        }
        if (after.contains(issueId)) {
            return Optional.of("the issue is still there.");
        }
        List<String> newIssueIds = after.issues().stream()
                .map(Issue::id)
                .filter(id -> !before.contains(id))
                .toList();
        if (!newIssueIds.isEmpty()) {
            return Optional.of("the fix caused new issues: " + String.join(", ", newIssueIds) + ".");
        }
        return Optional.empty();
    }

    /**
     * Undoes the most recent applied fix, re-scans, and saves the new scan with the audit (F09,
     * UC09, SD07). If no fixes have been applied, there's nothing to undo: it throws
     * InvalidInputException so the user is told.
     */
    public ScanResult undoLast(String auditId) {
        Audit audit = findAudit(auditId);
        FixCommand command = audit.popAppliedFix()
                .orElseThrow(() -> new InvalidInputException("There's nothing to undo: no fixes have been applied."));
        PageSnapshot snapshot = audit.getSnapshot();
        command.undo(snapshot);
        ScanResult after = ruleEngine.scan(snapshot);
        audit.addScan(after);
        repository.save(audit);
        return after;
    }

    private Audit findAudit(String auditId) {
        return repository.findById(auditId)
                .orElseThrow(() -> new NotFoundException("No audit with ID " + auditId));
    }

    /**
     * The issue the proposal fixes, after checking that it's in the latest scan and that the fix
     * type can fix its rule (SD06). Used by applyVerified and preview, so both refuse the same fixes.
     */
    private static Issue allowedIssue(Audit audit, FixProposal proposal) {
        Issue issue = audit.latestScan().find(proposal.issueId())
                .orElseThrow(() -> new InvalidInputException(
                        "Issue " + proposal.issueId() + " isn't in the latest scan, so it can't be fixed."));
        if (proposal.fixType() == null || !proposal.fixType().canFix(issue.ruleId())) {
            throw new InvalidInputException(
                    "Fix type " + proposal.fixType() + " isn't allowed for " + issue.ruleId() + " issues.");
        }
        return issue;
    }

    /**
     * The smallest part of the page that changed, before and after. Starting at the issue's element,
     * it goes up until everything outside the element is unchanged. If the element's content is
     * unchanged, only its own attributes changed, so it shows the element. Otherwise it shows the
     * child nodes that changed, such as a label added before a field or a title added to the head.
     */
    static String affectedCode(Document before, Document after, String elementId) {
        String id = elementId;
        while (!changedOnlyInside(before, after, id)) {
            id = PageSnapshot.findElement(before, id).parent().attr(PageSnapshot.ID_ATTR);
        }
        Element oldElement = PageSnapshot.findElement(before, id);
        Element newElement = PageSnapshot.findElement(after, id);
        if (oldElement.html().equals(newElement.html())) {
            return beforeAndAfter(RuleSupport.snippet(oldElement), RuleSupport.snippet(newElement));
        }

        List<Node> oldNodes = oldElement.childNodes();
        List<Node> newNodes = newElement.childNodes();
        int start = 0;
        while (start < oldNodes.size() && start < newNodes.size()
                && sameCode(oldNodes.get(start), newNodes.get(start))) {
            start++;
        }
        int oldEnd = oldNodes.size();
        int newEnd = newNodes.size();
        while (oldEnd > start && newEnd > start && sameCode(oldNodes.get(oldEnd - 1), newNodes.get(newEnd - 1))) {
            oldEnd--;
            newEnd--;
        }
        return beforeAndAfter(code(oldNodes.subList(start, oldEnd)), code(newNodes.subList(start, newEnd)));
    }

    /** True if the two pages are the same once the element with this stable ID is removed from both. */
    private static boolean changedOnlyInside(Document before, Document after, String id) {
        return withoutElement(before, id).equals(withoutElement(after, id));
    }

    private static String withoutElement(Document doc, String id) {
        Document copy = doc.clone();
        PageSnapshot.findElement(copy, id).remove();
        return copy.outerHtml();
    }

    private static boolean sameCode(Node a, Node b) {
        return a.outerHtml().equals(b.outerHtml());
    }

    /** The nodes' code for display, without stable IDs, as in issue snippets. */
    private static String code(List<Node> nodes) {
        StringBuilder code = new StringBuilder();
        for (Node node : nodes) {
            code.append(node instanceof Element element ? RuleSupport.snippet(element) : node.outerHtml());
        }
        return code.toString();
    }

    private static String beforeAndAfter(String before, String after) {
        return "Before:\n" + (before.isEmpty() ? "(nothing)" : before)
                + "\n\nAfter:\n" + (after.isEmpty() ? "(nothing)" : after);
    }

    /**
     * Turns a proposal into the right FixCommand (F08). This is the only place that knows which
     * class goes with which FixType. It doesn't check that the fix type can fix the issue's rule:
     * applyVerified and preview do that first, using FixType.canFix.
     */
    FixCommand createCommand(Issue issue, FixProposal proposal) {
        return switch (proposal.fixType()) {
            case SET_ALT_TEXT -> new SetAltTextFix(issue.id(), issue.elementId(), proposal.value());
            case ADD_LABEL -> new AddLabelFix(issue.id(), issue.elementId(), proposal.value());
            case SET_ARIA_LABEL -> new SetAriaLabelFix(issue.id(), issue.elementId(), proposal.value());
            case SET_LANGUAGE -> new SetLanguageFix(issue.id(), issue.elementId(), proposal.value());
            case SET_TITLE -> new SetTitleFix(issue.id(), proposal.value());
        };
    }
}
