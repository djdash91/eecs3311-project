import { useState } from 'react';
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
  const [audit, setAudit] = useState(null);

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
      <main id="main" tabIndex={-1}>
        {view === 'audit' && <AuditView audit={audit} onAuditLoaded={setAudit} />}
        {view === 'agent' && <AgentPanel audit={audit} />}
        {view === 'reports' && <ReportView audit={audit} />}
      </main>
    </>
  );
}
