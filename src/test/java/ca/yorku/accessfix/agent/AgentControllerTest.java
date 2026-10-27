package ca.yorku.accessfix.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.yorku.accessfix.agent.events.AgentEventPublisher;
import ca.yorku.accessfix.agent.mode.AskMode;
import ca.yorku.accessfix.agent.tools.SearchWcagTool;
import ca.yorku.accessfix.agent.tools.ToolRegistry;
import ca.yorku.accessfix.llm.ContentBlock;
import ca.yorku.accessfix.llm.FakeLLMClient;
import ca.yorku.accessfix.llm.LLMRequest;
import ca.yorku.accessfix.llm.Message;
import ca.yorku.accessfix.persistence.InMemoryAuditRepository;
import ca.yorku.accessfix.wcag.WcagCriterion;
import ca.yorku.accessfix.wcag.WcagKnowledgeBase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * A test written BEFORE the code: it describes what AgentController.run must do. Remove @Disabled when
 * you start on run(), then add tests for the step limit, a refused tool, and an API failure.
 *
 * <p>Note: the agent's real behaviour with Claude is tested with KUMA in Stage 3. This test only checks
 * the deterministic loop around the model, using a scripted fake model.
 */
class AgentControllerTest {

    @Test
    @Disabled("Enable once AgentController.run and ToolRegistry.execute are implemented")
    void runsToolThenReturnsTheAnswer() {
        // Stub search, so this test doesn't depend on your WcagKnowledgeBase.search
        WcagCriterion nonText = new WcagCriterion("1.1.1", "Non-text Content", "A", "Text alternatives.", "Blind users", "https://www.w3.org/");
        WcagKnowledgeBase kb = new WcagKnowledgeBase(List.of(nonText)) {
            @Override
            public List<WcagCriterion> search(String query) {
                return List.of(nonText);
            }
        };
        FakeLLMClient llm = new FakeLLMClient()
                .reply(FakeLLMClient.toolCall("call-1", SearchWcagTool.NAME, Map.of("query", "alt text")))
                .reply(FakeLLMClient.text("Images need alt text under 1.1.1."));
        InMemoryAuditRepository repository = new InMemoryAuditRepository();
        MemoryManager memory = new MemoryManager(repository);
        ToolRegistry tools = new ToolRegistry(List.of(new SearchWcagTool(kb)));
        AgentController controller = new AgentController(llm, tools, memory, new AgentEventPublisher());

        // You may need an Audit in the repository, depending on how loadDecisions handles a missing one.
        AgentSession session = memory.sessionFor("audit-1");
        session.setMode(new AskMode());

        AgentResult result = controller.run(session, "What does WCAG say about alt text?");

        assertEquals("Images need alt text under 1.1.1.", result.answer());
        assertEquals(2, llm.requests().size(), "one call with the tool, one after its result");

        LLMRequest second = llm.requests().get(1);
        Message lastMessage = second.messages().get(second.messages().size() - 1);
        assertTrue(lastMessage.content().get(0) instanceof ContentBlock.ToolResultBlock result1
                && result1.toolUseId().equals("call-1"), "tool result must answer call-1");
        assertTrue(!session.isBusy(), "run must clear the running flag");
    }
}
