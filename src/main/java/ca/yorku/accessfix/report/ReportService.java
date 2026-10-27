package ca.yorku.accessfix.report;

import ca.yorku.accessfix.domain.Audit;
import org.springframework.stereotype.Service;

/**
 * Builds the Markdown report and the fixed page (F10, SD07).
 */
@Service
public class ReportService {

    /**
     * TODO(F10): build a Markdown report with the issue counts from the first and latest scans, the
     * verified fixes, the escalated issues, and a note that automated checks find only some barriers.
     *
     * <p>Wording matters here: describe what was found and changed. Don't say the page "is accessible"
     * or "passes WCAG", because six automated rules can't establish that.
     *
     * <p>Make the report itself accessible: real headings in order, tables with header rows, and link
     * text that describes the destination.
     */
    public String generate(Audit audit) {
        throw new UnsupportedOperationException("TODO: ReportService.generate");
    }

    /**
     * TODO(F10): return the snapshot's current HTML with every data-af-id attribute removed.
     * Test that a page with no fixes comes back with no data-af-id left in it.
     */
    public String exportPatchedPage(Audit audit) {
        throw new UnsupportedOperationException("TODO: ReportService.exportPatchedPage");
    }
}
