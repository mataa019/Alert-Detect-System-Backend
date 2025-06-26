// Shared API utility for newUi
const API_BASE = '/api/tasks';

export async function fetchJSON(url, options = {}) {
  const res = await fetch(url, options);
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export function getTasksByAssignee(assignee) {
  return fetchJSON(`${API_BASE}/by-assignee/${assignee}`);
}

export function getAllTasks() {
  // You may need to implement this endpoint in your backend
  return fetchJSON(`${API_BASE}/by-assignee/all`);
}

export function getTasksByGroup(groupId) {
  return fetchJSON(`${API_BASE}/group/${groupId}`);
}

export function assignOrReassignTask(taskId, assignee, performedBy) {
  return fetchJSON(`${API_BASE}/assign/${taskId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ assignee, performedBy })
  });
}

export function approveCase(taskId, approved, comments, approvedBy) {
  return fetchJSON(`${API_BASE}/${taskId}/approve-case`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ approved, comments, approvedBy })
  });
}

export function completeTask(taskId, variables = {}) {
  return fetchJSON(`${API_BASE}/complete`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ taskId, variables })
  });
}

export function getAuditLogs(caseId) {
  // You may need to implement this endpoint in your backend
  return fetch(`/api/audit/${caseId}`).then(r => r.json());
}



// CASE API
const CASE_API = '/api/cases';

export function createCase(caseData) {
  return fetchJSON(CASE_API, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(caseData)
  });
}

export function getCases({ status, creator, pendingApproval } = {}) {
  let url = CASE_API + '?';
  if (status) url += `status=${encodeURIComponent(status)}&`;
  if (creator) url += `creator=${encodeURIComponent(creator)}&`;
  if (pendingApproval) url += `pendingApproval=true&`;
  return fetchJSON(url);
}

export function getCaseById(caseId) {
  return fetchJSON(`${CASE_API}/${caseId}`);
}

export function updateCase(caseId, action, data) {
  return fetchJSON(`${CASE_API}/${caseId}?action=${action}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  });
}

export function deleteCase(caseId, deletedBy) {
  return fetchJSON(`${CASE_API}/${caseId}?deletedBy=${encodeURIComponent(deletedBy)}`, {
    method: 'DELETE'
  });
}

export function getRecentCases(limit = 10, user = '') {
  let url = `${CASE_API}/recent?limit=${limit}`;
  if (user) url += `&user=${encodeURIComponent(user)}`;
  return fetchJSON(url);
}

export function getUserRole(user) {
  return fetchJSON(`${CASE_API}/user/role?user=${encodeURIComponent(user)}`);
}

export function abandonCase(caseId, abandonedBy, reason) {
  return fetchJSON(`${CASE_API}/abandon/${caseId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ abandonedBy, reason })
  });
}
export function createTaskForCase(caseId, createdBy) {
  return fetchJSON(`/api/tasks/create/${caseId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ createdBy })
  });
}
