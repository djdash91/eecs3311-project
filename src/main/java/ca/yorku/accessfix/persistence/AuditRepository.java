package ca.yorku.accessfix.persistence;

import ca.yorku.accessfix.domain.Audit;
import java.util.Optional;

/**
 * Stores whole Audits (view C). The core depends on this interface, not on H2, so tests can use
 * InMemoryAuditRepository (dependency inversion).
 */
public interface AuditRepository {

    void save(Audit audit);

    Optional<Audit> findById(String auditId);
}
