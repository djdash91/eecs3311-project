// TODO(F10): "Export report" and "Download fixed page". One approach: fetch the text, make a Blob,
// and click a temporary link with the download attribute.
export default function ReportView({ audit }) {
  return (
    <section aria-labelledby="reports-heading">
      <h1 id="reports-heading">Reports</h1>
      <p>{audit ? 'Not built yet.' : 'Load a page on the Audit page first.'}</p>
    </section>
  );
}
