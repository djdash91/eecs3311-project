package ca.yorku.accessfix.common;

/** The user's input is invalid, for example a malformed URL (UC01, 1a). REST maps it to 400. */
public class InvalidInputException extends AccessFixException {
    public InvalidInputException(String message) {
        super(message);
    }
}
