package ca.yorku.accessfix.ui.cli;

import ca.yorku.accessfix.agent.AgentResult;
import ca.yorku.accessfix.facade.AccessFixFacade;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code accessfix fix <auditId>}: runs a fix session through the approved plan (F08, UC08, SD05,
 * SD06). ConsoleEventPrinter shows each proposal with its before-and-after code; this command reads
 * the decision and calls submitDecision until no fix is waiting (Stage 1, section 1.1).
 *
 * <p>Not called FixCommand: that is the Command pattern's interface in the fix package.
 */
@Component
@Profile("cli")
@Command(name = "fix", description = "Run a fix session: approve or reject each proposed fix.")
public class FixSessionCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "The audit ID printed by load")
    private String auditId;

    @Spec
    private CommandSpec spec;

    private final AccessFixFacade facade;
    private final TerminalPrompt prompt;

    public FixSessionCommand(AccessFixFacade facade, TerminalPrompt prompt) {
        this.facade = facade;
        this.prompt = prompt;
    }

    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        AgentResult result = facade.startFixSession(auditId);
        while (result.fixPending()) {
            Optional<Boolean> approved = prompt.confirm(out, "Approve this fix?");
            if (approved.isEmpty()) {
                // The input ended. The session lives only in this process, so nothing stays waiting.
                out.println("No decision was given, so the session stopped. Verified fixes are kept.");
                return 1;
            }
            String reason = null;
            if (!approved.get()) {
                reason = prompt.ask(out, "Reason (optional, press Enter to skip):").filter(r -> !r.isEmpty()).orElse(null);
            }
            result = facade.submitDecision(auditId, approved.get(), reason);
        }
        printSummary(out, result);
        return 0;
    }

    private static void printSummary(PrintWriter out, AgentResult result) {
        out.println();
        out.println("Fix session finished.");
        if (result.stepLimitReached()) {
            out.println("It stopped at the step limit. Run fix again to continue with what is left.");
        }
        if (result.answer() != null && !result.answer().isBlank()) {
            out.println(result.answer());
        }
        Map<String, Integer> counts = result.issueCounts() == null ? Map.of() : result.issueCounts();
        out.println("Issues left: " + counts.values().stream().mapToInt(Integer::intValue).sum());
        counts.forEach((rule, count) -> out.println("  " + rule + ": " + count));
    }
}
