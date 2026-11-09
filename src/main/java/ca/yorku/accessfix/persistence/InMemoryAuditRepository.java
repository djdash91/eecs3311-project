package ca.yorku.accessfix.persistence;

import ca.yorku.accessfix.domain.Audit;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

/**
 * Keeps audits in memory. Active when {@code accessfix.storage=memory}, and used directly in unit
 * tests. Audits are lost when the process ends, so CLI commands can't share them: the application
 * uses JdbcAuditRepository ({@code accessfix.storage=h2}) by default.
 */
@Repository
@ConditionalOnProperty(name = "accessfix.storage", havingValue = "memory", matchIfMissing = true)
public class InMemoryAuditRepository implements AuditRepository {

    private final Map<String, Audit> audits = new ConcurrentHashMap<>();

    @Override
    public void save(Audit audit) {
        audits.put(audit.getId(), audit);
    }

    @Override
    public Optional<Audit> findById(String auditId) {
        return Optional.ofNullable(audits.get(auditId));
    }
}
