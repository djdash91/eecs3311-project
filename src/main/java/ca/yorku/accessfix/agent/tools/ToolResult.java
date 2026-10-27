package ca.yorku.accessfix.agent.tools;

/**
 * A tool's output, sent back to the model (view B).
 *
 * @param status  OK and ERROR go back to the model. PENDING means a fix waits for the user and nothing
 *                is sent yet (SD05). ACCEPTED means submit_plan accepted a valid plan (SD04).
 * @param content text for the model
 * @param payload optional object for our own code, for example the accepted RemediationPlan
 */
public record ToolResult(Status status, String content, Object payload) {

    public enum Status { OK, ERROR, PENDING, ACCEPTED }

    public static ToolResult ok(String content) {
        return new ToolResult(Status.OK, content, null);
    }

    public static ToolResult error(String message) {
        return new ToolResult(Status.ERROR, message, null);
    }

    public static ToolResult pending() {
        return new ToolResult(Status.PENDING, "", null);
    }

    public static ToolResult accepted(String content, Object payload) {
        return new ToolResult(Status.ACCEPTED, content, payload);
    }

    public boolean isError() {
        return status == Status.ERROR;
    }
}
