package ca.yorku.accessfix.scan.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.yorku.accessfix.TestSnapshots;
import ca.yorku.accessfix.domain.Issue;
import ca.yorku.accessfix.domain.PageSnapshot;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WORKED EXAMPLE of deterministic tests (Stage 3, Part A): normal, boundary and edge cases for one
 * rule. Copy this shape for the other five rules.
 */
class ImageAltRuleTest {

    private final ImageAltRule rule = new ImageAltRule();

    @Test
    void flagsImageWithoutAlt() {
        PageSnapshot snapshot = TestSnapshots.fromHtml("<img src='cat.png'>");

        List<Issue> issues = rule.check(snapshot);

        assertEquals(1, issues.size());
        Issue issue = issues.get(0);
        assertEquals("image-alt", issue.ruleId());
        assertEquals("1.1.1", issue.criterion());
        assertEquals(TestSnapshots.idOf(snapshot, "img"), issue.elementId());
    }

    @Test
    void acceptsEmptyAltAsDecorative() {
        assertTrue(rule.check(TestSnapshots.fromHtml("<img src='line.png' alt=''>")).isEmpty());
    }

    @Test
    void acceptsDescriptiveAlt() {
        assertTrue(rule.check(TestSnapshots.fromHtml("<img src='cat.png' alt='A sleeping cat'>")).isEmpty());
    }

    @Test
    void acceptsAriaLabelAsName() {
        assertTrue(rule.check(TestSnapshots.fromHtml("<img src='cat.png' aria-label='A sleeping cat'>")).isEmpty());
    }

    @Test
    void skipsHiddenImages() {
        PageSnapshot snapshot = TestSnapshots.withHidden("<img id='h' src='a.png'><img src='b.png'>", "#h");

        List<Issue> issues = rule.check(snapshot);

        assertEquals(1, issues.size(), "only the visible image is reported");
    }

    @Test
    void skipsImagesRemovedFromAccessibilityTree() {
        String html = "<img src='a.png' aria-hidden='true'><img src='b.png' role='presentation'>";
        assertTrue(rule.check(TestSnapshots.fromHtml(html)).isEmpty());
    }

    @Test
    void issueIdIsStableAcrossScans() {
        PageSnapshot snapshot = TestSnapshots.fromHtml("<p>Hi</p><img src='cat.png'>");

        String first = rule.check(snapshot).get(0).id();
        String second = rule.check(snapshot).get(0).id();

        assertEquals(first, second);
        assertEquals("image-alt:" + TestSnapshots.idOf(snapshot, "img"), first);
    }

    @Test
    void snippetHidesInternalIds() {
        Issue issue = rule.check(TestSnapshots.fromHtml("<img src='cat.png'>")).get(0);
        assertFalse(issue.snippet().contains(PageSnapshot.ID_ATTR));
    }
}
