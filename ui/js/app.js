// API Configuration
const API_BASE_URL = 'http://localhost:8080/api';

// Global state
let currentMode = 'draft';
let allCases = [];
let currentUser = 'user'; // Default user

// Valid values from backend
const VALID_CASE_TYPES = [
    "FRAUD_DETECTION", "MONEY_LAUNDERING", "SUSPICIOUS_ACTIVITY", 
    "COMPLIANCE_VIOLATION", "AML", "FRAUD", "COMPLIANCE", "SANCTIONS", "KYC"
];

const VALID_PRIORITIES = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];

const VALID_TYPOLOGIES = [
    "MONEY_LAUNDERING", "TERRORIST_FINANCING", "FRAUD", "SANCTIONS_VIOLATION"
];

// User Management and Role System
const USER_ROLES = {
    ANALYST: 'ANALYST',
    SUPERVISOR: 'SUPERVISOR',
    ADMIN: 'ADMIN'
};

const USERS = {
    'analyst1': { name: 'John Analyst', role: USER_ROLES.ANALYST, email: 'john.analyst@company.com' },
    'analyst2': { name: 'Jane Analyst', role: USER_ROLES.ANALYST, email: 'jane.analyst@company.com' },
    'supervisor1': { name: 'Mike Supervisor', role: USER_ROLES.SUPERVISOR, email: 'mike.supervisor@company.com' },
    'admin1': { name: 'Sarah Admin', role: USER_ROLES.ADMIN, email: 'sarah.admin@company.com' }
};

let currentUserData = USERS[currentUser];

// Role-based permissions
function hasPermission(action) {
    const userRole = currentUserData.role;
    
    switch (action) {
        case 'CREATE_CASE':
            return [USER_ROLES.ANALYST, USER_ROLES.SUPERVISOR, USER_ROLES.ADMIN].includes(userRole);
        case 'APPROVE_CASE':
            return [USER_ROLES.SUPERVISOR, USER_ROLES.ADMIN].includes(userRole);
        case 'ASSIGN_TASK':
            return [USER_ROLES.SUPERVISOR, USER_ROLES.ADMIN].includes(userRole);
        case 'VIEW_ALL_CASES':
            return [USER_ROLES.SUPERVISOR, USER_ROLES.ADMIN].includes(userRole);
        case 'COMPLETE_TASK':
            return [USER_ROLES.ANALYST, USER_ROLES.SUPERVISOR, USER_ROLES.ADMIN].includes(userRole);
        case 'MANAGE_USERS':
            return userRole === USER_ROLES.ADMIN;
        default:
            return true;
    }
}

function switchUser(username) {
    if (USERS[username]) {
        currentUser = username;
        currentUserData = USERS[username];
        updateUIBasedOnRole();
        refreshAllData();
        showSuccess(`Switched to user: ${currentUserData.name} (${currentUserData.role})`);
    } else {
        showAlert('User not found');
    }
}

function updateUIBasedOnRole() {
    const userRole = currentUserData.role;
    
    // Update header to show current user
    const userInfo = document.querySelector('.user-info');
    if (userInfo) {
        userInfo.innerHTML = `
            <i class="fas fa-user"></i>
            <span>${currentUserData.name}</span>
            <span class="role-badge role-${userRole.toLowerCase()}">${userRole}</span>
        `;
    }
    
    // Update user selector
    const userSelect = document.getElementById('user-select');
    if (userSelect) {
        userSelect.value = currentUser;
    }
    
    // Show/hide navigation items based on role
    const approvalsNav = document.querySelector('[data-section="approvals"]');
    if (approvalsNav) {
        approvalsNav.style.display = hasPermission('APPROVE_CASE') ? 'flex' : 'none';
    }
    
    // Handle task assignee input visibility
    const assigneeGroup = document.getElementById('assignee-group');
    const assigneeInput = document.getElementById('assignee-input');
    
    if (assigneeGroup && assigneeInput) {
        if (hasPermission('VIEW_ALL_CASES')) {
            // Supervisors can view tasks for any user
            assigneeGroup.style.display = 'flex';
            assigneeInput.value = currentUser;
            assigneeInput.placeholder = 'Enter username to view their tasks';
        } else {
            // Analysts can only view their own tasks
            assigneeGroup.style.display = 'none';
            assigneeInput.value = currentUser;
        }
    }
    
    // Update button text based on role
    const loadTasksBtn = document.querySelector('[onclick="loadTasks()"]');
    if (loadTasksBtn) {
        if (hasPermission('VIEW_ALL_CASES')) {
            loadTasksBtn.innerHTML = '<i class="fas fa-refresh"></i> Load Tasks';
        } else {
            loadTasksBtn.innerHTML = '<i class="fas fa-refresh"></i> Load My Tasks';
        }
    }
}

