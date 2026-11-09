import { useEffect, useRef, useState } from 'react';
import AuditView from './views/AuditView.jsx';
import AgentPanel from './views/AgentPanel.jsx';
import ReportView from './views/ReportView.jsx';

const VIEWS = [
  { id: 'audit', label: 'Audit' },
  { id: 'agent', label: 'Agent' },
  { id: 'reports', label: 'Reports' },
];

export default function App() {
  const [view, setView] = useState('audit');
  // The AuditSummary from RestApiController: { id, source, latestScan }
  const [audit, setAudit] = useState(null);
  const mainRef = useRef(null);
  const firstRender = useRef(true);

  // Move focus to the new page's content, so keyboard and screen reader users start there.
  useEffect(() => {
    if (firstRender.current) {
      firstRender.current = false;
      return;
    }
    mainRef.current?.querySelector(`#${view}-view h1`)?.focus();
  }, [view]);

  // Re-scan, undo, applied alt text and verified agent fixes all give a new latest scan.
  function handleScanChanged(scan) {
    setAudit((current) => (current ? { ...current, latestScan: scan } : current));
  }

  return (
    <>
      <a className="skip-link" href="#main">Skip to main content</a>
      <header className="site-header">
        <p className="product-name">AccessFix</p>
        <nav aria-label="Main">
          <ul>
            {VIEWS.map((v) => (
              <li key={v.id}>
                <button
                  type="button"
                  aria-current={view === v.id ? 'page' : undefined}
                  onClick={() => setView(v.id)}
                >
                  {v.label}
                </button>
              </li>
            ))}
          </ul>
        </nav>
      </header>
      <main id="main" tabIndex={-1} ref={mainRef}>
        {/* The views stay mounted, so a fix session and its event stream continue while another view is open. */}
        <div id="audit-view" hidden={view !== 'audit'}>
          <AuditView audit={audit} onAuditLoaded={setAudit} onScanChanged={handleScanChanged} />
        </div>
        <div id="agent-view" hidden={view !== 'agent'}>
          <AgentPanel key={audit?.id ?? 'none'} audit={audit} onScanChanged={handleScanChanged} />
        </div>
        <div id="reports-view" hidden={view !== 'reports'}>
          <ReportView audit={audit} />
        </div>
      </main>
    </>
  );
}
