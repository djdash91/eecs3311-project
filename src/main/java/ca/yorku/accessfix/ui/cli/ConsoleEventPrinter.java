package ca.yorku.accessfix.ui.cli;

import ca.yorku.accessfix.agent.ApprovalGate;
import ca.yorku.accessfix.agent.events.AgentEvent;
import ca.yorku.accessfix.agent.events.AgentEventListener;
import java.io.PrintWriter;

/**
 * Prints agent events in the terminal (Observer, view A): the CLI's version of the agent panel. A
 * fix proposal is printed with its before-and-after code, so the user can decide at the prompt (SD05).
 */
public class ConsoleEventPrinter implements AgentEventListener {

    private final PrintWriter out;

    public ConsoleEventPrinter() {
        this(new PrintWriter(System.out, true));
    }

    ConsoleEventPrinter(PrintWriter out) {
        this.out = out;
    }

    @Override
    public void onEvent(AgentEvent event) {
        out.println("[" + event.type().name().toLowerCase() + "] " + event.message());
        if (event.data() instanceof ApprovalGate.ProposedFix proposed) {
            out.println();
            out.println(proposed.beforeAndAfter());
            out.println();
        }
        out.flush();
    }
}
