package ca.yorku.accessfix.ui.cli;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

/**
 * The top-level "accessfix" command, with the eleven commands of Stage 1, section 1.1. load prints
 * the new audit's ID, and every other command except wcag takes it.
 */
@Component
@Profile("cli")
@Command(name = "accessfix", mixinStandardHelpOptions = true,
        description = "Audit and repair web accessibility issues.",
        subcommands = {LoadCommand.class, ScanCommand.class, ExplainCommand.class, AltCommand.class,
                AskCommand.class, PlanCommand.class, FixSessionCommand.class, UndoCommand.class,
                ReportCommand.class, ExportCommand.class, WcagCommand.class})
public class RootCommand {
}
