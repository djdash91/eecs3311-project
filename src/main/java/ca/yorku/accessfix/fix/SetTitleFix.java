package ca.yorku.accessfix.fix;

import ca.yorku.accessfix.domain.PageSnapshot;

/**
 * Sets the page title (FixType.SET_TITLE). The title is element text, not an attribute, and the
 * title element may not exist yet, so this implements FixCommand directly.
 *
 * <p>TODO(F08): implement. Remember whether you created the title element or changed an existing
 * one, so undo can reverse the right thing.
 */
public class SetTitleFix implements FixCommand {

    private final String issueId;
    private final String title;

    public SetTitleFix(String issueId, String title) {
        this.issueId = issueId;
        this.title = title;
    }

    @Override
    public String issueId() {
        return issueId;
    }

    @Override
    public String description() {
        return "Set page title to \"" + title + "\"";
    }

    @Override
    public void execute(PageSnapshot snapshot) {
        throw new UnsupportedOperationException("TODO: SetTitleFix.execute");
    }

    @Override
    public void undo(PageSnapshot snapshot) {
        throw new UnsupportedOperationException("TODO: SetTitleFix.undo");
    }
}
