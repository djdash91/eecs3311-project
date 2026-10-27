package ca.yorku.accessfix.llm;

import java.util.Map;

/**
 * One piece of a message. These are AccessFix's own types: only ClaudeLLMClient converts them to and
 * from the Anthropic SDK's types (Adapter pattern), so the rest of the code never imports com.anthropic.
 */
public sealed interface ContentBlock
        permits ContentBlock.Text, ContentBlock.ToolUse, ContentBlock.ToolResultBlock, ContentBlock.Image {

    /** Plain text from the user or the model. */
    record Text(String text) implements ContentBlock {
    }

    /** The model asks to run a tool. {@code id} must be echoed back in the matching ToolResultBlock. */
    record ToolUse(String id, String name, Map<String, Object> input) implements ContentBlock {
    }

    /** The result of a tool call, sent back to the model in a user message. */
    record ToolResultBlock(String toolUseId, String content, boolean isError) implements ContentBlock {
    }

    /** An image, for example an image screenshot for alt text (F07). mediaType like "image/png". */
    record Image(String mediaType, byte[] data) implements ContentBlock {
    }
}
