import * as api from './api.js';

const urlParams = new URLSearchParams(window.location.search);
const user = urlParams.get('user') || 'admin';
const role = urlParams.get('role') || 'admin';

document.getElementById('userInfo').innerHTML = `
  <span class="role-badge role-${role}">${role.toUpperCase()}</span>
  <span>${user}</span>
`;

window.loadTasks = async function() {
  let tasks = await api.getAllTasks();
  const status = document.getElementById('statusFilter').value;
  const group = document.getElementById('groupFilter').value;
  const assignee = document.getElementById('assigneeFilter').value;
  if (status) tasks = tasks.filter(t => t.status === status);
  if (group) tasks = tasks.filter(t => (t.candidateGroup || '') === group);
  if (assignee) tasks = tasks.filter(t => (t.assignee || '') === assignee);
  renderTasks(tasks);
};

function renderTasks(tasks) {
  const tbody = document.querySelector('#tasksTable tbody');
  tbody.innerHTML = '';
  if (!tasks.length) {
    tbody.innerHTML = '<tr><td colspan="6" class="empty-state">No tasks found.</td></tr>';
    return;
  }
  for (const task of tasks) {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${task.id}</td>
      <td>${task.title || task.taskName}</td>
      <td><span class="status-badge status-${task.status}">${task.status}</span></td>
      <td>${task.assignee || ''}</td>
      <td>${task.candidateGroup || ''}</td>
      <td>
        <button class="btn btn-primary" onclick="showAssign('${task.id}')">Assign</button>
        <button class="btn btn-success" onclick="showApprove('${task.id}')">Approve/Reject</button>
        <button class="btn btn-secondary" onclick="showAudit('${task.caseId}')">Audit Log</button>
      </td>
    `;
    tbody.appendChild(tr);
  }
}

window.showAssign = function(taskId) {
  const assignee = prompt('Enter new assignee username:');
  if (assignee) {
    api.assignOrReassignTask(taskId, assignee, user)
      .then(() => loadTasks())
      .catch(e => alert(e.message));
  }
};

window.showApprove = function(taskId) {
  const approved = confirm('Approve this case? (Cancel = Reject)');
  const comments = prompt('Comments (optional):');
  api.approveCase(taskId, approved, comments, user)
    .then(() => loadTasks())
    .catch(e => alert(e.message));
};

window.showAudit = function(caseId) {
  api.getAuditLogs(caseId).then(logs => {
    const modal = document.getElementById('auditLogModal');
    const body = document.getElementById('auditLogBody');
    body.innerHTML = logs && logs.length ? logs.map(log => `
      <div class="audit-entry">
        <strong>${log.action}</strong> by <em>${log.performedBy}</em> <br>
        <span>${log.details}</span> <br>
        <small>${new Date(log.timestamp).toLocaleString()}</small>
      </div>
    `).join('') : '<div class="empty-state">No audit logs found.</div>';
    modal.style.display = 'flex';
  });
};

window.closeAuditModal = function() {
  document.getElementById('auditLogModal').style.display = 'none';
};

// --- CASES ---
window.loadCases = async function() {
  const cases = await api.getAllCases();
  renderCases(cases);
  loadRecentCases();
};

function renderCases(cases) {
  const tbody = document.querySelector('#casesTable tbody');
  tbody.innerHTML = '';
  if (!cases.length) {
    tbody.innerHTML = '<tr><td colspan="6" class="empty-state">No cases found.</td></tr>';
    return;
  }
  for (const c of cases) {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${c.id}</td>
      <td>${c.caseName || c.title}</td>
      <td><span class="status-badge status-${c.status}">${c.status}</span></td>
      <td>${c.assignee || ''}</td>
      <td>${c.createdDate ? new Date(c.createdDate).toLocaleString() : ''}</td>
      <td>
        <button class="btn btn-secondary" onclick="showCaseDetails('${c.id}')">Details</button>
        <button class="btn btn-primary" onclick="editCase('${c.id}')">Edit</button>
        <button class="btn btn-danger" onclick="abandonCase('${c.id}')">Abandon</button>
        <button class="btn btn-danger" onclick="deleteCase('${c.id}')">Delete</button>
        <button class="btn btn-secondary" onclick="showAudit('${c.id}')">Audit Log</button>
      </td>
    `;
    tbody.appendChild(tr);
  }
}

window.renderCases = renderCases; // expose for testing/debug

window.showCreateCaseModal = function() {
  document.getElementById('caseCreateModal').style.display = 'flex';
};
window.closeCaseCreateModal = function() {
  document.getElementById('caseCreateModal').style.display = 'none';
};
window.createCase = async function() {
  // Collect and validate all fields
  const type = document.getElementById('createCaseType').value.trim();
  const priority = document.getElementById('createPriority').value.trim();
  const entity = document.getElementById('createEntity').value.trim();
  const alertId = document.getElementById('createAlertId').value.trim();
  const typology = document.getElementById('createTypology').value.trim();
  const riskScore = document.getElementById('createRiskScore').value;
  const description = document.getElementById('createCaseDescription').value;
  const assignee = document.getElementById('createCaseAssignee').value.trim();
  if (!type || !priority || !entity || !alertId || !typology || !riskScore) {
    alert('All mandatory fields are required.');
    return;
  }
  try {
    // 1. Create the case
    const caseData = {
      caseType: type,
      priority,
      entity,
      alertId,
      typology,
      riskScore: parseFloat(riskScore),
      description,
      assignee
    };
    const createdCase = await api.createCase(caseData);
    // 2. Create the first task for the case
    await api.createTaskForCase(createdCase.id, assignee);
    closeCaseCreateModal();
    loadCases();
  } catch (e) {
    alert(e.message);
  }
};
window.deleteCase = async function(caseId) {
  if (!confirm('Are you sure you want to delete this case?')) return;
  try {
    await api.deleteCase(caseId, user);
    loadCases();
  } catch (e) {
    alert(e.message);
  }
};
window.loadRecentCases = async function() {
  const recent = await api.getRecentCases(10, user);
  const ul = document.getElementById('recentCasesList');
  ul.innerHTML = '';
  if (!recent.length) {
    ul.innerHTML = '<li class="empty-state">No recent cases.</li>';
    return;
  }
  for (const c of recent) {
    const li = document.createElement('li');
    li.textContent = `${c.caseName || c.title} (${c.status})`;
    ul.appendChild(li);
  }
};

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
