package ca.yorku.accessfix.domain;

/**
 * A structured explanation of an issue (F03). Also the shape you ask Claude to return with structured outputs.
 */
public record Explanation(String whatIsWrong, String whoIsAffected, String criterion, String howToFix) {

    /** The fallback when the API fails or the result is invalid (UC03, 2a and 3a). */
    public static Explanation fromRuleMessage(Issue issue) {
        return new Explanation(issue.message(), null, issue.criterion(), null);
    }
}
