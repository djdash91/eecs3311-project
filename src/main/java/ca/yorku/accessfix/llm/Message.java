package ca.yorku.accessfix.llm;

import java.util.List;

/** One turn of the conversation: the user's text, the model's reply, or tool results. */
public record Message(Role role, List<ContentBlock> content) {

    public enum Role { USER, ASSISTANT }

    public Message {
        content = List.copyOf(content);
    }

    public static Message userText(String text) {
        return new Message(Role.USER, List.of(new ContentBlock.Text(text)));
    }

    public static Message user(List<ContentBlock> blocks) {
        return new Message(Role.USER, blocks);
    }

    public static Message assistant(List<ContentBlock> blocks) {
        return new Message(Role.ASSISTANT, blocks);
    }
}
