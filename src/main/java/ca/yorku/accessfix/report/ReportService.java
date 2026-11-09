package ca.yorku.accessfix.report;

import ca.yorku.accessfix.domain.Audit;
import ca.yorku.accessfix.domain.Issue;
import ca.yorku.accessfix.domain.PageSnapshot;
import ca.yorku.accessfix.domain.ScanResult;
import ca.yorku.accessfix.fix.FixCommand;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

/**
 * Builds the Markdown report and the fixed page (F10, SD07).
 */
@Service
public class ReportService {

    /** F10: "a note that automated checks find only some barriers". Says what was checked, not what the page is. */
    static final String AUTOMATED_CHECKS_NOTE = "Automated checks find only some accessibility barriers. AccessFix "
            + "checks six things: image text alternatives, form labels, link names, button names, the page language "
            + "and the page title. Many WCAG 2.2 success criteria, such as keyboard access, colour contrast and "
            + "whether alt text is accurate, need a person to check them. An issue count of zero means these rules "
            + "found nothing, not that the page has no barriers. Test the page with a keyboard and a screen reader, "
            + "and with disabled people where possible.";

    /**
     * Builds a Markdown report with the issue counts from the first and latest scans, the verified
     * fixes, the escalated issues and a note that automated checks find only some barriers (F10).
     * It also lists the issues in the latest scan, so a report with no fixes still shows the
     * starting issues (UC10 1a).
     *
     * <p>The wording describes what was found and changed, never that the page "is accessible". The
     * report is accessible itself: headings in order, and every table has a header row.
     */
    public String generate(Audit audit) {
        ScanResult first = audit.firstScan();
        ScanResult latest = audit.latestScan();
        List<FixCommand> fixes = audit.getAppliedFixes().reversed();
        List<String> escalated = audit.getEscalatedIssueIds().stream().sorted().toList();

        StringBuilder md = new StringBuilder();
        md.append("# Accessibility report\n\n");
        md.append("- **Page:** ").append(cell(audit.getSource())).append('\n');
        md.append("- **Audit ID:** ").append(audit.getId()).append("\n\n");

        md.append("## Summary\n\n");
        md.append("| Scan | Issues found |\n|---|---|\n");
        md.append("| First scan | ").append(first.issueCount()).append(" |\n");
        md.append("| Latest scan | ").append(latest.issueCount()).append(" |\n\n");
        md.append(count(fixes.size(), "fix was", "fixes were")).append(" applied and verified, and ")
                .append(count(escalated.size(), "issue was", "issues were")).append(" escalated for a person to fix.\n\n");
        appendRulesNotRun(md, "first", first);
        appendRulesNotRun(md, "latest", latest);
        appendCountsByRule(md, first, latest);

        md.append("## Verified fixes\n\n");
        if (fixes.isEmpty()) {
            md.append("No fixes have been applied yet.\n\n");
        } else {
            md.append("Each fix was kept only after a re-scan showed that its issue was gone and no new issues appeared. "
                    + "They are listed in the order they were applied.\n\n");
            md.append("| Order | Issue | WCAG criterion | Change |\n|---|---|---|---|\n");
            for (int i = 0; i < fixes.size(); i++) {
                FixCommand fix = fixes.get(i);
                md.append("| ").append(i + 1)
                        .append(" | ").append(code(fix.issueId()))
                        .append(" | ").append(findIssue(audit, fix.issueId()).map(Issue::criterion).orElse("Unknown"))
                        .append(" | ").append(cell(fix.description())).append(" |\n");
            }
            md.append('\n');
        }

        md.append("## Escalated issues\n\n");
        if (escalated.isEmpty()) {
            md.append("No issues have been escalated.\n\n");
        } else {
            md.append("These issues need a person to fix them.\n\n");
            md.append("| Issue | WCAG criterion | Element | Problem | Why it was escalated | In the latest scan |\n|---|---|---|---|---|---|\n");
            for (String issueId : escalated) {
                Optional<Issue> issue = findIssue(audit, issueId);
                md.append("| ").append(code(issueId))
                        .append(" | ").append(issue.map(Issue::criterion).orElse("Unknown"))
                        .append(" | ").append(issue.map(i -> code(i.snippet())).orElse("Unknown"))
                        .append(" | ").append(issue.map(i -> cell(i.message())).orElse("Unknown"))
                        .append(" | ").append(audit.escalationReason(issueId).map(ReportService::cell).orElse("Not recorded"))
                        .append(" | ").append(latest.contains(issueId) ? "Still found" : "Not found").append(" |\n");
            }
            md.append('\n');
        }

        md.append("## Issues in the latest scan\n\n");
        if (latest.issues().isEmpty()) {
            md.append("The automated rules found no issues in the latest scan. Manual testing is still needed.\n\n");
        } else {
            md.append("| Issue | WCAG criterion | Element | Problem |\n|---|---|---|---|\n");
            for (Issue issue : latest.issues()) {
                md.append("| ").append(code(issue.id()))
                        .append(" | ").append(issue.criterion())
                        .append(" | ").append(code(issue.snippet()))
                        .append(" | ").append(cell(issue.message())).append(" |\n");
            }
            md.append('\n');
        }

        md.append("## About this report\n\n").append(AUTOMATED_CHECKS_NOTE).append('\n');
        return md.toString();
    }

