# Design-to-implementation traceability (Stage 2, section 11)

Update the Status column as you work. Statuses: Not started, Starter only, In progress, Implemented, Modified (explain in DESIGN-CHANGES.md).

"Starter only" means the class exists with its interface and TODOs, but the logic isn't written yet.

| Feature | Designed classes (Stage 1) | Implemented files | Status | Tests |
|---|---|---|---|---|
| F01 Page loading | SnapshotService, AccessFixFacade, PageSnapshot, Audit | `snapshot/SnapshotService.java`, `facade/AccessFixFacade.java`, `domain/PageSnapshot.java`, `domain/Audit.java` | Starter only | |
| F02 Scan | RuleEngine, Rule, ImageAltRule, FormLabelRule (+4 rules), ScanResult, Issue | `scan/RuleEngine.java`, `scan/Rule.java`, `scan/rules/*.java` | ImageAltRule implemented; others starter only | `ImageAltRuleTest` |
| F03 Explanation | IssueAssistant, WcagKnowledgeBase, LLMClient, ClaudeLLMClient | `agent/IssueAssistant.java`, `llm/ClaudeLLMClient.java` | Starter only | |
| F04 Ask mode | AgentController, AgentSession, AskMode, ToolRegistry, Tool, AgentEventPublisher | `agent/AgentController.java`, `agent/mode/AskMode.java`, `agent/tools/*.java`, `agent/events/*.java` | AskMode and AgentEventPublisher implemented; loop starter only | `AgentControllerTest` (disabled) |
| F05 WCAG retrieval | WcagKnowledgeBase, SearchWcagTool | `wcag/WcagKnowledgeBase.java`, `agent/tools/SearchWcagTool.java`, `resources/wcag/wcag-criteria.json` | Loading and tool implemented; search starter only; 7 of 55 entries | |
| F06 Plan mode | PlanMode, SubmitPlanTool, RemediationPlan | `agent/mode/PlanMode.java`, `agent/tools/SubmitPlanTool.java`, `domain/RemediationPlan.java` | Starter only | |
| F07 Alt text | IssueAssistant, ClaudeLLMClient, FixService | `agent/IssueAssistant.java`, `fix/FixService.java` | Starter only | |
| F08 Fix mode | FixMode, ProposeFixTool, ApprovalGate, FixService, FixType | `agent/mode/FixMode.java`, `agent/tools/ProposeFixTool.java`, `agent/ApprovalGate.java`, `domain/FixType.java` | FixType implemented; others starter only | `FixTypeTest` |
| F09 Verification and undo | FixService, FixCommand, AbstractAttributeFix, SetAltTextFix, AddLabelFix | `fix/*.java` | AbstractAttributeFix and SetAltTextFix implemented; others starter only | `SetAltTextFixTest` |
| F10 Report | ReportService | `report/ReportService.java` | Starter only | |
| F11 Memory of rejections | MemoryManager, ApprovalGate, Audit, AuditRepository | `agent/MemoryManager.java`, `persistence/*.java` | InMemoryAuditRepository implemented; others starter only | |
| GUI | AuditView, AgentPanel, ReportView, RestApiController, SseEventStream | `frontend/src/views/*.jsx`, `ui/rest/*.java` | REST mapping implemented; views starter only | |
| CLI | CliApp, ConsoleEventPrinter | `ui/cli/*.java` | load and scan implemented; 9 commands to add | |
