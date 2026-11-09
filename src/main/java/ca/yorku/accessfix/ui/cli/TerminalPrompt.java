package ca.yorku.accessfix.ui.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Reads the user's answers at the terminal: plan approval, fix decisions and alt text (Stage 1: "Plans
 * and fix proposals are approved or rejected at the terminal prompt"). Tests give it scripted input.
 */
@Component
@Profile("cli")
public class TerminalPrompt {

    private final BufferedReader in;

    public TerminalPrompt() {
        this(new BufferedReader(new InputStreamReader(System.in)));
    }

    TerminalPrompt(BufferedReader in) {
        this.in = in;
    }

    /** Prints the question and returns the trimmed line typed, or empty at the end of the input. */
    public Optional<String> ask(PrintWriter out, String question) {
        out.print(question + " ");
        out.flush();
        try {
            String line = in.readLine();
            return Optional.ofNullable(line).map(String::trim);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Asks until the answer is yes or no. Empty at the end of the input. */
    public Optional<Boolean> confirm(PrintWriter out, String question) {
        while (true) {
            Optional<String> answer = ask(out, question + " (y/n)");
            if (answer.isEmpty()) {
                return Optional.empty();
            }
            switch (answer.get().toLowerCase()) {
                case "y", "yes" -> {
                    return Optional.of(true);
                }
                case "n", "no" -> {
                    return Optional.of(false);
                }
                default -> out.println("Type y or n.");
            }
        }
    }
}
