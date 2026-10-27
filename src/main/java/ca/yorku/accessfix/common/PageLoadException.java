package ca.yorku.accessfix.common;

/** The site could not be reached or timed out (UC01, 2a). The UI should offer Retry. REST maps it to 502. */
public class PageLoadException extends AccessFixException {
    public PageLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