function refreshAllData() {
    // Refresh all sections based on current view
    const activeSection = document.querySelector('.section.active');
    if (activeSection) {
        const sectionId = activeSection.id;
        switch (sectionId) {
            case 'dashboard':
                refreshDashboard();
                break;
            case 'cases':
                loadCases();
                break;
            case 'tasks':
                loadTasks();
                break;
            case 'approvals':
                loadApprovals();
                break;
        }
    }
}

// Utility Functions
function showLoading() {
    document.getElementById('loading-overlay').style.display = 'flex';
}

function hideLoading() {
    document.getElementById('loading-overlay').style.display = 'none';
}

function showSuccess(message) {
    const successDiv = document.getElementById('success-message');
    successDiv.querySelector('span').textContent = message;
    successDiv.style.display = 'flex';
    
    setTimeout(() => {
        successDiv.style.display = 'none';
    }, 3000);
}

function showAlert(message, type = 'error') {
    const alertsContainer = document.getElementById('form-alerts');
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type}`;
    alertDiv.innerHTML = `
        <i class="fas fa-${type === 'error' ? 'exclamation-circle' : 'info-circle'}"></i>
        ${message}
    `;
    alertsContainer.appendChild(alertDiv);
    
    setTimeout(() => {
        alertDiv.remove();
    }, 5000);
}

function clearAlerts() {
    document.getElementById('form-alerts').innerHTML = '';
}

function formatDate(dateString) {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function formatStatus(status) {
    return status.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase());
}

// API Functions
async function apiRequest(endpoint, options = {}) {
    const url = `${API_BASE_URL}${endpoint}`;
    const config = {
        ...options,
        headers: {
            'Content-Type': 'application/json',
            ...options.headers,
        },
    };

    try {
        showLoading();
        const response = await fetch(url, config);
        
        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || `HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        return data;
    } catch (error) {
        console.error('API request failed:', error);
        throw error;
    } finally {
        hideLoading();
    }
}

// Navigation
function showSection(sectionId) {
    // Hide all sections
    document.querySelectorAll('.section').forEach(section => {
        section.classList.remove('active');
    });
    
    // Remove active class from all nav buttons
    document.querySelectorAll('.nav-btn').forEach(btn => {
        btn.classList.remove('active');
    });
    
    // Show selected section
    document.getElementById(sectionId).classList.add('active');
    
    // Add active class to clicked nav button
    document.querySelector(`[data-section="${sectionId}"]`).classList.add('active');
    
    // Load data for section
    switch(sectionId) {
        case 'dashboard':
            loadDashboard();
            break;
        case 'cases':
            loadCases();
            break;
        case 'tasks':
            loadTasks();
            break;
        case 'approvals':
            loadApprovals();
            break;
    }
}

// Dashboard Functions
async function loadDashboard() {
    try {
        // Load stats
        await Promise.all([
            loadCaseStats(),
            loadRecentCases()
        ]);
    } catch (error) {
        console.error('Error loading dashboard:', error);
    }
}

async function loadCaseStats() {
    try {
        const cases = await apiRequest('/cases');
        
        // Update stats
        document.getElementById('total-cases').textContent = cases.length;
        document.getElementById('draft-cases').textContent = 
            cases.filter(c => c.status === 'DRAFT').length;
        document.getElementById('pending-cases').textContent = 
            cases.filter(c => c.status === 'PENDING_CASE_CREATION_APPROVAL').length;
        
        // Load tasks count
        try {
            const tasks = await apiRequest(`/tasks/my/${currentUser}`);
            document.getElementById('my-tasks').textContent = tasks.length;
        } catch (error) {
            document.getElementById('my-tasks').textContent = '0';
        }
        
    } catch (error) {
        console.error('Error loading case stats:', error);
        // Set default values on error
        document.getElementById('total-cases').textContent = '0';
        document.getElementById('draft-cases').textContent = '0';
        document.getElementById('pending-cases').textContent = '0';
        document.getElementById('my-tasks').textContent = '0';
    }
}

