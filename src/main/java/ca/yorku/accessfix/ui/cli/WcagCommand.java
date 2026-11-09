package ca.yorku.accessfix.ui.cli;

import ca.yorku.accessfix.facade.AccessFixFacade;
import ca.yorku.accessfix.wcag.WcagCriterion;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.Callable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/** {@code accessfix wcag <query...>}: searches the WCAG 2.2 A and AA criteria (F05, UC05). No audit needed. */
@Component
@Profile("cli")
@Command(name = "wcag", description = "Search the WCAG 2.2 Level A and AA criteria.")
public class WcagCommand implements Callable<Integer> {

    @Parameters(arity = "1..*", description = "Words to search for; quotes are optional")
    private List<String> query;

    @Spec
    private CommandSpec spec;

    private final AccessFixFacade facade;

    public WcagCommand(AccessFixFacade facade) {
        this.facade = facade;
    }

    @Override
    public Integer call() {
        String text = String.join(" ", query);
        List<WcagCriterion> results = facade.searchWcag(text);
        PrintWriter out = spec.commandLine().getOut();
        if (results.isEmpty()) {
            out.println("No criteria match \"" + text + "\".");
        }
        for (WcagCriterion c : results) {
            out.println(c.number() + " " + c.title() + " (Level " + c.level() + ")");
            out.println("  " + c.summary());
            out.println("  " + c.url());
        }
        return 0;
    }
}
