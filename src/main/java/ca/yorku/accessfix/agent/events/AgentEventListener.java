package ca.yorku.accessfix.agent.events;

/** Observer: implemented by SseEventStream (GUI) and ConsoleEventPrinter (CLI). */
public interface AgentEventListener {
    void onEvent(AgentEvent event);
}
