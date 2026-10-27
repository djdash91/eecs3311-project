package ca.yorku.accessfix.ui.cli;

import ca.yorku.accessfix.domain.Issue;
import ca.yorku.accessfix.domain.ScanResult;
import ca.yorku.accessfix.facade.AccessFixFacade;
import java.util.concurrent.Callable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/** WORKED EXAMPLE: {@code accessfix scan <auditId>} re-scans and lists issues. */
@Component
@Profile("cli")
@Command(name = "scan", description = "Re-scan an audit's page and list its issues.")
public class ScanCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "The audit ID printed by load")
    private String auditId;

    private final AccessFixFacade facade;

    public ScanCommand(AccessFixFacade facade) {
        this.facade = facade;
    }

    @Override
    public Integer call() {
        ScanResult result = facade.scan(auditId);
        if (result.issues().isEmpty()) {
            System.out.println("No issues found by the automated rules. Manual testing is still needed.");
        }
        for (Issue issue : result.issues()) {
            System.out.printf("%-24s WCAG %-6s %s%n", issue.id(), issue.criterion(), issue.message());
        }
        if (!result.allRulesRan()) {
            System.out.println("Rules not run: " + String.join(", ", result.rulesNotRun()));
        }
        return 0;
    }
}
