# KUMA (Stage 3)

Nothing to do here yet. This folder is a placeholder so Stage 3 has a home.

## What KUMA is

KUMA is a Python SDK (package `kuma-defuzex`, Python 3.10 or newer) for evaluating AI agents. It sends a test case to your agent as step-by-step inputs, captures evidence, and produces a judgment. It doesn't run your agent for you, so you write a small Python script that connects KUMA to AccessFix.

## How AccessFix will connect

The REST API is the natural connection point: a Python script can call `POST /api/audits`, `/ask`, `/plan`, `/fix-session` and `/fix-session/decision`, and read the event stream. The sample pages in `samples/` make good test contexts.

## Prepare for it now

- Publish an `AgentEvent` for every tool call, with the tool name and a short input summary. That record is the evidence you'll need for behaviours such as "the agent searched WCAG before citing a criterion" (BR-01) or "the agent didn't propose a rejected fix again" (F11).
- Keep prompts in the mode classes, not scattered through the code, so a prompt fix in Stage 3 is a one-file change you can describe as before fix, modification, after fix.
- Note surprising agent behaviour as you build. Those notes become test cases.

## Setup, when you get there

```bash
python -m venv .venv
# Windows: .\.venv\Scripts\Activate.ps1    macOS/Linux: source .venv/bin/activate
python -m pip install --upgrade kuma-defuzex
kuma quickstart
```

See the KUMA repository's SDK guide for creating runs and test cases.
