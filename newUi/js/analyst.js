import * as api from './api.js';

const urlParams = new URLSearchParams(window.location.search);
const user = urlParams.get('user') || 'analyst';

document.getElementById('userInfo').innerHTML = `
  <span class="role-badge role-analyst">ANALYST</span>
  <span>${user}</span>
`;

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

window.onload = loadTasks;
