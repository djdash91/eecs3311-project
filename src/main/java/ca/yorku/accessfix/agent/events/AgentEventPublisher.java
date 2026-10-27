package ca.yorku.accessfix.agent.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Observer pattern subject. The agent publishes events here without knowing whether the GUI or the
 * CLI is listening.
 */
@Component
public class AgentEventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(AgentEventPublisher.class);

    private final List<AgentEventListener> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(AgentEventListener listener) {
        listeners.add(listener);
    }

    public void unsubscribe(AgentEventListener listener) {
        listeners.remove(listener);
    }

    /** A broken listener (for example a closed browser tab) must not stop the agent, so errors are logged. */
    public void publish(AgentEvent event) {
        for (AgentEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (RuntimeException e) {
                LOG.warn("Event listener failed; removing it", e);
                listeners.remove(listener);
            }
        }
    }
}
