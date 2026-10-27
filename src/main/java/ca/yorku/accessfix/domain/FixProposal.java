package ca.yorku.accessfix.domain;

/**
 * A proposed fix (F08).
 *
 * @param issueId the issue to fix
 * @param fixType the kind of fix
 * @param value   the one value every fix type needs: the alt text, label text, aria-label,
 *                language code or page title
 */
public record FixProposal(String issueId, FixType fixType, String value) {
}
