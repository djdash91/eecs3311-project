import { useEffect, useRef, useState } from 'react';
import {
  FIX_TYPE_NAMES,
  approvePlan,
  createPlan,
  openEventStream,
  sendMessage,
  startFix,
  submitDecision,
} from '../api.js';

const MODES = [
  { id: 'ask', label: 'Ask' },
  { id: 'plan', label: 'Plan' },
  { id: 'fix', label: 'Fix' },
];

// Ask, Plan and Fix modes (F04, F06, F08). Progress arrives over the event stream (SD03, SD05) and is
// announced politely; a new fix proposal gets keyboard focus.
export default function AgentPanel({ audit, onScanChanged }) {
  if (!audit) {
    return (
      <section aria-labelledby="agent-heading">
        <h1 id="agent-heading" tabIndex={-1}>Agent</h1>
        <p>Load a page on the Audit page first.</p>
      </section>
    );
  }
  return <AgentWorkspace audit={audit} onScanChanged={onScanChanged} />;
}

function AgentWorkspace({ audit, onScanChanged }) {
  const [mode, setMode] = useState('ask');
  const [busy, setBusy] = useState(false);
  const [status, setStatus] = useState('');
  const [activity, setActivity] = useState([]);
  const [error, setError] = useState(null);

  const [question, setQuestion] = useState('');
  const [conversation, setConversation] = useState([]);
  const [goal, setGoal] = useState('');
  const [plan, setPlan] = useState(null);
  const [pending, setPending] = useState(null); // { proposal, beforeAndAfter }
  const [fixLog, setFixLog] = useState([]);
  const [finished, setFinished] = useState(null);

  // The latest callback, so the event stream isn't reopened every time App renders.
  const scanChanged = useRef(onScanChanged);
  scanChanged.current = onScanChanged;

  useEffect(() => openEventStream(audit.id, (event) => {
    setActivity((items) => [...items, event]);
    switch (event.type) {
      case 'TOOL_CALL':
      case 'ERROR': // the failed request shows the error, with Retry
        return;
      case 'FIX_PROPOSED':
        setPending(event.data);
        setStatus('A fix is waiting for your decision.');
        return;
      case 'FIX_VERIFIED':
        setFixLog((log) => [...log, { kept: true, text: event.message }]);
        scanChanged.current(event.data.scanAfter);
        break;
      case 'FIX_ROLLED_BACK':
      case 'ESCALATED':
        setFixLog((log) => [...log, { kept: false, text: event.message }]);
        break;
      default:
        break;
    }
    setStatus(event.message);
  }), [audit.id]);

  // Runs one request. On failure the error is shown with Retry, which runs `retry` (UC04 1d, UC06 1b).
  async function perform(action, retry) {
    setBusy(true);
    setError(null);
    setStatus('');
    setActivity([]);
    try {
      await action();
    } catch (e) {
      setError({ message: e.message, retry });
      setStatus('');
    } finally {
      setBusy(false);
    }
  }

  function ask(text) {
    perform(async () => {
      const result = await sendMessage(audit.id, text);
      setConversation((items) => [...items, { question: text, ...result }]);
      setQuestion('');
    }, () => ask(text));
  }

  function makePlan(text) {
    perform(async () => setPlan(await createPlan(audit.id, text || null)), () => makePlan(text));
  }

  function approve() {
    perform(async () => {
      await approvePlan(audit.id);
      setPlan((current) => ({ ...current, approved: true }));
      setStatus('Plan approved. Select Fix, then Start, to begin fixing.');
    }, approve);
  }

  // A result either has a proposal waiting (the run paused) or the session has finished.
  function showFixResult(result) {
    if (result.pendingProposal) {
      // The FIX_PROPOSED event may arrive before or after this response; keep its before-and-after view.
      setPending((current) => (sameProposal(current?.proposal, result.pendingProposal)
        ? current
        : { proposal: result.pendingProposal, beforeAndAfter: null }));
    } else {
      setPending(null);
      setFinished(result);
    }
  }

  function start() {
    perform(async () => {
      setFinished(null);
      setFixLog([]);
      showFixResult(await startFix(audit.id));
    }, start);
  }

  // If the run fails after a decision, the decision has been carried out and the session can be
  // started again; verified fixes are kept (F08).
  function decide(approved, reason) {
    perform(async () => {
      try {
        showFixResult(await submitDecision(audit.id, approved, reason || null));
      } catch (e) {
        setPending(null);
        throw e;
      }
    }, start);
  }

  const modeHeading = { ask: 'Ask about the audit', plan: 'Remediation plan', fix: 'Fix session' }[mode];

  return (
    <section aria-labelledby="agent-heading">
      <h1 id="agent-heading" tabIndex={-1}>Agent</h1>

      <div role="group" aria-label="Mode" className="button-row">
        {MODES.map((m) => (
          <button key={m.id} type="button" aria-pressed={mode === m.id} onClick={() => setMode(m.id)}>
            {m.label}
          </button>
        ))}
      </div>

      <p aria-live="polite" className="status">{busy && !status ? 'The agent is working…' : status}</p>
      {error && (
        <div role="alert" className="error">
          <p>{error.message}</p>
          <button type="button" onClick={error.retry} disabled={busy}>Retry</button>
        </div>
      )}

      <h2>{modeHeading}</h2>

      {mode === 'ask' && (
        <>
          <Conversation items={conversation} />
          <form onSubmit={(e) => { e.preventDefault(); ask(question.trim()); }}>
            <label htmlFor="agent-question">Your question</label>
            <textarea id="agent-question" rows={3} required value={question}
              onChange={(e) => setQuestion(e.target.value)} />
            <button type="submit" disabled={busy}>{busy ? 'Working…' : 'Send'}</button>
          </form>
        </>
      )}

      {mode === 'plan' && (
        <>
          <form onSubmit={(e) => { e.preventDefault(); makePlan(goal.trim()); }}>
            <label htmlFor="plan-goal">Goal (optional)</label>
            <input id="plan-goal" type="text" value={goal} onChange={(e) => setGoal(e.target.value)}
              aria-describedby="plan-goal-hint" />
            <button type="submit" disabled={busy}>{plan ? 'Create a new plan' : 'Create plan'}</button>
            <p id="plan-goal-hint" className="hint">For example: fix the sign-up form first.</p>
          </form>
          {plan && <PlanTable plan={plan} />}
          {plan && !plan.approved && (
            <button type="button" onClick={approve} disabled={busy}>Approve</button>
          )}
        </>
      )}

      {mode === 'fix' && (
        <>
          {!pending && (
            <button type="button" onClick={start} disabled={busy}>Start</button>
          )}
          {pending && (
            <Proposal key={proposalKey(pending.proposal)} pending={pending} busy={busy} onDecide={decide} />
          )}
          {fixLog.length > 0 && (
            <>
              <h3>Results so far</h3>
              <ul>
                {fixLog.map((entry, i) => (
                  <li key={i} className={entry.kept ? 'success' : undefined}>{entry.text}</li>
                ))}
              </ul>
            </>
          )}
          {finished && <SessionSummary result={finished} />}
        </>
      )}

      {activity.length > 0 && (
        <details>
          <summary>Agent activity ({activity.length})</summary>
          <ol>
            {activity.map((event, i) => (
              <li key={i}>{event.type === 'TOOL_CALL' ? `Used the ${event.message} tool` : event.message}</li>
            ))}
          </ol>
        </details>
      )}
    </section>
  );
}

