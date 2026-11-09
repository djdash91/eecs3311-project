package ca.yorku.accessfix.ui.cli;

import ca.yorku.accessfix.domain.PlanItem;
import ca.yorku.accessfix.domain.RemediationPlan;
import ca.yorku.accessfix.facade.AccessFixFacade;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code accessfix plan <auditId> [--goal text]}: the agent creates a remediation plan, and the user
 * approves it at the prompt (F06, UC06). A plan that isn't approved stays saved, unapproved; running
 * plan again replaces it.
 */
@Component
@Profile("cli")
@Command(name = "plan", description = "Create a remediation plan, then approve it.")
public class PlanCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "The audit ID printed by load")
    private String auditId;

    @Option(names = {"-g", "--goal"}, description = "What to focus on, for example \"fix the sign-up form first\"")
    private String goal;

    @Spec
    private CommandSpec spec;

    private final AccessFixFacade facade;
    private final TerminalPrompt prompt;

    public PlanCommand(AccessFixFacade facade, TerminalPrompt prompt) {
        this.facade = facade;
        this.prompt = prompt;
    }

    @Override
    public Integer call() {
        RemediationPlan plan = facade.createPlan(auditId, goal);
        PrintWriter out = spec.commandLine().getOut();
        out.println();
        out.println("Remediation plan, most urgent first:");
        plan.getItems().stream()
                .sorted(Comparator.comparingInt(PlanItem::priority))
                .forEach(item -> out.printf("%3d  %-24s %-16s %s%n", item.priority(), item.issueId(),
                        item.isManual() ? "manual" : item.fixType().toolName(), item.reason()));

        Optional<Boolean> approved = prompt.confirm(out, "Approve this plan?");
        if (approved.orElse(false)) {
            facade.approvePlan(auditId);
            out.println("Plan approved. Run fix " + auditId + " to start fixing.");
        } else {
            out.println("The plan wasn't approved. Run plan again to create a new one.");
        }
        return 0;
    }
}
