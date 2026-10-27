package ca.yorku.accessfix.llm;

import ca.yorku.accessfix.common.AccessFixException;

/** The Claude API failed or returned something unusable. Callers show an error with Retry. */
public class LLMException extends AccessFixException {
    public LLMException(String message, Throwable cause) {
        super(message, cause);
    }

    public LLMException(String message) {
        super(message);
    }
}
