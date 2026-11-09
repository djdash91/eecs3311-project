package ca.yorku.accessfix.persistence;

import ca.yorku.accessfix.domain.Audit;
import ca.yorku.accessfix.fix.AddLabelFix;
import ca.yorku.accessfix.fix.FixCommand;
import ca.yorku.accessfix.fix.SetAltTextFix;
import ca.yorku.accessfix.fix.SetAriaLabelFix;
import ca.yorku.accessfix.fix.SetLanguageFix;
import ca.yorku.accessfix.fix.SetTitleFix;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Stores audits in the H2 file database so they survive restarts and carry over between CLI
 * commands (F11: "including after the app restarts"). Active when {@code accessfix.storage=h2}.
 *
 * <p>The table is created by src/main/resources/schema.sql: one row per audit, the whole Audit
 * stored as JSON.
 *
 * <p>The JSON is read from and written to the classes' fields, not their getters, so everything an
 * Audit holds is stored, including each fix's undo information (such as the old alt text). Getters
 * return copies or computed values, such as latestScan(), so they aren't stored. Each applied fix
 * is stored with its FixType name ("type": "SET_ALT_TEXT"), so it is read back as the right class.
 * The domain classes have no JSON annotations: the type names are added here, through a mix-in.
 * Final fields, such as Audit's list of scans, are filled in too, so the classes can keep them final.
 */
@Repository
@ConditionalOnProperty(name = "accessfix.storage", havingValue = "h2")
public class JdbcAuditRepository implements AuditRepository {

    /** Type names for the FixCommand classes. Add a line here for each new fix class. */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = SetAltTextFix.class, name = "SET_ALT_TEXT"),
            @JsonSubTypes.Type(value = AddLabelFix.class, name = "ADD_LABEL"),
            @JsonSubTypes.Type(value = SetAriaLabelFix.class, name = "SET_ARIA_LABEL"),
            @JsonSubTypes.Type(value = SetLanguageFix.class, name = "SET_LANGUAGE"),
            @JsonSubTypes.Type(value = SetTitleFix.class, name = "SET_TITLE")
    })
    private interface FixCommandTypes {
    }

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .changeDefaultVisibility(visibility -> visibility
                    .withFieldVisibility(Visibility.ANY)
                    .withGetterVisibility(Visibility.NONE)
                    .withIsGetterVisibility(Visibility.NONE)
                    .withSetterVisibility(Visibility.NONE))
            .enable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS)
            .addMixIn(FixCommand.class, FixCommandTypes.class)
            .build();

    private final JdbcClient jdbc;

    public JdbcAuditRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Inserts the audit, or replaces the stored copy if one with the same ID exists. */
    @Override
    public void save(Audit audit) {
        jdbc.sql("MERGE INTO audit (id, data) KEY (id) VALUES (?, ?)")
                .params(audit.getId(), MAPPER.writeValueAsString(audit))
                .update();
    }

    @Override
    public Optional<Audit> findById(String auditId) {
        return jdbc.sql("SELECT data FROM audit WHERE id = ?")
                .params(auditId)
                .query(String.class)
                .optional()
                .map(json -> read(auditId, json));
    }

    private static Audit read(String auditId, String json) {
        try {
            return MAPPER.readValue(json, Audit.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("The stored audit " + auditId + " can't be read: " + e.getOriginalMessage(), e);
        }
    }
}
