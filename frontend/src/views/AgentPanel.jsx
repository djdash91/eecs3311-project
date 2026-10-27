// TODO(F04, F06, F08): Ask, Plan and Fix modes.
//  - Open the event stream with openEventStream(audit.id, ...) in a useEffect, and close it on cleanup.
//  - Put progress messages in an aria-live="polite" region so screen reader users hear them.
//  - Show each fix proposal as a before-and-after view with Approve and Reject (and a reason field).
//  - Move keyboard focus to a new proposal so keyboard users don't have to hunt for it.
export default function AgentPanel({ audit }) {
  if (!audit) {
    return (
      <section aria-labelledby="agent-heading">
        <h1 id="agent-heading">Agent</h1>
        <p>Load a page on the Audit page first.</p>
      </section>
    );
  }
  return (
    <section aria-labelledby="agent-heading">
      <h1 id="agent-heading">Agent</h1>
      <p>Not built yet.</p>
    </section>
  );
}
