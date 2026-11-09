package ca.yorku.accessfix.ui.cli;

import ca.yorku.accessfix.domain.FixProposal;
import ca.yorku.accessfix.domain.FixType;
import ca.yorku.accessfix.domain.VerificationResult;
import ca.yorku.accessfix.facade.AccessFixFacade;
import java.io.PrintWriter;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code accessfix alt <auditId> <issueId>}: suggests alt text for an image, lets the user accept or
 * change it, then applies and verifies it (F07, UC07, F09).
 */
@Component
@Profile("cli")
@Command(name = "alt", description = "Suggest alt text for an image, then apply it.")
public class AltCommand implements Callable<Integer> {

    static final String CANCEL = "cancel";

    @Parameters(index = "0", description = "The audit ID printed by load")
    private String auditId;

    @Parameters(index = "1", description = "The image issue's ID, as scan lists it")
    private String issueId;

    @Spec
    private CommandSpec spec;

    private final AccessFixFacade facade;
    private final TerminalPrompt prompt;

    public AltCommand(AccessFixFacade facade, TerminalPrompt prompt) {
        this.facade = facade;
        this.prompt = prompt;
    }

    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        Optional<String> suggestion = facade.suggestAltText(auditId, issueId);
        Optional<String> typed;
        if (suggestion.isPresent()) {
            out.println("Suggested alt text: " + suggestion.get());
            typed = prompt.ask(out, "Press Enter to apply it, type different alt text, or type " + CANCEL + ":");
        } else {
            // UC07 1a and 2a: no screenshot, or the suggestion failed its checks or the API failed.
            out.println("No suggestion could be made, so write the alt text yourself.");
            typed = prompt.ask(out, "Alt text to apply, or " + CANCEL + ":");
        }
        String text = typed.map(t -> t.isEmpty() ? suggestion.orElse("") : t).orElse(CANCEL);
        if (text.isEmpty() || text.equalsIgnoreCase(CANCEL)) {
            out.println("Nothing was applied.");
            return 0;
        }
        VerificationResult result = facade.applyFix(auditId, new FixProposal(issueId, FixType.SET_ALT_TEXT, text));
        out.println(result.reason());
        out.println("Issues now: " + result.scanAfter().issueCount());
        return result.kept() ? 0 : 1;
    }
}
