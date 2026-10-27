package ca.yorku.accessfix.common;

/** Base class for errors the user should see as a message rather than a stack trace. */
public class AccessFixException extends RuntimeException {
    public AccessFixException(String message) {
        super(message);
    }

    public AccessFixException(String message, Throwable cause) {
        super(message, cause);
    }
}