function Conversation({ items }) {
  if (items.length === 0) {
    return <p>Ask about the issues, an element or a WCAG criterion. The agent can't change the page in Ask mode.</p>;
  }
  return (
    <ol className="conversation">
      {items.map((item, i) => (
        <li key={i}>
          <p><strong>You:</strong> {item.question}</p>
          <p className="answer"><strong>Agent:</strong> {item.answer}</p>
          {item.stepLimitReached && (
            <p className="notice">The agent reached its step limit, so this answer may be incomplete.</p>
          )}
        </li>
      ))}
    </ol>
  );
}

function PlanTable({ plan }) {
  const items = [...plan.items].sort((a, b) => a.priority - b.priority);
  return (
    <>
      <p>{plan.approved ? 'This plan is approved.' : 'This plan is waiting for your approval.'}</p>
      {/* Focusable so keyboard users can scroll the table on a narrow screen. */}
      <div className="table-scroll" tabIndex={0} role="region" aria-labelledby="plan-caption">
        <table>
          <caption id="plan-caption">Plan items, most urgent first</caption>
          <thead>
            <tr>
              <th scope="col">Priority</th>
              <th scope="col">Issue</th>
              <th scope="col">Fix</th>
              <th scope="col">Reason</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.issueId}>
                <td>{item.priority}</td>
                <th scope="row">{item.issueId}</th>
                <td>{item.fixType ? FIX_TYPE_NAMES[item.fixType] ?? item.fixType : 'Manual (escalated to a person)'}</td>
                <td>{item.reason}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}

