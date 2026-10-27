package ca.yorku.accessfix.llm;

/**
 * The agent's only way to reach a model (Adapter pattern target). Tests replace it with
 * FakeLLMClient, so the agent loop can be tested without network calls or cost.
 */
public interface LLMClient {

    /** One call, possibly with tools. Throws LLMException if the API fails. */
    LLMResponse send(LLMRequest request);

    /**
     * One call whose answer must match {@code type} (structured outputs), for example Explanation.
     * Throws LLMException if the API fails or the answer doesn't parse.
     */
    <T> T sendStructured(LLMRequest request, Class<T> type);
}
