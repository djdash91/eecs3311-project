package ca.yorku.accessfix.ui.rest;

import ca.yorku.accessfix.domain.Audit;
import ca.yorku.accessfix.domain.ScanResult;

/**
 * What the GUI needs after loading a page: the audit's ID, where the page came from and its latest
 * scan. Sending the whole Audit would include the snapshot's HTML and screenshots, and Jackson can't
 * serialise its FixCommand objects.
 */
public record AuditSummary(String id, String source, ScanResult latestScan) {

    public static AuditSummary of(Audit audit) {
        return new AuditSummary(audit.getId(), audit.getSource(), audit.latestScan());
    }
}
