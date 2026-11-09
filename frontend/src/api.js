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

const issuePath = (auditId, issueId) => `/audits/${auditId}/issues/${encodeURIComponent(issueId)}`;

export const loadPageFromUrl = (url) => request('POST', '/audits', { url });
export const loadPageFromHtml = (html, fileName) => request('POST', '/audits', { html, fileName });
export const rescan = (auditId) => request('POST', `/audits/${auditId}/scan`);
export const searchWcag = (query) => request('GET', `/wcag?q=${encodeURIComponent(query)}`);

export const explain = (auditId, issueId) => request('POST', `${issuePath(auditId, issueId)}/explain`);
export const suggestAltText = (auditId, issueId) => request('POST', `${issuePath(auditId, issueId)}/alt-text`);
export const applyFix = (auditId, proposal) => request('POST', `/audits/${auditId}/fixes`, proposal);
export const undoLastFix = (auditId) => request('POST', `/audits/${auditId}/fixes/undo`);

export const sendMessage = (auditId, text) => request('POST', `/audits/${auditId}/ask`, { text });
export const createPlan = (auditId, goal) => request('POST', `/audits/${auditId}/plan`, { text: goal });
export const approvePlan = (auditId) => request('POST', `/audits/${auditId}/plan/approve`);
export const startFix = (auditId) => request('POST', `/audits/${auditId}/fix-session`);
export const submitDecision = (auditId, approved, reason) =>
  request('POST', `/audits/${auditId}/fix-session/decision`, { approved, reason });

export const exportReport = (auditId) => request('GET', `/audits/${auditId}/report`);
export const exportPatchedPage = (auditId) => request('GET', `/audits/${auditId}/fixed-page`);

// The names SseEventStream gives events: one per AgentEvent.Type.
export const EVENT_TYPES = [
  'PROGRESS',
  'TOOL_CALL',
  'FIX_PROPOSED',
  'FIX_VERIFIED',
  'FIX_ROLLED_BACK',
  'ESCALATED',
  'SESSION_FINISHED',
  'ERROR',
];

// Live agent events (SD05). The browser reconnects automatically if the connection drops.
// onEvent receives the AgentEvent: { auditId, type, message, data }.
export function openEventStream(auditId, onEvent) {
  const source = new EventSource(`/api/audits/${auditId}/events`);
  for (const type of EVENT_TYPES) {
    source.addEventListener(type, (e) => onEvent(JSON.parse(e.data)));
  }
  return () => source.close();
}

// Readable names for FixType values, for the plan and fix proposals.
export const FIX_TYPE_NAMES = {
  SET_ALT_TEXT: 'Set alt text',
  ADD_LABEL: 'Add a visible label',
  SET_ARIA_LABEL: 'Set an aria-label',
  SET_LANGUAGE: 'Set the page language',
  SET_TITLE: 'Set the page title',
};
