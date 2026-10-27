package ca.yorku.accessfix.agent;

import ca.yorku.accessfix.domain.Explanation;
import ca.yorku.accessfix.domain.Issue;
import ca.yorku.accessfix.domain.PageSnapshot;
import ca.yorku.accessfix.llm.LLMClient;
import ca.yorku.accessfix.wcag.WcagKnowledgeBase;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * One-off model requests: explanations (F03) and alt-text suggestions (F07). Its checkAltText() is
 * also used by ProposeFixTool, so agent-written alt text gets the same checks.
 */
@Service
public class IssueAssistant {

    /** A common guideline for alt text length. Choose your own limit and justify it. */
    public static final int MAX_ALT_TEXT_LENGTH = 150;

    private final LLMClient llm;
    private final WcagKnowledgeBase wcag;

    public IssueAssistant(LLMClient llm, WcagKnowledgeBase wcag) {
        this.llm = llm;
        this.wcag = wcag;
    }

    /**
     * TODO(F03, SD02): get the criterion from wcag.get(issue.criterion()), ask for an Explanation with
     * llm.sendStructured(), and check its criterion matches the issue. On LLMException or a mismatch,
     * return Explanation.fromRuleMessage(issue).
     */
    public Explanation explain(Issue issue) {
        throw new UnsupportedOperationException("TODO: IssueAssistant.explain");
    }

    /**
     * TODO(F07, SD02): send the image's screenshot (snapshot.screenshotOf(issue.elementId())) and the
     * text around the image. Return empty when there is no screenshot, the API fails, or the
     * suggestion fails checkAltText(): the user then writes the alt text.
     */
    public Optional<String> suggestAltText(Issue issue, PageSnapshot snapshot) {
        throw new UnsupportedOperationException("TODO: IssueAssistant.suggestAltText");
    }

    /**
     * TODO(F07): return a list of problems; empty means the text passes. F07 names three checks:
     * length (MAX_ALT_TEXT_LENGTH), filenames (for example "IMG_2041.jpg"), and phrases like
     * "image of" or "picture of".
     *
     * <p>F08 allows empty alt text for decorative images in Fix mode, but F07 suggestions shouldn't be
     * empty. Decide where that difference lives, and test both cases.
     *
     * <p>This method is deterministic: an ideal first unit-test target.
     */
    public List<String> checkAltText(String text) {
        throw new UnsupportedOperationException("TODO: IssueAssistant.checkAltText");
    }
}
