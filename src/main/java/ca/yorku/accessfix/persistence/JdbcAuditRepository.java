package ca.yorku.accessfix.persistence;

import ca.yorku.accessfix.domain.Audit;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Stores audits in the H2 file database so they survive restarts and carry over between CLI
 * commands (F11: "including after the app restarts"). Active when {@code accessfix.storage=h2}.
 *
 * <p>The table is created by src/main/resources/schema.sql: one row per audit, the whole Audit
 * stored as JSON.
 *
 * <p>TODO: implement save and findById.
 * <ul>
 *   <li>Serialise the Audit to JSON and back. Audit has private fields and no setters, so either
 *       configure the mapper to use fields, or write small record DTOs for storage (often cleaner).</li>
 *   <li>The hard part: appliedFixes holds FixCommand objects of several classes. The JSON needs a
 *       type name per fix so it can be read back as the right class. Decide how, and test the round
 *       trip for every fix type.</li>
 *   <li>Use MERGE INTO audit (id, data) KEY (id) VALUES (?, ?) for insert-or-update in H2.</li>
 * </ul>
 * Record the storage design in docs/DESIGN-CHANGES.md: Stage 1 doesn't specify it.
 */
@Repository
@ConditionalOnProperty(name = "accessfix.storage", havingValue = "h2")
public class JdbcAuditRepository implements AuditRepository {

    private final JdbcClient jdbc;

    public JdbcAuditRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(Audit audit) {
        throw new UnsupportedOperationException("TODO: JdbcAuditRepository.save");
    }

    @Override
    public Optional<Audit> findById(String auditId) {
        throw new UnsupportedOperationException("TODO: JdbcAuditRepository.findById");
    }
}
