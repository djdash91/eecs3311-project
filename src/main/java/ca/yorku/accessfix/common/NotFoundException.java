package ca.yorku.accessfix.common;

/** An audit, issue or similar was not found. REST maps it to 404. */
public class NotFoundException extends AccessFixException {
    public NotFoundException(String message) {
        super(message);
    }
}
