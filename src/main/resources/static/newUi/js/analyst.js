import * as api from './api.js';

const urlParams = new URLSearchParams(window.location.search);
const user = urlParams.get('user') || 'analyst';

document.getElementById('userInfo').innerHTML = `
  <span class="role-badge role-analyst">ANALYST</span>
  <span>${user}</span>
`;

// --- TASKS ---
async function loadTasks() {
  const tasks = await api.getTasksByAssignee(user);
  renderTasks(tasks);
}

function renderTasks(tasks) {
  const tbody = document.querySelector('#tasksTable tbody');
  tbody.innerHTML = '';
  if (!tasks.length) {
    tbody.innerHTML = '<tr><td colspan="4" class="empty-state">No tasks assigned.</td></tr>';
    return;
  }
  for (const task of tasks) {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${task.id}</td>
      <td>${task.title || task.taskName}</td>
      <td><span class="status-badge status-${task.status}">${task.status}</span></td>
      <td>
        <button class="btn btn-success" onclick="completeTask('${task.id}')">Complete</button>
        <button class="btn btn-secondary" onclick="showDetails('${task.id}')">Details</button>
      </td>
    `;
    tbody.appendChild(tr);
  }
}

window.completeTask = function(taskId) {
  api.completeTask(taskId)
    .then(() => loadTasks())
    .catch(e => alert(e.message));
};

window.showDetails = function(taskId) {
  // For demo, just show a modal with taskId. You can expand this to fetch and show more details.
  const modal = document.getElementById('taskDetailsModal');
  const body = document.getElementById('taskDetailsBody');
  body.innerHTML = `<div>Task ID: <b>${taskId}</b></div>`;
  modal.style.display = 'flex';
};

window.closeTaskModal = function() {
  document.getElementById('taskDetailsModal').style.display = 'none';
};

// --- CASES ---
async function loadCases() {
  const cases = await api.getCasesByAssignee(user);
  renderCases(cases);
}

function renderCases(cases) {
  const tbody = document.querySelector('#casesTable tbody');
  tbody.innerHTML = '';
  if (!cases.length) {
    tbody.innerHTML = '<tr><td colspan="5" class="empty-state">No cases assigned.</td></tr>';
    return;
  }
  for (const c of cases) {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${c.id}</td>
      <td>${c.caseName || c.title}</td>
      <td><span class="status-badge status-${c.status}">${c.status}</span></td>
      <td>${c.createdDate ? new Date(c.createdDate).toLocaleString() : ''}</td>
      <td>
        <button class="btn btn-secondary" onclick="showCaseDetails('${c.id}')">Details</button>
        <button class="btn btn-primary" onclick="editCase('${c.id}')">Edit</button>
        <button class="btn btn-danger" onclick="abandonCase('${c.id}')">Abandon</button>
      </td>
    `;
    tbody.appendChild(tr);
  }
}

window.showCaseDetails = async function(caseId) {
  const c = await api.getCaseById(caseId);
  const modal = document.getElementById('caseDetailsModal');
  const body = document.getElementById('caseDetailsBody');
  body.innerHTML = `
    <div><b>Case ID:</b> ${c.id}</div>
    <div><b>Name:</b> ${c.caseName || c.title}</div>
    <div><b>Status:</b> <span class="status-badge status-${c.status}">${c.status}</span></div>
    <div><b>Description:</b> ${c.description || ''}</div>
    <div><b>Created:</b> ${c.createdDate ? new Date(c.createdDate).toLocaleString() : ''}</div>
    <div><b>Assignee:</b> ${c.assignee || ''}</div>
  `;
  modal.style.display = 'flex';
};

window.closeCaseModal = function() {
  document.getElementById('caseDetailsModal').style.display = 'none';
};

window.editCase = async function(caseId) {
  const c = await api.getCaseById(caseId);
  const modal = document.getElementById('caseEditModal');
  const form = document.getElementById('caseEditForm');
  form.caseId.value = c.id;
  form.caseName.value = c.caseName || c.title || '';
  form.description.value = c.description || '';
  modal.style.display = 'flex';
};

window.saveCaseEdit = async function() {
  const form = document.getElementById('caseEditForm');
  const caseId = form.caseId.value;
  const data = {
    caseName: form.caseName.value,
    description: form.description.value
  };
  try {
    await api.updateCase(caseId, data);
    document.getElementById('caseEditModal').style.display = 'none';
    loadCases();
  } catch (e) {
    alert(e.message);
  }
};

window.closeCaseEditModal = function() {
  document.getElementById('caseEditModal').style.display = 'none';
};

window.abandonCase = async function(caseId) {
  if (!confirm('Are you sure you want to abandon this case?')) return;
  try {
    await api.abandonCase(caseId);
    loadCases();
  } catch (e) {
    alert(e.message);
  }
};

window.onload = function() {
  loadTasks();
  loadCases();
};
