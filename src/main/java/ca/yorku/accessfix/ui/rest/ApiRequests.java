package ca.yorku.accessfix.ui.rest;

/** JSON request bodies for RestApiController. */
public final class ApiRequests {

    private ApiRequests() {
    }

    /** Either url, or html with an optional file name. */
    public record LoadPage(String url, String html, String fileName) {
    }

    public record Text(String text) {
    }

    public record DecisionBody(boolean approved, String reason) {
    }
}