// One proposal, with its before-and-after code, Approve and Reject (F08, SD05). It takes focus when it
// appears, so keyboard and screen reader users don't have to look for it.
function Proposal({ pending, busy, onDecide }) {
  const [reason, setReason] = useState('');
  const headingRef = useRef(null);
  const { proposal, beforeAndAfter } = pending;

  useEffect(() => {
    headingRef.current?.focus();
  }, []);

  const value = proposal.value === '' ? 'empty alt text (a decorative image)' : `“${proposal.value}”`;
  const [before, after] = splitBeforeAndAfter(beforeAndAfter);

  return (
    <section aria-labelledby="proposal-heading" className="proposal">
      <h3 id="proposal-heading" tabIndex={-1} ref={headingRef}>Proposed fix for {proposal.issueId}</h3>
      <p>{FIX_TYPE_NAMES[proposal.fixType] ?? proposal.fixType}: {value}</p>
      {beforeAndAfter === null ? (
        <p>Loading the before-and-after code…</p>
      ) : after === null ? (
        <pre><code>{beforeAndAfter}</code></pre>
      ) : (
        <div className="diff">
          <div>
            <h4>Before</h4>
            <pre><code>{before}</code></pre>
          </div>
          <div>
            <h4>After</h4>
            <pre><code>{after}</code></pre>
          </div>
        </div>
      )}
      <form onSubmit={(e) => e.preventDefault()}>
        <label htmlFor="decision-reason">Reason for rejecting (optional)</label>
        <input id="decision-reason" type="text" value={reason} onChange={(e) => setReason(e.target.value)}
          aria-describedby="decision-reason-hint" />
        <p id="decision-reason-hint" className="hint">The agent is told the reason and won't propose this fix again.</p>
        <div className="button-row">
          <button type="button" onClick={() => onDecide(true, null)} disabled={busy}>Approve</button>
          <button type="button" onClick={() => onDecide(false, reason.trim())} disabled={busy}>Reject</button>
        </div>
      </form>
    </section>
  );
}

function SessionSummary({ result }) {
  const counts = Object.entries(result.issueCounts ?? {});
  const total = counts.reduce((sum, [, n]) => sum + n, 0);
  return (
    <section aria-labelledby="summary-heading">
      <h3 id="summary-heading">Session finished</h3>
      {result.stepLimitReached && (
        <p className="notice">The session stopped at its step limit. Start it again to continue with what is left.</p>
      )}
      {result.answer && <p className="answer">{result.answer}</p>}
      <p>Issues left: {total}</p>
      {counts.length > 0 && (
        <ul>
          {counts.map(([rule, n]) => <li key={rule}>{rule}: {n}</li>)}
        </ul>
      )}
    </section>
  );
}

function sameProposal(a, b) {
  return a != null && b != null && proposalKey(a) === proposalKey(b);
}

function proposalKey(p) {
  return `${p.issueId}|${p.fixType}|${p.value}`;
}

// FixService.preview's text is "Before:\n…\n\nAfter:\n…".
function splitBeforeAndAfter(text) {
  const marker = '\n\nAfter:\n';
  const at = text?.indexOf(marker) ?? -1;
  if (!text?.startsWith('Before:\n') || at < 0) {
    return [text, null];
  }
  return [text.slice('Before:\n'.length, at), text.slice(at + marker.length)];
}
