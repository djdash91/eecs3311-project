package ca.yorku.accessfix.ui.rest;

import ca.yorku.accessfix.agent.AgentResult;
import ca.yorku.accessfix.agent.events.AgentEventPublisher;
import ca.yorku.accessfix.common.InvalidInputException;
import ca.yorku.accessfix.domain.Audit;
import ca.yorku.accessfix.domain.Explanation;
import ca.yorku.accessfix.domain.FixProposal;
import ca.yorku.accessfix.domain.RemediationPlan;
import ca.yorku.accessfix.domain.ScanResult;
import ca.yorku.accessfix.domain.VerificationResult;
import ca.yorku.accessfix.facade.AccessFixFacade;
import ca.yorku.accessfix.facade.PageInput;
import ca.yorku.accessfix.wcag.WcagCriterion;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * The GUI's HTTP entry point (view A). Every method only translates HTTP to a facade call: if you
 * find yourself writing logic here, it belongs in the facade or a service, where the CLI can use it too.
 *
 * <p>Try endpoints without the React app using requests.http (REST Client extension).
 */
@RestController
@RequestMapping("/api")
public class RestApiController {

    private final AccessFixFacade facade;
    private final AgentEventPublisher publisher;

    public RestApiController(AccessFixFacade facade, AgentEventPublisher publisher) {
        this.facade = facade;
        this.publisher = publisher;
    }

    /**
     * Returning the Audit itself is fine to get started, but it sends the whole snapshot (HTML and
     * screenshots) to the browser, and Jackson can't serialise FixCommand objects once fixes exist.
     * TODO: return a small record (for example AuditSummary: id, source, latest scan) instead.
     */
    @PostMapping("/audits")
    public Audit loadPage(@RequestBody ApiRequests.LoadPage body) {
        if (body.url() != null && !body.url().isBlank()) {
            return facade.loadPage(PageInput.ofUrl(body.url().trim()));
        }
        if (body.html() != null && !body.html().isBlank()) {
            return facade.loadPage(PageInput.ofHtml(body.html(), body.fileName()));
        }
        throw new InvalidInputException("Enter a URL or choose an HTML file.");
    }

    @PostMapping("/audits/{auditId}/scan")
    public ScanResult scan(@PathVariable String auditId) {
        return facade.scan(auditId);
    }

    @PostMapping("/audits/{auditId}/issues/{issueId}/explain")
    public Explanation explain(@PathVariable String auditId, @PathVariable String issueId) {
        return facade.explainIssue(auditId, issueId);
    }

    /** Returns {"suggestion": "..."} or {"suggestion": null} when no suggestion could be made (F07). */
    @PostMapping("/audits/{auditId}/issues/{issueId}/alt-text")
    public Map<String, String> suggestAltText(@PathVariable String auditId, @PathVariable String issueId) {
        String suggestion = facade.suggestAltText(auditId, issueId).orElse(null);
        return java.util.Collections.singletonMap("suggestion", suggestion);
    }

    @PostMapping("/audits/{auditId}/fixes")
    public VerificationResult applyFix(@PathVariable String auditId, @RequestBody FixProposal proposal) {
        return facade.applyFix(auditId, proposal);
    }

    @PostMapping("/audits/{auditId}/fixes/undo")
    public ScanResult undoLastFix(@PathVariable String auditId) {
        return facade.undoLastFix(auditId);
    }

    @GetMapping("/wcag")
    public List<WcagCriterion> searchWcag(@RequestParam("q") String query) {
        return facade.searchWcag(query);
    }

    @PostMapping("/audits/{auditId}/ask")
    public AgentResult sendMessage(@PathVariable String auditId, @RequestBody ApiRequests.Text body) {
        return facade.sendMessage(auditId, body.text());
    }

    @PostMapping("/audits/{auditId}/plan")
    public RemediationPlan createPlan(@PathVariable String auditId, @RequestBody(required = false) ApiRequests.Text body) {
        return facade.createPlan(auditId, body == null ? null : body.text());
    }

    @PostMapping("/audits/{auditId}/plan/approve")
    public void approvePlan(@PathVariable String auditId) {
        facade.approvePlan(auditId);
    }

    @PostMapping("/audits/{auditId}/fix-session")
    public AgentResult startFix(@PathVariable String auditId) {
        return facade.startFixSession(auditId);
    }

    @PostMapping("/audits/{auditId}/fix-session/decision")
    public AgentResult submitDecision(@PathVariable String auditId, @RequestBody ApiRequests.DecisionBody body) {
        return facade.submitDecision(auditId, body.approved(), body.reason());
    }

    @GetMapping(value = "/audits/{auditId}/report", produces = "text/markdown")
    public String exportReport(@PathVariable String auditId) {
        return facade.exportReport(auditId);
    }

    @GetMapping(value = "/audits/{auditId}/fixed-page", produces = MediaType.TEXT_HTML_VALUE)
    public String exportPatchedPage(@PathVariable String auditId) {
        return facade.exportPatchedPage(auditId);
    }

    /** One SSE connection per open agent panel (SD05). The stream forwards only this audit's events. */
    @GetMapping(value = "/audits/{auditId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@PathVariable String auditId) {
        SseEmitter emitter = new SseEmitter(0L); // 0 = no timeout
        SseEventStream stream = new SseEventStream(auditId, emitter);
        publisher.subscribe(stream);
        emitter.onCompletion(() -> publisher.unsubscribe(stream));
        emitter.onTimeout(() -> publisher.unsubscribe(stream));
        emitter.onError(error -> publisher.unsubscribe(stream));
        return emitter;
    }
}
