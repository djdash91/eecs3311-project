package ca.yorku.accessfix.ui.cli;

import ca.yorku.accessfix.agent.AgentResult;
import ca.yorku.accessfix.facade.AccessFixFacade;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.Callable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code accessfix ask <auditId> <question...>}: asks the agent about the audit (F04, UC04). The
 * conversation lasts only for this command (Stage 1, section 1.1).
 */
@Component
@Profile("cli")
@Command(name = "ask", description = "Ask the agent a question about the audit.")
public class AskCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "The audit ID printed by load")
    private String auditId;

    @Parameters(index = "1..*", arity = "1..*", description = "The question; quotes are optional")
    private List<String> question;

    @Spec
    private CommandSpec spec;

    private final AccessFixFacade facade;

    public AskCommand(AccessFixFacade facade) {
        this.facade = facade;
    }

    @Override
    public Integer call() {
        AgentResult result = facade.sendMessage(auditId, String.join(" ", question));
        PrintWriter out = spec.commandLine().getOut();
        out.println();
        out.println(result.answer());
        if (result.stepLimitReached()) {
            out.println("(The agent reached its step limit, so this answer may be incomplete.)");
        }
        return 0;
    }
}
