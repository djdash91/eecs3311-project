package ca.yorku.accessfix.ui.cli;

import ca.yorku.accessfix.domain.Explanation;
import ca.yorku.accessfix.facade.AccessFixFacade;
import java.io.PrintWriter;
import java.util.concurrent.Callable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/** {@code accessfix explain <auditId> <issueId>}: explains an issue in plain language (F03, UC03). */
@Component
@Profile("cli")
@Command(name = "explain", description = "Explain an issue in plain language.")
public class ExplainCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "The audit ID printed by load")
    private String auditId;

    @Parameters(index = "1", description = "The issue ID, as scan lists it")
    private String issueId;

    @Spec
    private CommandSpec spec;

    private final AccessFixFacade facade;

    public ExplainCommand(AccessFixFacade facade) {
        this.facade = facade;
    }

    @Override
    public Integer call() {
        Explanation explanation = facade.explainIssue(auditId, issueId);
        PrintWriter out = spec.commandLine().getOut();
        // The fallback (UC03 2a) has only the rule message and the criterion.
        print(out, "What is wrong", explanation.whatIsWrong());
        print(out, "Who is affected", explanation.whoIsAffected());
        print(out, "WCAG criterion", explanation.criterion());
        print(out, "How to fix it", explanation.howToFix());
        return 0;
    }

    private static void print(PrintWriter out, String label, String value) {
        if (value != null && !value.isBlank()) {
            out.println(label + ": " + value);
        }
    }
}
