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

window.onload = loadTasks;
