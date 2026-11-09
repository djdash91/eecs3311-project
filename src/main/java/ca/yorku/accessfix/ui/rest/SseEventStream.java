package ca.yorku.accessfix.ui.rest;

import ca.yorku.accessfix.agent.events.AgentEvent;
import ca.yorku.accessfix.agent.events.AgentEventListener;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Sends one audit's agent events to one browser tab over Server-Sent Events (Observer, view A). */
public class SseEventStream implements AgentEventListener {

    private final String auditId;
    private final SseEmitter emitter;

    public SseEventStream(String auditId, SseEmitter emitter) {
        this.auditId = auditId;
        this.emitter = emitter;
    }

    /**
     * Sends the event to the browser, named after its type so the page can listen for each type
     * (api.js openEventStream). Events for other audits are ignored. send() throws IOException when
     * the tab has closed; it is rethrown unchecked so AgentEventPublisher removes this listener.
     */
    @Override
    public void onEvent(AgentEvent event) {
        if (!auditId.equals(event.auditId())) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name(event.type().name()).data(event, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            throw new UncheckedIOException("The event stream for audit " + auditId + " has closed", e);
        }
    }

    String auditId() {
        return auditId;
    }

    SseEmitter emitter() {
        return emitter;
    }
}
