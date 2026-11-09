import { useState } from 'react';
import { exportPatchedPage, exportReport } from '../api.js';

// Export report and Download fixed page (F10, UC10). The text is fetched, put in a Blob and saved
// by clicking a temporary link with the download attribute.
export default function ReportView({ audit }) {
  const [busy, setBusy] = useState(false);
  const [status, setStatus] = useState('');
  const [error, setError] = useState(null);

  if (!audit) {
    return (
      <section aria-labelledby="reports-heading">
        <h1 id="reports-heading" tabIndex={-1}>Reports</h1>
        <p>Load a page on the Audit page first.</p>
      </section>
    );
  }

  async function download(fetchText, fileName, type, doneMessage) {
    setBusy(true);
    setError(null);
    setStatus('');
    try {
      saveFile(await fetchText(audit.id), fileName, type);
      setStatus(doneMessage);
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }

  const name = fileNameFor(audit.source);

  return (
    <section aria-labelledby="reports-heading">
      <h1 id="reports-heading" tabIndex={-1}>Reports</h1>
      <p>Page: {audit.source}</p>
      <p>
        The report lists the issue counts from the first and latest scans, the verified fixes and the
        escalated issues. The fixed page is the corrected HTML to copy into your site; AccessFix never
        changes the live site.
      </p>
      <div className="button-row">
        <button type="button" disabled={busy}
          onClick={() => download(exportReport, `${name}-accessibility-report.md`, 'text/markdown',
            'The report was downloaded.')}>
          Export report
        </button>
        <button type="button" disabled={busy}
          onClick={() => download(exportPatchedPage, `${name}-fixed.html`, 'text/html',
            'The fixed page was downloaded.')}>
          Download fixed page
        </button>
      </div>
      <p role="status">{status}</p>
      {error && <p role="alert" className="error">{error}</p>}
    </section>
  );
}

function saveFile(text, fileName, type) {
  const url = URL.createObjectURL(new Blob([text], { type }));
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.append(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

// "https://example.com/shop/" or "shop.html" becomes a safe file name such as "example-com-shop".
function fileNameFor(source) {
  const name = source.replace(/^https?:\/\//, '').replace(/\.html?$/, '').replace(/[^a-z0-9]+/gi, '-');
  return name.replace(/^-+|-+$/g, '').toLowerCase() || 'page';
}
