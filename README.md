<<<<<<< HEAD
# AccessFix starter code (EECS3311, Stage 2)

AccessFix is an AI agent that audits and repairs web accessibility issues. This is starter code built from the Stage 1 design report. It gives you the structure, the interfaces and one worked example of each design pattern. The logic is yours to write, with AI help.

## What the starter gives you, and what it leaves to you

Complete, as worked examples to copy:

- `ImageAltRule` and its tests: how to write and test a rule (Strategy pattern).
- `AbstractAttributeFix`, `SetAltTextFix` and their tests: how a fix applies and undoes itself (Command pattern).
- `AskMode`: what a mode supplies (State pattern).
- `SearchWcagTool`: how a tool describes itself and validates input.
- `AgentEventPublisher`: the Observer pattern subject.
- `FixType`: the one place that says which fix types can fix which rules (F08).
- `LoadCommand` and `ScanCommand`: two CLI commands.
- `RestApiController`: every endpoint, mapped to a facade method.
- `AuditView.jsx`: an accessible form.
- `FakeLLMClient`: a scripted model for testing the agent loop without API calls.

Left for you, with step-by-step TODO comments that name the feature and sequence diagram:

- the five other rules and `RuleEngine.scan`;
- `SnapshotService` (Playwright);
- `FixService` and the other fix commands;
- `ClaudeLLMClient` (the SDK conversion);
- `AgentController.run` (the agent loop);
- `ToolRegistry.execute` and the other tools;
- `PlanMode`, `FixMode` and `ApprovalGate`;
- `MemoryManager`, `IssueAssistant`, `ReportService` and `JdbcAuditRepository`;
- the facade methods;
- the other CLI commands;
- most of the React app;
- 48 more WCAG entries in `src/main/resources/wcag/wcag-criteria.json`.

Unfinished methods throw `UnsupportedOperationException("TODO: ...")`. The REST API returns these as HTTP 501, so you can see what's missing.

## Set up a fresh computer

### Install the tools

You don't need to install Maven: the project includes the Maven wrapper (`mvnw`), which downloads the right version the first time.

On Windows, in PowerShell:

```powershell
winget install Git.Git
winget install EclipseAdoptium.Temurin.21.JDK
winget install OpenJS.NodeJS.LTS
```

On macOS, with Homebrew:

```bash
brew install git node
brew install --cask temurin@21
```

Close and reopen your terminal, then check that each command prints a version: `java -version` (it should say 21), `node -v` and `git --version`.

### Set up VS Code

1. Open this folder in VS Code (**File**, then **Open Folder**).
2. Accept the prompt to install the recommended extensions. The list is in `.vscode/extensions.json`: Java, Spring Boot, Todo Tree, REST Client, Mermaid preview, Claude Code and Python. Use another AI coding tool instead of Claude Code if you prefer; the project requires that you use one and log it.
3. Wait for the Java extension to finish importing the project. The status bar shows progress; the first import downloads dependencies and takes a few minutes.

### Add your Anthropic API key

Copy `.env.example` to `.env` and paste your key. VS Code's Run and Debug configurations and the Java test runner read `.env`, and Git ignores it. Never commit a key.

To use the key from a terminal too, set it as an environment variable. On Windows, run `setx ANTHROPIC_API_KEY "your-key"` and reopen VS Code. On macOS, add `export ANTHROPIC_API_KEY=your-key` to `~/.zshrc`.

You can do all the deterministic work (rules, fixes, scanning, reports) without a key. The Claude client connects only the first time it's used.

### Check that everything works

1. Run the tests: on Windows `.\mvnw.cmd test`, on macOS `./mvnw test`. Or use **Terminal**, then **Run Task**, then **Backend: run all tests**. Expect 16 passing tests and 2 skipped.
2. Check your API key with one tiny call: set `ACCESSFIX_LIVE_TESTS=true` in `.env`, then run `ClaudeConnectionLiveTest` from the Testing panel. Set it back to `false` afterwards.
3. Start the web server: **Run and Debug**, then **AccessFix: web server**. It listens on port 8080.
4. Start the React app: run the tasks **Frontend: install packages** and **Frontend: start dev server**, then open http://localhost:5173. Loading a page shows a "TODO" error until you implement `loadPage`, which is expected.

The first time `SnapshotService` launches Playwright, it downloads Chromium (around 150 MB).

## Run the application

| What | How |
|---|---|
| Web server (backend for the GUI) | Run and Debug: **AccessFix: web server**, or `./mvnw spring-boot:run` |
| React app | `cd frontend`, `npm install` once, then `npm run dev` |
| A CLI command | Run and Debug: **AccessFix: CLI, ask for arguments**, then type for example `cli load samples/broken-page.html` |
| CLI from a terminal | `./mvnw spring-boot:run -Dspring-boot.run.arguments="cli scan <auditId>"` |
| REST calls without the GUI | Open `requests.http` and select **Send Request** |

