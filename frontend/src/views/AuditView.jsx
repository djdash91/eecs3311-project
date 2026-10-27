import { useState } from 'react';
import { loadPageFromUrl } from '../api.js';

// WORKED EXAMPLE of an accessible form: a visible label tied to the input, the error announced with
// role="alert" and linked to the field, and a busy state screen readers can hear.
export default function AuditView({ audit, onAuditLoaded }) {
  const [url, setUrl] = useState('');
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError(null);
    setLoading(true);
    try {
      onAuditLoaded(await loadPageFromUrl(url));
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  const latestScan = audit?.scans?.at(-1);

  return (
    <section aria-labelledby="audit-heading">
      <h1 id="audit-heading">Audit a page</h1>

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
        {/* TODO(F01): add "Upload HTML file" with a labelled file input and loadPageFromHtml. */}
      </form>

      {error && (
        <p id="load-error" role="alert" className="error">
          {error} {/* TODO: add a Retry button for page-load errors (UC01 2a) */}
        </p>
      )}

      <div aria-live="polite">
        {latestScan && (
          <>
            <h2>Issues ({latestScan.issues.length})</h2>
            {latestScan.issues.length === 0 ? (
              <p>No issues found by the automated rules. Manual testing is still needed.</p>
            ) : (
              <ul className="issue-list">
                {latestScan.issues.map((issue) => (
                  <li key={issue.id}>
                    <h3>{issue.message}</h3>
                    <p>WCAG {issue.criterion} · rule {issue.ruleId}</p>
                    <pre><code>{issue.snippet}</code></pre>
                    {/* TODO(F03, F07): Explain and Suggest alt text buttons for this issue */}
                  </li>
                ))}
              </ul>
            )}
          </>
        )}
      </div>
      {/* TODO(F02, F05, F09): Re-scan, Undo and WCAG search */}
    </section>
  );
}
