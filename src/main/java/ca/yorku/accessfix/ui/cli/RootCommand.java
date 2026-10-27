package ca.yorku.accessfix.ui.cli;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

/**
 * The top-level "accessfix" command. LoadCommand and ScanCommand are worked examples.
 *
 * <p>TODO: add explain, alt, ask, plan, fix, undo, report, export and wcag (Stage 1, 1.1), each a
 * small class like ScanCommand. For fix: loop while the AgentResult says a fix is pending, read
 * approve or reject (and a reason) from the terminal, and call facade.submitDecision().
 */
@Component
@Profile("cli")
@Command(name = "accessfix", mixinStandardHelpOptions = true,
        description = "Audit and repair web accessibility issues.",
        subcommands = {LoadCommand.class, ScanCommand.class})
public class RootCommand {
}
