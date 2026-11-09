package ca.yorku.accessfix.agent;

import ca.yorku.accessfix.common.NotFoundException;
import ca.yorku.accessfix.domain.Audit;
import ca.yorku.accessfix.domain.Decision;
import ca.yorku.accessfix.persistence.AuditRepository;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Keeps one AgentSession per audit (in memory) and stores decisions, failure counts and escalations
 * on the Audit (persisted through AuditRepository, so they survive restarts, F11).
 */
@Service
public class MemoryManager {

    private final AuditRepository repository;
    private final Map<String, AgentSession> sessions = new ConcurrentHashMap<>();

    public MemoryManager(AuditRepository repository) {
        this.repository = repository;
    }

    public AgentSession sessionFor(String auditId) {
        return sessions.computeIfAbsent(auditId, AgentSession::new);
    }

    /**
     * The audit's rejected decisions, oldest first, read from storage so they survive restarts (F11).
     * Only rejections are stored, but approvals are filtered out here too, so the agent is only ever
     * told what the user refused.
     */
    public List<Decision> loadDecisions(String auditId) {
        return findAudit(auditId).getDecisions().stream().filter(decision -> !decision.approved()).toList();
    }

    /**
     * Saves a rejection with the audit, so every later run is told about it (F11, SD06). Approvals
     * aren't stored: an approved fix is already recorded as an applied fix.
     */
    public void recordDecision(String auditId, Decision decision) {
        if (decision.approved()) {
            return;
        }
        Audit audit = findAudit(auditId);
        audit.addDecision(decision);
        repository.save(audit);
    }

    /** Adds one failed fix for the issue, saves, and returns the new count (F08, SD06). */
    public int recordFailure(String auditId, String issueId) {
        Audit audit = findAudit(auditId);
        int failures = audit.incrementFailures(issueId);
        repository.save(audit);
        return failures;
    }

    /**
     * Records the issue as escalated, with why, and saves (F08, UC08 2b and 4a). The reason is shown
     * in the report. Escalating an issue again keeps the first reason.
     */
    public void recordEscalation(String auditId, String issueId, String reason) {
        Audit audit = findAudit(auditId);
        audit.markEscalated(issueId, reason);
        repository.save(audit);
    }

    private Audit findAudit(String auditId) {
        return repository.findById(auditId)
                .orElseThrow(() -> new NotFoundException("No audit with ID " + auditId));
    }
}
