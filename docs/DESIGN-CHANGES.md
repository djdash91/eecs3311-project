# Design changes (Stage 2, section 11)

Format required by the project: original design, change, reason.

## Decisions made by the starter code

These came with the starter code, not from you. For each one, decide whether to keep it, change it or revert it, and edit the reason into your own words. The marker will expect you to explain them.

| Original design (Stage 1) | Change | Reason |
|---|---|---|
| `Tool` has only `execute(input, session)` | Added `name()`, `description()` and `inputSchema()` | Claude needs a name, a description and a JSON schema for every tool. |
| `FixCommand` has `execute` and `undo` | Added `issueId()` and `description()` | Undo, the report and the Audit's history need to know which issue each fix addressed. |
| `Rule` has `id()` and `check()` | Added `criterion()` | Each issue records its WCAG criterion, and the rule is the one place that knows it. |
| `AgentSession` holds messages, mode, running flag and pending proposal | Added `pendingToolUseId` and held tool results | SD05 says the decision becomes the result of the waiting tool call, and other results from the same turn are sent with it. The session must remember which call that was. |
| `FixProposal` holds "its values" | One `value` string | Every fix type takes exactly one value (alt text, label, aria-label, language or title). |
| `AuditRepository.findById` returns `Audit` | Returns `Optional<Audit>` | Makes "not found" explicit; the facade turns it into NotFoundException. |
| `IssueAssistant.suggestAltText` returns `String` | Returns `Optional<String>` | "No suggestion" is a normal outcome in F07 (no screenshot, API failure, failed checks). |
| One `AuditRepository` backed by H2 | Two implementations chosen by `accessfix.storage` | Unit tests and early development need storage before the H2 version exists. |
| `submitDecision(auditId, decision)` | `submitDecision(auditId, approved, reason)` | The proposal lives in the session, so the facade builds the Decision. |
| Rule IDs not specified | Constants in `RuleIds` | Issue IDs, FixType and prompts all refer to them. |
| `ToolResult` holds output or an error | Status of OK, ERROR, PENDING or ACCEPTED, plus an optional payload | Fix mode needs "pending" (SD05) and Plan mode needs "accepted" (SD04). |

## Your changes during implementation

| Original design | Change | Reason |
|---|---|---|
| | | |
