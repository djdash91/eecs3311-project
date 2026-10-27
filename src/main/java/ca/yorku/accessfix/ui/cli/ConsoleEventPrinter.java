package ca.yorku.accessfix.ui.cli;

import ca.yorku.accessfix.agent.events.AgentEvent;
import ca.yorku.accessfix.agent.events.AgentEventListener;

/** Prints agent events in the terminal (Observer, view A). Extend the format as you like. */
public class ConsoleEventPrinter implements AgentEventListener {

    @Override
    public void onEvent(AgentEvent event) {
        System.out.println("[" + event.type().name().toLowerCase() + "] " + event.message());
    }
}
