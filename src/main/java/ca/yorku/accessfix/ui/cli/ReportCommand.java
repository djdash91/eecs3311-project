package ca.yorku.accessfix.ui.cli;

import ca.yorku.accessfix.facade.AccessFixFacade;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/** {@code accessfix report <auditId> [-o file]}: the Markdown report (F10, UC10, SD07). */
@Component
@Profile("cli")
@Command(name = "report", description = "Create the Markdown report.")
public class ReportCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "The audit ID printed by load")
    private String auditId;

    @Option(names = {"-o", "--output"}, description = "The file to save it to; without it, the report is printed")
    private Path output;

    @Spec
    private CommandSpec spec;

    private final AccessFixFacade facade;

    public ReportCommand(AccessFixFacade facade) {
        this.facade = facade;
    }

    @Override
    public Integer call() throws Exception {
        OutputFile.write(facade.exportReport(auditId), output, spec.commandLine().getOut(), "report");
        return 0;
    }
}
