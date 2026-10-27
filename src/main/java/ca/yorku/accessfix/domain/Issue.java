package ca.yorku.accessfix.domain;

/**
 * One problem found by a rule (view C).
 *
 * @param id        stable ID: rule ID plus element ID, so the same problem keeps its ID in every scan
 * @param ruleId    one of {@link RuleIds}
 * @param criterion the WCAG success criterion number, for example "1.1.1"
 * @param elementId the element's stable ID attribute (see {@link PageSnapshot#ID_ATTR})
 * @param snippet   the element's code, without the stable ID attributes, for display
 * @param message   short rule message, also the fallback when an explanation fails (F03)
 */
public record Issue(String id, String ruleId, String criterion, String elementId, String snippet, String message) {

    public static String idFor(String ruleId, String elementId) {
        return ruleId + ":" + elementId;
    }

    public static Issue of(String ruleId, String criterion, String elementId, String snippet, String message) {
        return new Issue(idFor(ruleId, elementId), ruleId, criterion, elementId, snippet, message);
    }
}
