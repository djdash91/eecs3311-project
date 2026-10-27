// One function per RestApiController endpoint. Components call these, never fetch() directly,
// so the HTTP details live in one place (the same idea as the facade on the backend).

async function request(method, path, body) {
  const response = await fetch(`/api${path}`, {
    method,
    headers: body === undefined ? {} : { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const type = response.headers.get('Content-Type') ?? '';
  const data = type.includes('application/json') ? await response.json() : await response.text();
  if (!response.ok) {
    // ApiExceptionHandler sends {"error": "..."}
    throw new Error(data?.error ?? `Request failed (${response.status})`);
  }
  return data;
}

export const loadPageFromUrl = (url) => request('POST', '/audits', { url });
export const loadPageFromHtml = (html, fileName) => request('POST', '/audits', { html, fileName });
export const rescan = (auditId) => request('POST', `/audits/${auditId}/scan`);
export const searchWcag = (query) => request('GET', `/wcag?q=${encodeURIComponent(query)}`);

// TODO: add explain, suggestAltText, applyFix, undoLastFix, sendMessage, createPlan, approvePlan,
// startFix, submitDecision, exportReport and exportPatchedPage, matching RestApiController.

// Live agent events (SD05). The browser reconnects automatically if the connection drops.
export function openEventStream(auditId, onEvent) {
  const source = new EventSource(`/api/audits/${auditId}/events`);
  // TODO: add a listener for each AgentEvent type you send, for example:
  // source.addEventListener('FIX_PROPOSED', (e) => onEvent(JSON.parse(e.data)));
  return () => source.close();
}