CLI commands share audits only once storage is set to H2 (`accessfix.storage=h2` in `application.properties`), because each command is a separate process.

## How the code maps to your design

| Package | Stage 1 view | Contents |
|---|---|---|
| `ui.rest`, `ui.cli`, `facade` | View A | `RestApiController`, `SseEventStream`, `CliApp`, `AccessFixFacade` |
| `agent`, `agent.mode`, `agent.tools`, `agent.events`, `llm` | View B | `AgentController`, modes, tools, `ApprovalGate`, `MemoryManager`, `IssueAssistant`, `LLMClient` |
| `domain`, `scan`, `snapshot`, `report`, `persistence` | View C | `Audit`, `PageSnapshot`, `RuleEngine`, rules, `ReportService`, `AuditRepository` |
| `fix`, `wcag` | View D | `FixService`, fix commands, `WcagKnowledgeBase` |

`docs/TRACEABILITY.md` maps every feature to its files. `docs/DESIGN-CHANGES.md` lists small changes the starter made to your design; review each one, because you'll need to justify them.

## Suggested build order

Deterministic parts first, so you have a tested base before any AI behaviour is involved.

1. **Rules (F02).** Write the five rules and `RuleEngine.scan`, with tests modelled on `ImageAltRuleTest`. Use `samples/broken-page.html`; its README lists the expected issues.
2. **Fixes (F09).** The other fix commands, `FixService.createCommand` and `applyVerified`, with round-trip tests.
3. **Loading (F01).** `SnapshotService` with Playwright, then `loadPage`, `scan` and the CLI `load` and `scan` commands. This is the first time you can run the whole path.
4. **WCAG search (F05).** `WcagKnowledgeBase.search`, and the remaining knowledge base entries.
5. **Storage and report (F10, F11).** `JdbcAuditRepository`, then `ReportService`.
6. **Claude (F03, F07).** `ClaudeLLMClient.send`, then `IssueAssistant`.
7. **Agent loop (F04).** `ToolRegistry.execute`, then `AgentController.run`. Enable `AgentControllerTest` first and make it pass.
8. **Plan mode (F06), then Fix mode (F08).** `ApprovalGate` and `MemoryManager` come with Fix mode.
9. **GUI.** Server-Sent Events, then the React views.

The Todo Tree extension lists every `TODO(` comment, grouped by file.

## Working with AI tools

The project requires AI-assisted development, and you remain responsible for the result. Some habits that help:

- **Give the AI your design, not just the task.** Paste the relevant sequence diagram and class description from your Stage 1 report, and the file you're working on. Ask it to follow the existing interfaces.
- **Ask for one method at a time, with tests.** Read every line before accepting it. When it gets something wrong, that's a log entry.
- **Write log entries as you go** in `docs/AI-TOOLS-AND-LOG.md`, not at the end.
- **Don't use the SDK's tool runner** (`BetaToolRunner`). It runs tools itself and would bypass `ToolRegistry`'s mode checks.
- **Keep `com.anthropic` imports inside `ClaudeLLMClient`.** That's your Adapter pattern; check it after the AI edits other files.

## Things that commonly go wrong

- **Spring Boot 4 compared with older examples.** This project uses Spring Boot 4.0.6, and many AI suggestions and tutorials show Spring Boot 3. Two signs of older code are `spring-boot-starter-web` (now `spring-boot-starter-webmvc`) and `com.fasterxml.jackson.databind.ObjectMapper` in Spring code (Spring Boot 4 uses Jackson 3, in `tools.jackson`). The Anthropic SDK uses the older Jackson 2 internally; both can be on the classpath.
- **"Could not find or load main class".** Let the Java extension finish importing, then run **Java: Clean Java Language Server Workspace** from the Command Palette.
- **`mvnw` permission denied on macOS.** Run `chmod +x mvnw`.
- **Port 8080 already in use.** Stop the other run in VS Code's Debug toolbar, or change `server.port`.
- **Dependency errors mentioning okhttp or kotlin.** Run `./mvnw dependency:tree` and ask your AI tool to explain the conflict.

## Versions

Spring Boot 4.0.6, Java 21, Anthropic Java SDK 2.70.0 (model `claude-opus-5-5`), Playwright for Java 1.63.0, jsoup 1.23.2, picocli 4.7.7, Maven 3.9.16 (through the wrapper), React 19.3.0 and Vite 8.3.4. Newer patch versions are usually fine; change one at a time and rerun the tests.
=======
Stage 1 Assignment is Stage1-Design-Report.md
>>>>>>> 340511274b7c317bb7e14b062801f5291672f564