    /** The snapshot's current HTML with every stable ID attribute removed (F10: "Download fixed page"). */
    public String exportPatchedPage(Audit audit) {
        Document doc = audit.getSnapshot().parse();
        doc.getAllElements().removeAttr(PageSnapshot.ID_ATTR);
        return doc.outerHtml();
    }

    private static void appendRulesNotRun(StringBuilder md, String which, ScanResult scan) {
        if (!scan.allRulesRan()) {
            md.append("**Some rules didn't run in the ").append(which).append(" scan:** ")
                    .append(String.join(", ", scan.rulesNotRun()))
                    .append(". Its count leaves out anything those rules would have found.\n\n");
        }
    }

    /** One row per rule that found issues in either scan, so the reader can see where the change came from. */
    private static void appendCountsByRule(StringBuilder md, ScanResult first, ScanResult latest) {
        Map<String, Long> firstCounts = countByRule(first);
        Map<String, Long> latestCounts = countByRule(latest);
        Map<String, String> criteria = new TreeMap<>();
        Stream.concat(first.issues().stream(), latest.issues().stream())
                .forEach(issue -> criteria.putIfAbsent(issue.ruleId(), issue.criterion()));
        if (criteria.isEmpty()) {
            return;
        }
        md.append("### Issues by rule\n\n");
        md.append("| Rule | WCAG criterion | First scan | Latest scan |\n|---|---|---|---|\n");
        criteria.forEach((ruleId, criterion) -> md.append("| ").append(code(ruleId))
                .append(" | ").append(criterion)
                .append(" | ").append(firstCounts.getOrDefault(ruleId, 0L))
                .append(" | ").append(latestCounts.getOrDefault(ruleId, 0L)).append(" |\n"));
        md.append('\n');
    }

    private static Map<String, Long> countByRule(ScanResult scan) {
        return scan.issues().stream().collect(Collectors.groupingBy(Issue::ruleId, Collectors.counting()));
    }

    /** The issue from the latest scan that has it, so a fixed or escalated issue still shows its details. */
    private static Optional<Issue> findIssue(Audit audit, String issueId) {
        return audit.getScans().reversed().stream()
                .map(scan -> scan.find(issueId))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private static String count(int n, String singular, String plural) {
        return n + " " + (n == 1 ? singular : plural);
    }

    /** Text for a table cell: one line, with pipes escaped so they don't end the cell. */
    static String cell(String text) {
        return text == null ? "" : text.replaceAll("\\s*\\R\\s*", " ").replace("|", "\\|");
    }

    /**
     * Text as inline code in a table cell. Code that contains backticks is wrapped in two, with
     * spaces, as Markdown requires.
     */
    static String code(String text) {
        String safe = cell(text);
        return safe.contains("`") ? "`` " + safe + " ``" : "`" + safe + "`";
    }
}
