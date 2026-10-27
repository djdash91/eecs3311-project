package ca.yorku.accessfix.ui.rest;

import ca.yorku.accessfix.agent.events.AgentEvent;
import ca.yorku.accessfix.agent.events.AgentEventListener;
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
     * TODO(F04, F08): ignore events for other audits; otherwise
     * {@code emitter.send(SseEmitter.event().name(event.type().name()).data(event))}.
     * send() throws IOException when the tab has closed: rethrow it as an unchecked exception so
     * AgentEventPublisher removes this listener.
     */
    @Override
    public void onEvent(AgentEvent event) {
        throw new UnsupportedOperationException("TODO: SseEventStream.onEvent");
    }

    String auditId() {
        return auditId;
    }

    SseEmitter emitter() {
        return emitter;
    }
}
