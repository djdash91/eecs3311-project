package ca.yorku.accessfix.llm;

import java.util.Map;

/**
 * One piece of a message. These are AccessFix's own types: only the adapters, ClaudeLLMClient and
 * GeminiLLMClient, convert them to and from an SDK's types (Adapter pattern), so the rest of the code
 * never imports com.anthropic or com.google.genai.
 */
public sealed interface ContentBlock
        permits ContentBlock.Text, ContentBlock.ToolUse, ContentBlock.ToolResultBlock, ContentBlock.Image,
        ContentBlock.Thinking, ContentBlock.RedactedThinking {

    /** Plain text from the user or the model. */
    record Text(String text) implements ContentBlock {
    }

    /** The model asks to run a tool. {@code id} must be echoed back in the matching ToolResultBlock. */
    record ToolUse(String id, String name, Map<String, Object> input) implements ContentBlock {
    }

    /**
     * The result of a tool call, sent back to the model in a user message.
     *
     * @param image an image that goes with the text, or null; inspect_element uses it to show the
     *              model an image's screenshot (Stage 1, section 1.1)
     */
    record ToolResultBlock(String toolUseId, String content, boolean isError, Image image) implements ContentBlock {

        /** A result with text only, which is what most tools return. */
        public ToolResultBlock(String toolUseId, String content, boolean isError) {
            this(toolUseId, content, isError, null);
        }
    }

    /** An image, for example an image screenshot for alt text (F07). mediaType like "image/png". */
    record Image(String mediaType, byte[] data) implements ContentBlock {
    }

    /**
     * The model's thinking, from a reply. Keep it in the assistant message exactly as received: the API
     * checks {@code signature}, and changing or dropping a thinking block from an earlier turn makes
     * the next request fail. The text is often empty, because the API leaves it out by default. For
     * Gemini, the signature is the thought signature of the block that follows.
     */
    record Thinking(String thinking, String signature) implements ContentBlock {
    }

    /** Thinking the API returns encrypted. Kept and sent back unchanged, like Thinking. */
    record RedactedThinking(String data) implements ContentBlock {
    }
}
