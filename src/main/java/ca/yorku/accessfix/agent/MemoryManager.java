package ca.yorku.accessfix.agent;

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

    /** TODO(F11): load the audit and return its stored (rejected) decisions. */
    public List<Decision> loadDecisions(String auditId) {
        throw new UnsupportedOperationException("TODO: MemoryManager.loadDecisions");
    }

    /** TODO(F11): add the decision to the audit and save it. Only rejections are stored. */
    public void recordDecision(String auditId, Decision decision) {
        throw new UnsupportedOperationException("TODO: MemoryManager.recordDecision");
    }

    /** TODO(F08): add one failure for the issue, save, and return the new count. */
    public int recordFailure(String auditId, String issueId) {
        throw new UnsupportedOperationException("TODO: MemoryManager.recordFailure");
    }

    /** TODO(F08): mark the issue escalated on the audit and save. */
    public void recordEscalation(String auditId, String issueId) {
        throw new UnsupportedOperationException("TODO: MemoryManager.recordEscalation");
    }
}