async function loadRecentCases() {
    try {
        const cases = await apiRequest('/cases');
        const recentCases = cases.slice(0, 5); // Get last 5 cases
        
        const container = document.getElementById('recent-cases');
        
        if (recentCases.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-folder-open"></i>
                    <h3>No Cases Found</h3>
                    <p>Create your first case to get started.</p>
                </div>
            `;
            return;
        }
        
        container.innerHTML = recentCases.map(caseItem => `
            <div class="case-card" onclick="showCaseDetails('${caseItem.id}')">
                <div class="case-header">
                    <div class="case-number">${caseItem.caseNumber}</div>
                    <div class="case-status status-${caseItem.status.toLowerCase()}">${formatStatus(caseItem.status)}</div>
                </div>
                <div class="case-description">${caseItem.description || 'No description'}</div>
                <div style="margin-top: 0.5rem; font-size: 0.8rem; color: #6c757d;">
                    Created: ${formatDate(caseItem.createdDate)}
                </div>
            </div>
        `).join('');
        
    } catch (error) {
        console.error('Error loading recent cases:', error);
        document.getElementById('recent-cases').innerHTML = `
            <div class="alert alert-error">
                <i class="fas fa-exclamation-circle"></i>
                Error loading recent cases: ${error.message}
            </div>
        `;
    }
}

async function refreshDashboard() {
    await loadDashboard();
    showSuccess('Dashboard refreshed successfully!');
}

// Cases Functions
async function loadCases() {
    try {
        const cases = await apiRequest('/cases');
        allCases = cases;
        displayCases(cases);
    } catch (error) {
        console.error('Error loading cases:', error);
        document.getElementById('cases-container').innerHTML = `
            <div class="alert alert-error">
                <i class="fas fa-exclamation-circle"></i>
                Error loading cases: ${error.message}
            </div>
        `;
    }
}

function displayCases(cases) {
    const container = document.getElementById('cases-container');
    
    if (cases.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-folder-open"></i>
                <h3>No Cases Found</h3>
                <p>No cases match your current filter criteria.</p>
            </div>
        `;
        return;
    }
    
    container.innerHTML = cases.map(caseItem => `
        <div class="case-card" onclick="showCaseDetails('${caseItem.id}')">
            <div class="case-header">
                <div class="case-number">${caseItem.caseNumber}</div>
                <div class="case-status status-${caseItem.status.toLowerCase()}">${formatStatus(caseItem.status)}</div>
            </div>
            <div class="case-details">
                <div class="case-detail">
                    <label>Type</label>
                    <span>${caseItem.caseType || 'N/A'}</span>
                </div>
                <div class="case-detail">
                    <label>Priority</label>
                    <span>${caseItem.priority || 'N/A'}</span>
                </div>
                <div class="case-detail">
                    <label>Risk Score</label>
                    <span>${caseItem.riskScore || 'N/A'}</span>
                </div>
                <div class="case-detail">
                    <label>Created By</label>
                    <span>${caseItem.createdBy}</span>
                </div>
            </div>
            <div class="case-description">${caseItem.description || 'No description'}</div>
            <div class="case-actions">
                ${caseItem.status === 'DRAFT' ? 
                    `<button class="btn btn-warning btn-sm" onclick="event.stopPropagation(); editCase('${caseItem.id}')">
                        <i class="fas fa-edit"></i> Complete Case
                    </button>` : ''
                }
                <button class="btn btn-info btn-sm" onclick="event.stopPropagation(); showCaseDetails('${caseItem.id}')">
                    <i class="fas fa-eye"></i> View Details
                </button>
            </div>
        </div>
    `).join('');
}

function filterCases() {
    const statusFilter = document.getElementById('status-filter').value;
    
    let filteredCases = allCases;
    
    if (statusFilter) {
        filteredCases = filteredCases.filter(c => c.status === statusFilter);
    }
    
    displayCases(filteredCases);
}

async function showCaseDetails(caseId) {
    try {
        const caseItem = await apiRequest(`/cases/${caseId}`);
        
        const modalBody = document.getElementById('modal-body');
        modalBody.innerHTML = `
            <div class="case-details-modal">
                <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem; margin-bottom: 1.5rem;">
                    <div class="case-detail">
                        <label>Case Number</label>
                        <span>${caseItem.caseNumber}</span>
                    </div>
                    <div class="case-detail">
                        <label>Status</label>
                        <span class="case-status status-${caseItem.status.toLowerCase()}">${formatStatus(caseItem.status)}</span>
                    </div>
                    <div class="case-detail">
                        <label>Type</label>
                        <span>${caseItem.caseType || 'N/A'}</span>
                    </div>
                    <div class="case-detail">
                        <label>Priority</label>
                        <span>${caseItem.priority || 'N/A'}</span>
                    </div>
                    <div class="case-detail">
                        <label>Risk Score</label>
                        <span>${caseItem.riskScore || 'N/A'}</span>
                    </div>
                    <div class="case-detail">
                        <label>Entity</label>
                        <span>${caseItem.entity || 'N/A'}</span>
                    </div>
                    <div class="case-detail">
                        <label>Alert ID</label>
                        <span>${caseItem.alertId || 'N/A'}</span>
                    </div>
                    <div class="case-detail">
                        <label>Typology</label>
                        <span>${caseItem.typology || 'N/A'}</span>
                    </div>
                    <div class="case-detail">
                        <label>Created By</label>
                        <span>${caseItem.createdBy}</span>
                    </div>
                    <div class="case-detail">
                        <label>Created Date</label>
                        <span>${formatDate(caseItem.createdDate)}</span>
                    </div>
                </div>
                
                <div class="case-detail" style="margin-bottom: 1rem;">
                    <label>Description</label>
                    <div style="background: #f8f9fa; padding: 1rem; border-radius: 5px; margin-top: 0.5rem;">
                        ${caseItem.description || 'No description provided'}
                    </div>
                </div>
                
                ${caseItem.processInstanceId ? `
                    <div class="case-detail">
                        <label>Workflow Instance</label>
                        <span>${caseItem.processInstanceId}</span>
                    </div>
                ` : ''}
            </div>
        `;
        
        document.getElementById('modal-title').textContent = `Case Details - ${caseItem.caseNumber}`;
        document.getElementById('case-modal').style.display = 'flex';
        
    } catch (error) {
        console.error('Error loading case details:', error);
        showAlert(`Error loading case details: ${error.message}`);
    }
}

function closeModal() {
    document.getElementById('case-modal').style.display = 'none';
}

// Tasks Functions
async function loadTasks() {
    const assignee = document.getElementById('assignee-input').value || currentUser;
    
    // Check if user has permission to view all tasks or just their own
    const canViewAllTasks = hasPermission('VIEW_ALL_CASES');
    const actualAssignee = canViewAllTasks ? assignee : currentUser;
    
    try {
        // Load both Flowable tasks and database tasks
        const [flowableTasks, dbTasks] = await Promise.all([
            apiRequest(`/tasks/my/${actualAssignee}`).catch(() => []), // Flowable tasks
            apiRequest(`/tasks/by-assignee/${actualAssignee}`).catch(() => []) // Database tasks
        ]);
        
        // Debug output: show raw API response in UI
        const debugDiv = document.getElementById('tasks-debug');
        if (debugDiv) {
            debugDiv.innerHTML = `<pre style='background:#f8f8f8; color:#333; padding:8px; border-radius:4px; max-height:200px; overflow:auto;'>DB Tasks: ${JSON.stringify(dbTasks, null, 2)}\nFlowable Tasks: ${JSON.stringify(flowableTasks, null, 2)}</pre>`;
        }
        
        displayTasks(flowableTasks, dbTasks);
    } catch (error) {
        console.error('Error loading tasks:', error);
        document.getElementById('tasks-container').innerHTML = `
            <div class="alert alert-error">
                <i class="fas fa-exclamation-circle"></i>
                Error loading tasks: ${error.message}
            </div>
        `;
    }
}

function displayTasks(flowableTasks = [], dbTasks = []) {
    const container = document.getElementById('tasks-container');
    if (flowableTasks.length === 0 && dbTasks.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-tasks"></i>
                <h3>No Tasks Found</h3>
                <p>No tasks are currently assigned to this user.</p>
            </div>
        `;
        return;
    }
    let html = '';
    // Display Database Tasks (Case-related tasks)
    if (dbTasks.length > 0) {
        html += '<h3><i class="fas fa-clipboard-list"></i> Case Management Tasks</h3>';
        html += dbTasks.map(task => {
            const canAssign = hasPermission('ASSIGN_TASK') && task.status !== 'COMPLETED';
            const canUnassign = canAssign && task.assignee; // Only show unassign if assigned
            return `
            <div class="task-card task-db">
                <div class="task-header">
                    <div class="task-name">${task.title || task.taskName}</div>
                    <div class="task-status status-${(task.status || '').toLowerCase()}">${task.status || 'Open'}</div>
                </div>
                <div class="task-details">
                    <div class="task-detail"><label>Task Type</label><span>${task.title || task.taskName}</span></div>
                    <div class="task-detail"><label>Case ID</label><span>${task.caseId}</span></div>
                    <div class="task-detail"><label>Priority</label><span class="priority-${(task.priority || 'normal').toLowerCase()}">${task.priority || 'Normal'}</span></div>
                    <div class="task-detail"><label>Assigned To</label><span>${task.assignee || 'Unassigned'}</span></div>
                    <div class="task-detail"><label>Created</label><span>${formatDate(task.createdAt)}</span></div>
                </div>
                <div class="task-description">${task.description || 'No description provided'}</div>
                <div class="task-actions">
                    ${task.status === 'OPEN' ? `<button class="btn btn-primary btn-sm" onclick="completeTask('${task.id}', 'database')"><i class="fas fa-check"></i> Complete Task</button>` : ''}
                    <button class="btn btn-info btn-sm" onclick="viewCaseFromTask('${task.caseId}')"><i class="fas fa-folder-open"></i> View Case</button>
                    ${canAssign ? `<button class="btn btn-warning btn-sm" onclick="openAssignModal('${task.id}')"><i class='fas fa-user-edit'></i> Assign/Reassign</button>` : ''}
                    ${canUnassign ? `<button class="btn btn-danger btn-sm" onclick="unassignTask('${task.id}')"><i class='fas fa-user-slash'></i> Unassign</button>` : ''}
                </div>
            </div>
            `;
        }).join('');
    }
    
    // Display Flowable Tasks (BPMN Workflow tasks)
    if (flowableTasks.length > 0) {
        html += '<h3><i class="fas fa-project-diagram"></i> Workflow Tasks</h3>';
        html += flowableTasks.map(task => `
            <div class="task-card task-flowable">
                <div class="task-header">
                    <div class="task-name">${task.name}</div>
                    <div class="task-status status-active">Active</div>
                </div>
                <div class="task-details">
                    <div class="task-detail"><label>Task ID</label><span>${task.id}</span></div>
                    <div class="task-detail"><label>Process Instance</label><span>${task.processInstanceId}</span></div>
                    <div class="task-detail"><label>Assignee</label><span>${task.assignee || 'Unassigned'}</span></div>
                    <div class="task-detail"><label>Created</label><span>${formatDate(task.createTime)}</span></div>
                    ${task.dueDate ? `<div class="task-detail"><label>Due Date</label><span>${formatDate(task.dueDate)}</span></div>` : ''}
                </div>
                <div class="task-description">${task.description || 'BPMN workflow task'}</div>
                <div class="task-actions">
                    <button class="btn btn-success btn-sm" onclick="completeTask('${task.id}', 'flowable')"><i class="fas fa-check"></i> Complete Workflow Task</button>
                </div>
            </div>
        `).join('');
    }    
    container.innerHTML = html;
}

// Supervisor/Unassign logic
async function unassignTask(taskId) {
    if (!confirm('Are you sure you want to unassign this task?')) return;
    try {
        await apiRequest(`/tasks/assign/${taskId}`, {
            method: 'PUT',
            body: JSON.stringify({ assignee: '', performedBy: currentUser })
        });
        showSuccess('Task unassigned successfully');
        loadTasks();
    } catch (err) {
        showAlert(err.message || 'Error unassigning task');
    }
}
window.unassignTask = unassignTask;

// Approval Functions
async function loadApprovals() {
    try {
        const pendingCases = await apiRequest('/cases/pending-approval');
        displayPendingApprovals(pendingCases);
    } catch (error) {
        console.error('Error loading pending approvals:', error);
        document.getElementById('pending-approvals').innerHTML = `
            <div class="alert alert-error">
                <i class="fas fa-exclamation-circle"></i>
                Error loading pending approvals: ${error.message}
            </div>
        `;
    }
}

function displayPendingApprovals(cases) {
    const container = document.getElementById('pending-approvals');
    
    if (cases.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-check-circle"></i>
                <h3>No Pending Approvals</h3>
                <p>All cases have been reviewed and approved.</p>
            </div>
        `;
        return;
    }
    
    container.innerHTML = cases.map(caseItem => `
        <div class="case-card approval-card">
            <div class="case-header">
                <div class="case-number">${caseItem.caseNumber}</div>
                <div class="case-status status-pending-approval">Pending Approval</div>
            </div>
            <div class="case-details">
                <div class="case-detail">
                    <label>Type</label>
                    <span>${caseItem.caseType || 'N/A'}</span>
                </div>
                <div class="case-detail">
                    <label>Priority</label>
                    <span class="priority-${caseItem.priority?.toLowerCase() || 'normal'}">${caseItem.priority || 'N/A'}</span>
                </div>
                <div class="case-detail">
                    <label>Risk Score</label>
                    <span class="risk-score-${getRiskLevel(caseItem.riskScore)}">${caseItem.riskScore || 'N/A'}</span>
                </div>
                <div class="case-detail">
                    <label>Created By</label>
                    <span>${caseItem.createdBy}</span>
                </div>
                <div class="case-detail">
                    <label>Entity</label>
                    <span>${caseItem.entity || 'N/A'}</span>
                </div>
                <div class="case-detail">
                    <label>Alert ID</label>
                    <span>${caseItem.alertId || 'N/A'}</span>
                </div>
            </div>
            <div class="case-description">
                <strong>Description:</strong>
                <p>${caseItem.description || 'No description'}</p>
            </div>            <div class="approval-actions">
                ${hasPermission('APPROVE_CASE') ? `
                    <button class="btn btn-success btn-sm" onclick="approveCase('${caseItem.id}', true)">
                        <i class="fas fa-check"></i> Approve
                    </button>
                    <button class="btn btn-danger btn-sm" onclick="approveCase('${caseItem.id}', false)">
                        <i class="fas fa-times"></i> Reject
                    </button>
                ` : `
                    <div class="permission-notice">
                        <i class="fas fa-info-circle"></i>
                        You need supervisor privileges to approve cases
                    </div>
                `}
                <button class="btn btn-info btn-sm" onclick="showCaseDetails('${caseItem.id}')">
                    <i class="fas fa-eye"></i> View Details
                </button>
            </div>
        </div>
    `).join('');
}

function getRiskLevel(riskScore) {
    if (!riskScore) return 'unknown';
    if (riskScore >= 80) return 'high';
    if (riskScore >= 50) return 'medium';
    return 'low';
}

async function approveCase(caseId, approved) {
    // Check if user has permission to approve cases
    if (!hasPermission('APPROVE_CASE')) {
        showAlert('You do not have permission to approve cases');
        return;
    }
    
    const comments = prompt(approved ? 
        'Enter approval comments (optional):' : 
        'Enter rejection reason (required):');
    
    if (!approved && (!comments || comments.trim() === '')) {
        showAlert('Rejection reason is required');
        return;
    }
    
    try {
        const response = await apiRequest(`/cases/${caseId}/approve`, {
            method: 'PUT',
            body: JSON.stringify({
                approved: approved,
                comments: comments || '',
                approvedBy: currentUser
            })
        });
        
        const action = approved ? 'approved' : 'rejected';
        showSuccess(`Case ${action} successfully by ${currentUserData.name}!`);
        
        // Refresh approvals list
        loadApprovals();
        
        // Refresh dashboard if it's visible
        if (document.getElementById('dashboard').classList.contains('active')) {
            loadDashboard();
        }
        
    } catch (error) {
        console.error('Error processing approval:', error);
        showAlert(`Error processing approval: ${error.message}`);
    }
}

// Form Functions
function toggleMode() {
    const mode = document.querySelector('input[name="mode"]:checked').value;
    currentMode = mode;
    
    const requiredStars = document.querySelectorAll('.required-star');
    const submitBtn = document.getElementById('submit-btn');
    
    if (mode === 'complete') {
        requiredStars.forEach(star => star.style.display = 'inline');
        submitBtn.innerHTML = '<i class="fas fa-save"></i> Create Complete Case';
    } else {
        requiredStars.forEach(star => star.style.display = 'none');
        submitBtn.innerHTML = '<i class="fas fa-save"></i> Create Draft';
    }
    
    // Clear previous errors
    clearFormErrors();
}

function validateForm() {
    clearFormErrors();
    
    const formData = new FormData(document.getElementById('case-form'));
    const errors = {};
    
    if (currentMode === 'complete') {
        // Required fields for complete case
        if (!formData.get('caseType')) {
            errors.caseType = 'Case type is required for complete cases';
        }
        if (!formData.get('priority')) {
            errors.priority = 'Priority is required for complete cases';
        }
        if (!formData.get('description')?.trim()) {
            errors.description = 'Description is required for complete cases';
        }
        if (!formData.get('riskScore') || parseFloat(formData.get('riskScore')) <= 0) {
            errors.riskScore = 'Risk score is required for complete cases';
        }
    } else {
        // For draft, only description is required
        if (!formData.get('description')?.trim()) {
            errors.description = 'Description is required';
        }
    }
    
    // Validation for provided values
    if (formData.get('caseType') && !VALID_CASE_TYPES.includes(formData.get('caseType'))) {
        errors.caseType = 'Invalid case type';
    }
    if (formData.get('priority') && !VALID_PRIORITIES.includes(formData.get('priority'))) {
        errors.priority = 'Invalid priority';
    }
    if (formData.get('typology') && !VALID_TYPOLOGIES.includes(formData.get('typology'))) {
        errors.typology = 'Invalid typology';
    }
    
    const riskScore = parseFloat(formData.get('riskScore'));
    if (formData.get('riskScore') && (riskScore < 0 || riskScore > 100)) {
        errors.riskScore = 'Risk score must be between 0 and 100';
    }
    
    // Display errors
    Object.keys(errors).forEach(field => {
        showFieldError(field, errors[field]);
    });
    
    return Object.keys(errors).length === 0;
}

function showFieldError(fieldName, message) {
    const field = document.querySelector(`[name="${fieldName}"]`);
    const formGroup = field.closest('.form-group');
    const errorSpan = formGroup.querySelector('.error-message');
    
    formGroup.classList.add('error');
    errorSpan.textContent = message;
}

function clearFormErrors() {
    document.querySelectorAll('.form-group.error').forEach(group => {
        group.classList.remove('error');
        group.querySelector('.error-message').textContent = '';
    });
}

async function createCase(event) {
    event.preventDefault();
    
    if (!validateForm()) {
        return;
    }
    
    clearAlerts();
    
    const formData = new FormData(event.target);
    const requestBody = {};
      // Only include non-empty fields
    for (let [key, value] of formData.entries()) {
        if (value && value.trim()) {
            if (key === 'riskScore') {
                requestBody[key] = parseFloat(value);
            } else {
                requestBody[key] = value;
            }
        }
    }
    
    // Add current user information
    requestBody.createdBy = currentUser;
    requestBody.assignee = currentUser; // Initially assign to creator
    
    const editingCaseId = event.target.dataset.editingCaseId;
    
    try {
        let result;
        let message;
        
        if (editingCaseId) {
            // User Story 2: Complete case creation
            result = await apiRequest(`/cases/${editingCaseId}/complete`, {
                method: 'PUT',
                body: JSON.stringify(requestBody)
            });
            
            if (result.status === 'PENDING_CASE_CREATION_APPROVAL') {
                message = `Case completion submitted for approval! Case Number: ${result.caseNumber}`;
            } else if (result.status === 'READY_FOR_ASSIGNMENT') {
                message = `Case completed and ready for assignment! Case Number: ${result.caseNumber}`;
            } else {
                message = `Case completed successfully! Case Number: ${result.caseNumber}`;
            }
            
        } else {
            // User Story 1: Create new case
            result = await apiRequest('/cases/create', {
                method: 'POST',
                body: JSON.stringify(requestBody)
            });
            
            if (result.status === 'DRAFT') {
                message = `Draft case created successfully! Case Number: ${result.caseNumber}. You can complete it later from the Cases tab.`;
            } else if (result.status === 'PENDING_CASE_CREATION_APPROVAL') {
                message = `Case created and submitted for approval! Case Number: ${result.caseNumber}`;
            } else if (result.status === 'READY_FOR_ASSIGNMENT') {
                message = `Complete case created and workflow started! Case Number: ${result.caseNumber}`;
            } else {
                message = `Case created successfully! Case Number: ${result.caseNumber}`;
            }
        }
        
        showSuccess(message);
        resetForm();
        
        // Refresh dashboard and cases if visible
        if (document.getElementById('dashboard').classList.contains('active')) {
            loadDashboard();
        }
        if (document.getElementById('cases').classList.contains('active')) {
            loadCases();
        }
        
    } catch (error) {
        console.error('Error creating/completing case:', error);
        showAlert(`Error processing case: ${error.message}`);
    }
}

function resetForm() {
    const form = document.getElementById('case-form');
    form.reset();
    form.removeAttribute('data-editing-case-id');
    
    clearFormErrors();
    clearAlerts();
    
    // Reset mode to draft
    document.querySelector('input[name="mode"][value="draft"]').checked = true;
    toggleMode();
    
    // Reset form title
    document.querySelector('#create-case .section-header h2').innerHTML = 
        '<i class="fas fa-plus"></i> Create Case';
    document.getElementById('submit-btn').innerHTML = 
        '<i class="fas fa-save"></i> Create Draft';
}

/**
 * User Story 2: Complete case creation (edit draft case)
 */
async function editCase(caseId) {
    console.log('editCase called with caseId:', caseId);
    alert('Edit case function called for case: ' + caseId); // Temporary debug
    
    try {
        const caseItem = await apiRequest(`/cases/${caseId}`);
        console.log('Case loaded:', caseItem);
        
        if (caseItem.status !== 'DRAFT') {
            showAlert('Only draft cases can be edited for completion');
            return;
        }
        
        // Populate the form with existing case data
        populateFormForEdit(caseItem);
        
        // Switch to create-case section
        showSection('create-case');
        
        // Set mode to complete
        const completeRadio = document.querySelector('input[name="mode"][value="complete"]');
        if (completeRadio) {
            completeRadio.checked = true;
            toggleMode();
        }
        
        // Update form title and button
        const headerElement = document.querySelector('#create-case .section-header h2');
        const submitButton = document.getElementById('submit-btn');
        
        if (headerElement) {
            headerElement.innerHTML = '<i class="fas fa-edit"></i> Complete Case Creation';
        }
        
        if (submitButton) {
            submitButton.innerHTML = '<i class="fas fa-check"></i> Complete Case Creation';
        }
        
        // Store case ID for completion
        const form = document.getElementById('case-form');
        if (form) {
            form.dataset.editingCaseId = caseId;
        }
        
        showSuccess('Draft case loaded for completion. Fill in the required details.');
        
    } catch (error) {
        console.error('Error loading case for edit:', error);
        showAlert(`Error loading case: ${error.message}`);
    }
}

/**
 * Populate form with existing case data
 */
function populateFormForEdit(caseItem) {
    console.log('Populating form with case data:', caseItem);
    
    const form = document.getElementById('case-form');
    if (!form) {
        console.error('Case form not found');
        return;
    }
    
    // Populate form fields
    const fields = {
        'caseType': caseItem.caseType,
        'priority': caseItem.priority,
        'entity': caseItem.entity,
        'alertId': caseItem.alertId,
        'description': caseItem.description,
        'riskScore': caseItem.riskScore,
        'typology': caseItem.typology
    };
    
    Object.entries(fields).forEach(([fieldName, value]) => {
        const field = form.elements[fieldName];
        if (field && value !== null && value !== undefined) {
            field.value = value;
            console.log(`Set ${fieldName} to:`, value);
        }
    });
}

// Function to view case from task
async function viewCaseFromTask(caseId) {
    try {
        // Switch to cases section and show the specific case
        showSection('cases');
        await loadCases();
        
        // Find and show the case details
        setTimeout(() => {
            showCaseDetails(caseId);
        }, 500); // Small delay to ensure cases are loaded
        
    } catch (error) {
        console.error('Error viewing case from task:', error);
        showAlert(`Error viewing case: ${error.message}`);
    }
}

// Supervisor: Render tasks with assign/reassign button
function renderTaskList(tasks) {
  const container = document.getElementById('taskListContainer');
  if (!container) return;
  container.innerHTML = '';
  if (!tasks || tasks.length === 0) {
    container.innerHTML = '<p>No tasks found.</p>';
    return;
  }
  const table = document.createElement('table');
  table.className = 'task-table';
  table.innerHTML = `
    <tr>
      <th>ID</th><th>Title</th><th>Assignee</th><th>Status</th><th>Action</th>
    </tr>
  `;
  tasks.forEach(task => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${task.id || task.taskId || ''}</td>
      <td>${task.title || ''}</td>
      <td>${task.assignee || 'Unassigned'}</td>
      <td>${task.status || ''}</td>
      <td><button onclick="openAssignModal('${task.id || task.taskId}')">Assign/Reassign</button></td>
    `;
    table.appendChild(tr);
  });
  container.appendChild(table);
}

// Open modal for assignment
window.openAssignModal = function(taskId) {
  document.getElementById('assignTaskId').value = taskId;
  document.getElementById('assigneeInput').value = '';
  document.getElementById('assignError').style.display = 'none';
  document.getElementById('assignModal').style.display = 'flex';
};

window.closeAssignModal = function() {
  document.getElementById('assignModal').style.display = 'none';
};

// Handle assignment form submit
function setupAssignForm() {
  const form = document.getElementById('assignForm');
  if (!form) return;
  form.onsubmit = async function(e) {
    e.preventDefault();
    const taskId = document.getElementById('assignTaskId').value;
    const assignee = document.getElementById('assigneeInput').value.trim();
    try {
      await apiRequest(`/tasks/assign/${taskId}`, {
        method: 'PUT',
        body: JSON.stringify({ assignee, performedBy: currentUser })
      });
      closeAssignModal();
      showSuccess(assignee ? 'Task assigned/reassigned successfully' : 'Task unassigned successfully');
      loadTasks();
    } catch (err) {
      showAssignError(err.message || 'Error assigning/unassigning task');
    }
  };
}

function showAssignError(msg) {
  const el = document.getElementById('assignError');
  if (el) {
    el.textContent = msg;
    el.style.display = 'block';
  }
}

document.addEventListener('DOMContentLoaded', setupAssignForm);

// Test connection function
async function testConnection() {
    try {
        const result = await apiRequest('/cases/test');
        showSuccess(`Backend connection successful! ${result.message}`);
        console.log('Connection test result:', result);
    } catch (error) {
        showAlert(`Backend connection failed: ${error.message}`);
        console.error('Connection test failed:', error);
    }
}

// Add test button to console
console.log('Alert Detection System UI loaded!');
console.log('Run testConnection() to test backend connectivity');
console.log('Backend URL:', API_BASE_URL);
