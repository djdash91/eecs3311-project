import { useState } from 'react';
import {
  applyFix,
  explain,
  loadPageFromHtml,
  loadPageFromUrl,
  rescan,
  searchWcag,
  suggestAltText,
  undoLastFix,
} from '../api.js';

// WORKED EXAMPLE of an accessible form: a visible label tied to the input, the error announced with
// role="alert" and linked to the field, and a busy state screen readers can hear.
export default function AuditView({ audit, onAuditLoaded, onScanChanged }) {
  const [url, setUrl] = useState('');
  const [file, setFile] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  const [lastLoad, setLastLoad] = useState(null);

  // Runs one way of loading a page, and remembers it so Retry can run it again (UC01 2a).
  async function load(loader) {
    setError(null);
    setLoading(true);
    setLastLoad(() => loader);
    try {
      onAuditLoaded(await loader());
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  function handleSubmit(event) {
    event.preventDefault();
    const pageUrl = url;
    load(() => loadPageFromUrl(pageUrl));
  }

  function handleUpload(event) {
    event.preventDefault();
    const chosen = file;
    load(async () => loadPageFromHtml(await chosen.text(), chosen.name));
  }

  const latestScan = audit?.latestScan;

  return (
    <section aria-labelledby="audit-heading">
      <h1 id="audit-heading" tabIndex={-1}>Audit a page</h1>

      <form onSubmit={handleSubmit}>
        <label htmlFor="page-url">Page URL</label>
        <input
          id="page-url"
          type="url"
          required
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          aria-describedby={error ? 'load-error' : undefined}
          aria-invalid={error ? true : undefined}
        />
        <button type="submit" disabled={loading}>
          {loading ? 'Loading page…' : 'Load page'}
        </button>
      </form>

      <form onSubmit={handleUpload}>
        <label htmlFor="page-file">Or upload an HTML file</label>
        <input
          id="page-file"
          type="file"
          accept=".html,.htm,text/html"
          required
          onChange={(e) => setFile(e.target.files[0] ?? null)}
        />
        <button type="submit" disabled={loading}>Upload HTML file</button>
      </form>

      {loading && <p role="status">Loading the page and scanning it…</p>}

      {error && (
        <div id="load-error" role="alert" className="error">
          <p>{error}</p>
          {lastLoad && (
            <button type="button" onClick={() => load(lastLoad)} disabled={loading}>Retry</button>
          )}
        </div>
      )}

      {audit && latestScan && (
        <IssuesSection key={audit.id} audit={audit} scan={latestScan} onScanChanged={onScanChanged} />
      )}

      <WcagSearch />
    </section>
  );
}

// The latest scan's issues, with Re-scan and Undo (F02, F09).
function IssuesSection({ audit, scan, onScanChanged }) {
  const [busy, setBusy] = useState(false);
  // The message and the scan it describes; it's hidden once another fix or the agent changes the scan.
  const [status, setStatus] = useState({ text: '', scan: null });
  const [error, setError] = useState(null);
  const clearStatus = () => setStatus({ text: '', scan: null });

  async function run(action, doneMessage) {
    setBusy(true);
    setError(null);
    clearStatus();
    try {
      const newScan = await action();
      onScanChanged(newScan);
      setStatus({ text: doneMessage(newScan), scan: newScan });
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <section aria-labelledby="issues-heading">
      <h2 id="issues-heading">Issues ({scan.issues.length})</h2>
      <p>Page: {audit.source}</p>

      <div className="button-row">
        <button type="button" disabled={busy}
          onClick={() => run(() => rescan(audit.id), (s) => `Scanned again: ${s.issues.length} issues.`)}>
          Re-scan
        </button>
        <button type="button" disabled={busy}
          onClick={() => run(() => undoLastFix(audit.id), (s) => `Undid the last fix. ${s.issues.length} issues now.`)}>
          Undo last fix
        </button>
      </div>
      <p role="status">{status.scan === scan ? status.text : ''}</p>
      {error && <p role="alert" className="error">{error}</p>}

      {scan.rulesNotRun.length > 0 && (
        <p className="notice">
          These rules didn't run, so their issues may be missing: {scan.rulesNotRun.join(', ')}.
          Select Re-scan to try them again.
        </p>
      )}

      {scan.issues.length === 0 ? (
        <p>No issues found by the automated rules. Manual testing is still needed.</p>
      ) : (
        <ul className="issue-list">
          {scan.issues.map((issue) => (
            <IssueItem key={issue.id} auditId={audit.id} issue={issue} onScanChanged={onScanChanged}
              onAction={clearStatus} />
          ))}
        </ul>
      )}
    </section>
  );
}

// One issue, with Explain (F03) and, for images, Suggest alt text and Apply (F07).
function IssueItem({ auditId, issue, onScanChanged, onAction }) {
  const [explanation, setExplanation] = useState(null);
  const [altText, setAltText] = useState(null);
  const [altNote, setAltNote] = useState('');
  const [result, setResult] = useState(null);
  const [busy, setBusy] = useState(null);
  const [error, setError] = useState(null);
  const altId = `alt-${issue.id}`;

  async function run(name, action) {
    onAction();
    setBusy(name);
    setError(null);
    try {
      await action();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(null);
    }
  }

  const handleExplain = () => run('explain', async () => setExplanation(await explain(auditId, issue.id)));

  const handleSuggest = () => run('suggest', async () => {
    const { suggestion } = await suggestAltText(auditId, issue.id);
    setAltText(suggestion ?? '');
    setAltNote(suggestion === null
      ? 'No suggestion could be made, for example because the image wasn’t shown. Write the alt text yourself.'
      : 'Check the suggestion and edit it if needed, then select Apply.');
  });

  function handleApply(event) {
    event.preventDefault();
    run('apply', async () => {
      const verification = await applyFix(auditId, { issueId: issue.id, fixType: 'SET_ALT_TEXT', value: altText });
      setResult(verification);
      if (verification.kept) {
        onScanChanged(verification.scanAfter);
      }
    });
  }

  return (
    <li>
      <h3>{issue.message}</h3>
      <p>WCAG {issue.criterion} · rule {issue.ruleId}</p>
      <pre><code>{issue.snippet}</code></pre>

      <div className="button-row">
        <button type="button" onClick={handleExplain} disabled={busy !== null}>
          {busy === 'explain' ? 'Explaining…' : 'Explain'}
        </button>
        {issue.ruleId === 'image-alt' && (
          <button type="button" onClick={handleSuggest} disabled={busy !== null}>
            {busy === 'suggest' ? 'Suggesting…' : 'Suggest alt text'}
          </button>
        )}
      </div>

      <div aria-live="polite">
        {busy && <p>Working…</p>}
        {explanation && <ExplanationDetails explanation={explanation} />}
        {result && (
          <p className={result.kept ? 'success' : 'error'}>
            {result.kept ? 'Verified: ' : ''}{result.reason}
          </p>
        )}
      </div>
      {error && <p role="alert" className="error">{error}</p>}

      {altText !== null && (
        <form onSubmit={handleApply}>
          <label htmlFor={altId}>Alt text</label>
          <input
            id={altId}
            type="text"
            value={altText}
            onChange={(e) => setAltText(e.target.value)}
            aria-describedby={`${altId}-note`}
          />
          <button type="submit" disabled={busy !== null}>
            {busy === 'apply' ? 'Applying…' : 'Apply'}
          </button>
          <p id={`${altId}-note`} className="hint">{altNote}</p>
        </form>
      )}
    </li>
  );
}

// An Explanation; the fallback has only whatIsWrong and the criterion (UC03 2a).
function ExplanationDetails({ explanation }) {
  const rows = [
    ['What is wrong', explanation.whatIsWrong],
    ['Who is affected', explanation.whoIsAffected],
    ['WCAG criterion', explanation.criterion],
    ['How to fix it', explanation.howToFix],
  ].filter(([, value]) => value);
  return (
    <dl className="explanation">
      {rows.map(([term, value]) => (
        <div key={term}>
          <dt>{term}</dt>
          <dd>{value}</dd>
        </div>
      ))}
    </dl>
  );
}

// Searching the WCAG reference (F05).
function WcagSearch() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState(null);
  const [error, setError] = useState(null);

  async function handleSearch(event) {
    event.preventDefault();
    setError(null);
    try {
      setResults(await searchWcag(query));
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <section aria-labelledby="wcag-heading">
      <h2 id="wcag-heading">Search WCAG</h2>
      <form onSubmit={handleSearch} role="search" aria-label="WCAG">
        <label htmlFor="wcag-query">Search the WCAG 2.2 A and AA criteria</label>
        <input id="wcag-query" type="search" required value={query} onChange={(e) => setQuery(e.target.value)} />
        <button type="submit">Search</button>
      </form>
      {error && <p role="alert" className="error">{error}</p>}
      <div aria-live="polite">
        {results && (
          results.length === 0 ? (
            <p>No criteria match “{query}”.</p>
          ) : (
            <>
              <p>{results.length} {results.length === 1 ? 'criterion matches' : 'criteria match'}.</p>
              <ul className="wcag-results">
                {results.map((c) => (
                  <li key={c.number}>
                    <h3>{c.number} {c.title} (Level {c.level})</h3>
                    <p>{c.summary}</p>
                    {c.whoItHelps && <p>Helps: {c.whoItHelps}</p>}
                    <p><a href={c.url}>Understanding {c.number} on the W3C website</a></p>
                  </li>
                ))}
              </ul>
            </>
          )
        )}
      </div>
    </section>
  );
}
