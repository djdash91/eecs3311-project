package ca.yorku.accessfix.snapshot;

import ca.yorku.accessfix.common.PageLoadException;
import ca.yorku.accessfix.domain.PageSnapshot;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Loads a page in headless Chromium and builds a PageSnapshot (F01, SD01): the final HTML with a
 * stable ID on every element, the IDs of hidden elements, and a screenshot of each rendered image
 * that has no alt attribute.
 *
 * <p>Each call launches its own Chromium and closes it. CLI commands each run in their own process,
 * so a shared browser would save nothing there, and pages are loaded rarely in the GUI.
 */
@Service
public class SnapshotService {

    private static final Logger LOG = LoggerFactory.getLogger(SnapshotService.class);

    /**
     * Runs in the page. Gives every element a stable ID ("e0", "e1"... in document order), and
     * returns the IDs of hidden elements and of images to screenshot. An image is screenshot only if
     * it has no alt attribute, is visible, has a size, and loaded: a broken image has no screenshot (F07).
     */
    private static final String MARK_ELEMENTS = """
            () => {
              const hidden = [];
              const images = [];
              document.querySelectorAll('*').forEach((el, i) => {
                const id = 'e' + i;
                el.setAttribute('%s', id);
                if (!el.checkVisibility({ visibilityProperty: true })) {
                  hidden.push(id);
                } else if (el.localName === 'img' && !el.hasAttribute('alt') && el.complete
                    && el.naturalWidth > 0 && el.getBoundingClientRect().width > 0
                    && el.getBoundingClientRect().height > 0) {
                  images.push(id);
                }
              });
              return { hidden, images };
            }
            """.formatted(PageSnapshot.ID_ATTR);

    private final int timeoutMs;

    public SnapshotService(@Value("${accessfix.snapshot.timeout-ms:30000}") int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    /**
     * Loads the page at the URL. The facade validates the URL first (SD01). Throws
     * PageLoadException if the site can't be reached, times out, or returns an HTTP error.
     */
    public PageSnapshot captureUrl(String url) {
        return capture(url, page -> {
            Response response = page.navigate(url, new Page.NavigateOptions().setTimeout(timeoutMs));
            if (response != null && response.status() >= 400) {
                throw new PageLoadException(url + " returned HTTP " + response.status() + ".", null);
            }
        });
    }

    /** Loads an uploaded HTML file. Relative links can't be resolved, so their images count as broken. */
    public PageSnapshot captureHtml(String html) {
        return capture("the HTML file", page -> page.setContent(html, new Page.SetContentOptions().setTimeout(timeoutMs)));
    }

    private PageSnapshot capture(String source, Consumer<Page> load) {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            load.accept(page);
            Map<?, ?> marked = (Map<?, ?>) page.evaluate(MARK_ELEMENTS);
            Map<String, byte[]> screenshots = new HashMap<>();
            for (String id : ids(marked, "images")) {
                screenshot(page, id).ifPresent(png -> screenshots.put(id, png));
            }
            return new PageSnapshot(page.content(), Set.copyOf(ids(marked, "hidden")), screenshots);
        } catch (PlaywrightException e) {
            throw new PageLoadException("Couldn't load " + source + ": " + e.getMessage(), e);
        }
    }

    /** A PNG of the element, or empty if it can't be taken. A missing screenshot only means no alt-text suggestion (F07). */
    private Optional<byte[]> screenshot(Page page, String id) {
        try {
            Locator image = page.locator("[" + PageSnapshot.ID_ATTR + "='" + id + "']");
            return Optional.of(image.screenshot(new Locator.ScreenshotOptions().setTimeout(timeoutMs)));
        } catch (PlaywrightException e) {
            LOG.warn("No screenshot for image {}: {}", id, e.getMessage());
            return Optional.empty();
        }
    }

    private static List<String> ids(Map<?, ?> marked, String key) {
        return ((List<?>) marked.get(key)).stream().map(String::valueOf).toList();
    }
}
