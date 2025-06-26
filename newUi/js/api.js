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
