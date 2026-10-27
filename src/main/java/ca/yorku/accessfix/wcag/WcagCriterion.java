package ca.yorku.accessfix.wcag;

/**
 * One knowledge base entry (F05).
 *
 * @param number     for example "1.1.1"
 * @param level      "A" or "AA"
 * @param whoItHelps who the criterion helps, used in explanations (F03)
 * @param url        the W3C "Understanding" page
 */
public record WcagCriterion(String number, String title, String level, String summary, String whoItHelps, String url) {
}
