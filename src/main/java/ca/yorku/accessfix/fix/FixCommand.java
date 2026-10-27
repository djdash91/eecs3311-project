package ca.yorku.accessfix.fix;

import ca.yorku.accessfix.domain.PageSnapshot;

/**
 * A fix that can apply and undo itself (Command pattern, view D). FixService is the invoker,
 * PageSnapshot the receiver, and Audit keeps the history of applied fixes.
 *
 * <p>Rule for implementations: {@code execute} then {@code undo} must leave the HTML exactly as it
 * was. Test that round trip for every fix type.
 */
public interface FixCommand {

    /** The issue this fix addresses, so the report and undo can refer to it. */
    String issueId();

    /** A short human-readable summary for the report, for example "Set alt text on e12". */
    String description();

    void execute(PageSnapshot snapshot);

    void undo(PageSnapshot snapshot);
}
