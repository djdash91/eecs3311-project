package ca.yorku.accessfix.snapshot;

import ca.yorku.accessfix.domain.PageSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Loads a page in headless Chromium and builds a PageSnapshot (F01, SD01).
 *
 * <p>TODO(F01): implement both methods with Playwright. Useful Playwright Java calls:
 * <ul>
 *   <li>{@code Playwright.create()}, then {@code playwright.chromium().launch()} and {@code browser.newPage()}.
 *       Playwright and Browser are AutoCloseable, so try-with-resources keeps Chromium from leaking.</li>
 *   <li>{@code page.navigate(url, new Page.NavigateOptions().setTimeout(timeoutMs))} for a URL,
 *       or {@code page.setContent(html)} for an uploaded file.</li>
 *   <li>{@code page.evaluate("...javascript...")} to run code in the page. One script can give every
 *       element a stable {@code data-af-id} (for example "e0", "e1" in document order) and return
 *       the IDs of elements that aren't visible ({@code el.checkVisibility()} is false).</li>
 *   <li>{@code page.content()} to read the final HTML after the IDs are added.</li>
 *   <li>{@code page.locator("[data-af-id='e12']").screenshot()} for each img without alt text.
 *       A broken image has no screenshot; F07 says the user then writes the alt text.</li>
 *   <li>Catch {@code PlaywrightException} (TimeoutError extends it) and throw PageLoadException.</li>
 * </ul>
 * The first run downloads Chromium (around 150 MB), so it is slow once.
 *
 * <p>Design question for your report: launching Chromium per call is simple but slow. Is one shared
 * browser worth the extra lifecycle code (closing it with {@code @PreDestroy})?
 */
@Service
public class SnapshotService {

    private final int timeoutMs;

    public SnapshotService(@Value("${accessfix.snapshot.timeout-ms:30000}") int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public PageSnapshot captureUrl(String url) {
        throw new UnsupportedOperationException("TODO: SnapshotService.captureUrl");
    }

    public PageSnapshot captureHtml(String html) {
        throw new UnsupportedOperationException("TODO: SnapshotService.captureHtml");
    }
}
