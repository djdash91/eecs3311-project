package ca.yorku.accessfix.facade;

/**
 * What the user loads (F01): a URL, or the contents of an HTML file. Exactly one is set.
 *
 * @param name for an HTML file, its file name (used as the audit's source)
 */
public record PageInput(String url, String html, String name) {

    public static PageInput ofUrl(String url) {
        return new PageInput(url, null, null);
    }

    public static PageInput ofHtml(String html, String name) {
        return new PageInput(null, html, name);
    }

    public boolean isUrl() {
        return url != null;
    }
}
