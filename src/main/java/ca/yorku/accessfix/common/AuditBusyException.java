package ca.yorku.accessfix.common;

/** A run is in progress or a fix is waiting for approval (Stage 1, section 1.1, step 4). REST maps it to 409. */
public class AuditBusyException extends AccessFixException {
    public AuditBusyException(String auditId) {
        super("Audit " + auditId + " is busy: wait for the agent, or decide on the pending fix first.");
    }
}
