package ca.yorku.accessfix.ui.cli;

import ca.yorku.accessfix.domain.ScanResult;
import ca.yorku.accessfix.facade.AccessFixFacade;
import java.util.concurrent.Callable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/** {@code accessfix undo <auditId>}: undoes the most recent fix and re-scans (F09, UC09, SD07). */
@Component
@Profile("cli")
@Command(name = "undo", description = "Undo the most recent fix.")
public class UndoCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "The audit ID printed by load")
    private String auditId;

    @Spec
    private CommandSpec spec;

    private final AccessFixFacade facade;

    public UndoCommand(AccessFixFacade facade) {
        this.facade = facade;
    }

    @Override
    public Integer call() {
        ScanResult scan = facade.undoLastFix(auditId);
        spec.commandLine().getOut().println("Undid the most recent fix. Issues now: " + scan.issueCount());
        return 0;
    }
}
