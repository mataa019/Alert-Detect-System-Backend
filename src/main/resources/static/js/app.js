// API Configuration
const API_BASE_URL = 'http://localhost:8080/api';

// Version for cache busting
console.log('Alert Detection System UI v1.2 - Loading...');

// Add error handler for any JavaScript errors
window.addEventListener('error', function(e) {
    console.error('JavaScript Error:', e.error, e.filename, e.lineno);
    alert('JavaScript Error: ' + e.message + ' at line ' + e.lineno);
});

// Global state
let currentMode = 'draft';
let allCases = [];
let currentUser = 'analyst'; // Default user

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
    ADMIN: 'ADMIN'
};

const USERS = {
    'analyst': { name: 'John Analyst', role: USER_ROLES.ANALYST, email: 'john.analyst@company.com' },
    'admin': { name: 'Sarah Admin', role: USER_ROLES.ADMIN, email: 'sarah.admin@company.com' }
};

let currentUserData = USERS[currentUser];

// Role-based permissions
function hasPermission(action) {
    const userRole = currentUserData.role;
      switch (action) {
        case 'CREATE_CASE':
            return true; // Both can create cases
        case 'APPROVE_CASE':
            return userRole === USER_ROLES.ADMIN; // Only admin can approve
        case 'ASSIGN_TASK':
            return userRole === USER_ROLES.ADMIN; // Only admin can assign tasks
        case 'VIEW_ALL_CASES':
            return userRole === USER_ROLES.ADMIN; // Admin can view all, analyst sees own
        case 'COMPLETE_TASK':
            return true; // Both can complete their tasks
        case 'MANAGE_USERS':
            return userRole === USER_ROLES.ADMIN;
        default:
            return false;
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
    // Update role-specific help
    updateRoleHelp();
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

    console.log('=== API REQUEST DEBUG ===');
    console.log('URL:', url);
    console.log('Config:', config);

    try {
        showLoading();
        const response = await fetch(url, config);
        
        console.log('Response status:', response.status);
        console.log('Response ok:', response.ok);
        
        if (!response.ok) {
            const errorText = await response.text();
            console.error('API Error:', errorText);
            throw new Error(errorText || `HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        console.log('Response data:', data);
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
        let cases;
        if (hasPermission('VIEW_ALL_CASES')) {
            // Admin can see all cases stats
            cases = await apiRequest('/cases');
        } else {
            // Analyst can only see their own cases stats
            cases = await apiRequest(`/cases?creator=${currentUser}`);
        }
        
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
        // Use the new recent cases endpoint with proper ordering
        let recentCases;
        if (hasPermission('VIEW_ALL_CASES')) {
            // Admin can see all recent cases
            recentCases = await apiRequest('/cases/recent?limit=5');
        } else {
            // Analyst can only see their own recent cases
            recentCases = await apiRequest(`/cases/recent?limit=5&user=${currentUser}`);
        }
        
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
        let cases;
        if (hasPermission('VIEW_ALL_CASES')) {
            // Admin can see all cases - get recent first for better performance
            cases = await apiRequest('/cases/recent?limit=50'); // Get more recent cases
        } else {
            // Analyst can only see their own cases
            cases = await apiRequest(`/cases/recent?limit=50&user=${currentUser}`);
        }
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
                ${(caseItem.createdBy === currentUser || hasPermission('VIEW_ALL_CASES')) ? 
                    `<button class="btn btn-primary btn-sm" onclick="event.stopPropagation(); editCaseDetails('${caseItem.id}')">
                        <i class="fas fa-edit"></i> Edit
                    </button>` : ''
                }
                ${(caseItem.status === 'DRAFT' || caseItem.createdBy === currentUser || hasPermission('VIEW_ALL_CASES')) ? 
                    `<button class="btn btn-danger btn-sm" onclick="event.stopPropagation(); deleteCase('${caseItem.id}', '${caseItem.caseNumber}')">
                        <i class="fas fa-trash"></i> Delete
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
    
    try {
        let flowableTasks = [];
        let dbTasks = [];
        
        if (canViewAllTasks && (!assignee || assignee === 'all')) {
            // Admin viewing all tasks
            console.log('Loading all tasks for admin');
            [flowableTasks, dbTasks] = await Promise.all([
                apiRequest(`/tasks/group/supervisors`).catch(() => []), // Flowable tasks for supervisors
                apiRequest(`/tasks/all`).catch(() => []) // All database tasks
            ]);
        } else {
            // Specific user tasks (analyst or admin viewing specific user)
            const actualAssignee = canViewAllTasks ? assignee : currentUser;
            console.log('Loading tasks for assignee:', actualAssignee);
            [flowableTasks, dbTasks] = await Promise.all([
                apiRequest(`/tasks/my/${actualAssignee}`).catch(() => []), // Flowable tasks
                apiRequest(`/tasks/by-assignee/${actualAssignee}`).catch(() => []) // Database tasks
            ]);
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
        html += dbTasks.map(task => `
            <div class="task-card task-db">
                <div class="task-header">
                    <div class="task-name">${task.title || task.taskName}</div>
                    <div class="task-status status-${(task.status || '').toLowerCase()}">${task.status || 'Open'}</div>
                </div>
                <div class="task-details">
                    <div class="task-detail">
                        <label>Task Type</label>
                        <span>${task.title || task.taskName}</span>
                    </div>
                    <div class="task-detail">
                        <label>Case ID</label>
                        <span>${task.caseId}</span>
                    </div>
                    <div class="task-detail">
                        <label>Priority</label>
                        <span class="priority-${(task.priority || 'normal').toLowerCase()}">${task.priority || 'Normal'}</span>
                    </div>
                    <div class="task-detail">
                        <label>Assigned To</label>
                        <span>${task.assignee || 'Unassigned'}</span>
                    </div>
                    <div class="task-detail">
                        <label>Created</label>
                        <span>${formatDate(task.createdAt)}</span>
                    </div>
                </div>
                <div class="task-description">
                    ${task.description || 'No description provided'}
                </div>
                <div class="task-actions">
                    ${task.status === 'OPEN' ? `
                        <button class="btn btn-primary btn-sm" onclick="completeTask('${task.id}', 'database')">
                            <i class="fas fa-check"></i> Complete Task
                        </button>
                    ` : ''}
                    <button class="btn btn-info btn-sm" onclick="viewCaseFromTask('${task.caseId}')">
                        <i class="fas fa-folder-open"></i> View Case
                    </button>
                </div>
            </div>
        `).join('');
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
                    <div class="task-detail">
                        <label>Task ID</label>
                        <span>${task.id}</span>
                    </div>
                    <div class="task-detail">
                        <label>Process Instance</label>
                        <span>${task.processInstanceId}</span>
                    </div>
                    <div class="task-detail">
                        <label>Assignee</label>
                        <span>${task.assignee || 'Unassigned'}</span>
                    </div>
                    <div class="task-detail">
                        <label>Created</label>
                        <span>${formatDate(task.createTime)}</span>
                    </div>
                    ${task.dueDate ? `
                    <div class="task-detail">
                        <label>Due Date</label>
                        <span>${formatDate(task.dueDate)}</span>
                    </div>
                    ` : ''}
                </div>
                <div class="task-description">
                    ${task.description || 'BPMN workflow task'}
                </div>
                <div class="task-actions">
                    <button class="btn btn-success btn-sm" onclick="completeTask('${task.id}', 'flowable')">
                        <i class="fas fa-check"></i> Complete Workflow Task
                    </button>
                </div>
            </div>
        `).join('');
    }    
    container.innerHTML = html;
}

async function completeTask(taskId, taskType = 'database') {
    if (!confirm('Are you sure you want to complete this task?')) {
        return;
    }
    
    try {
        if (taskType === 'flowable') {
            // Complete Flowable BPMN task
            await apiRequest(`/tasks/complete/${taskId}`, {
                method: 'PUT',
                body: JSON.stringify({
                    variables: {},
                    action: 'complete'
                })
            });
        } else {
            // Complete database task
            await apiRequest(`/task/update/${taskId}`, {
                method: 'PUT',
                body: JSON.stringify({
                    updatedBy: currentUser,
                    action: 'COMPLETE_TASK'
                })
            });
        }
        
        showSuccess('Task completed successfully!');
        await loadTasks(); // Refresh tasks
        
    } catch (error) {
        console.error('Error completing task:', error);
        showAlert(`Error completing task: ${error.message}`);
    }
}

async function showTaskDetails(taskId) {
    try {
        const task = await apiRequest(`/tasks/${taskId}`);
        
        const modalBody = document.getElementById('modal-body');
        modalBody.innerHTML = `
            <div class="task-details-modal">
                <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem;">
                    <div class="case-detail">
                        <label>Task ID</label>
                        <span>${task.id}</span>
                    </div>
                    <div class="case-detail">
                        <label>Name</label>
                        <span>${task.name}</span>
                    </div>
                    <div class="case-detail">
                        <label>Assignee</label>
                        <span>${task.assignee || 'Unassigned'}</span>
                    </div>
                    <div class="case-detail">
                        <label>Created</label>
                        <span>${formatDate(task.createTime)}</span>
                    </div>
                    <div class="case-detail">
                        <label>Process Instance</label>
                        <span>${task.processInstanceId || 'N/A'}</span>
                    </div>
                    <div class="case-detail">
                        <label>Process Definition</label>
                        <span>${task.processDefinitionId || 'N/A'}</span>
                    </div>
                </div>
                
                ${task.description ? `
                    <div class="case-detail" style="margin-top: 1rem;">
                        <label>Description</label>
                        <div style="background: #f8f9fa; padding: 1rem; border-radius: 5px; margin-top: 0.5rem;">
                            ${task.description}
                        </div>
                    </div>
                ` : ''}
            </div>
        `;
        
        document.getElementById('modal-title').textContent = `Task Details - ${task.name}`;
        document.getElementById('case-modal').style.display = 'flex';
        
    } catch (error) {
        console.error('Error loading task details:', error);
        showAlert(`Error loading task details: ${error.message}`);
    }
}

// Approval Functions
async function loadApprovals() {
    try {
        // Use the new approval tasks endpoint
        await loadApprovalTasks();
    } catch (error) {
        console.error('Error loading approvals:', error);
        document.getElementById('approvals-container').innerHTML = `
            <div class="alert alert-error">
                <i class="fas fa-exclamation-circle"></i>
                Error loading approvals: ${error.message}
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
        const response = await apiRequest(`/cases/${caseId}?action=approve`, {
            method: 'PUT',
            body: JSON.stringify({
                approved: approved,
                comments: comments || '',
                updatedBy: currentUser
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

/**
 * Load approval tasks for supervisors (admin)
 */
async function loadApprovalTasks() {
    try {
        const approvalTasks = await apiRequest('/tasks/pending-approvals');
        displayApprovalTasks(approvalTasks);
    } catch (error) {
        console.error('Error loading approval tasks:', error);
        document.getElementById('approvals-container').innerHTML = `
            <div class="alert alert-error">
                <i class="fas fa-exclamation-circle"></i>
                Error loading approval tasks: ${error.message}
            </div>
        `;
    }
}

/**
 * Display approval tasks for supervisors
 */
function displayApprovalTasks(tasks) {
    const container = document.getElementById('approvals-container');
    
    if (tasks.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-check-circle"></i>
                <h3>No Pending Approvals</h3>
                <p>There are no cases waiting for approval.</p>
            </div>
        `;
        return;
    }
    
    container.innerHTML = tasks.map(task => `
        <div class="approval-card">
            <div class="approval-header">
                <h4>${task.taskName}</h4>
                <span class="task-status status-${task.status.toLowerCase()}">${task.status}</span>
            </div>
            <div class="approval-details">
                <div class="detail-item">
                    <label>Case ID:</label>
                    <span>${task.caseId}</span>
                </div>
                <div class="detail-item">
                    <label>Created:</label>
                    <span>${formatDate(task.createdAt)}</span>
                </div>
                <div class="detail-item">
                    <label>Description:</label>
                    <span>${task.description}</span>
                </div>
            </div>
            <div class="approval-actions">
                <button class="btn btn-success btn-sm" onclick="approveCase('${task.id}', '${task.caseId}', true)">
                    <i class="fas fa-check"></i> Approve
                </button>
                <button class="btn btn-danger btn-sm" onclick="approveCase('${task.id}', '${task.caseId}', false)">
                    <i class="fas fa-times"></i> Reject
                </button>
                <button class="btn btn-info btn-sm" onclick="viewCaseFromTask('${task.caseId}')">
                    <i class="fas fa-eye"></i> View Case
                </button>
            </div>
        </div>
    `).join('');
}

/**
 * Approve or reject a case
 */
async function approveCase(taskId, caseId, approved) {
    const action = approved ? 'approve' : 'reject';
    const comments = prompt(`Enter comments for ${action}ing this case:`);
    
    if (comments === null) return; // User cancelled
    
    try {
        const result = await apiRequest(`/tasks/${taskId}/approve-case`, {
            method: 'PUT',
            body: JSON.stringify({
                approved: approved,
                comments: comments,
                approvedBy: currentUser
            })
        });
        
        showSuccess(result.message);
        
        // Refresh approval tasks and dashboard
        await loadApprovalTasks();
        await refreshDashboard();
        
    } catch (error) {
        console.error('Error processing approval:', error);
        showAlert(`Error ${action}ing case: ${error.message}`);
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
    
    console.log('=== CREATE CASE DEBUG ===');
    console.log('Current user:', currentUser);
    console.log('Current user data:', currentUserData);
    
    if (!validateForm()) {
        console.log('Form validation failed');
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
    
    console.log('Request body:', requestBody);
    
    const editingCaseId = event.target.dataset.editingCaseId;
    const editMode = event.target.dataset.editMode;
    
    try {
        let result;
        let message;
        
        if (editingCaseId && editMode === 'update') {
            console.log('Updating case:', editingCaseId);
            // General case editing/updating
            result = await apiRequest(`/cases/${editingCaseId}?action=edit`, {
                method: 'PUT',
                body: JSON.stringify(requestBody)
            });
            message = `Case updated successfully! Case Number: ${result.caseNumber}`;
            
        } else if (editingCaseId) {
            console.log('Completing case:', editingCaseId);
            // User Story 2: Complete case creation
            result = await apiRequest(`/cases/${editingCaseId}?action=complete`, {
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
            console.log('Creating new case');
            // User Story 1: Create new case
            result = await apiRequest('/cases', {
                method: 'POST',
                body: JSON.stringify(requestBody)
            });
            
            console.log('Case creation result:', result);
            
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
        
        console.log('Success message:', message);
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
    form.removeAttribute('data-edit-mode');
    
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
 * Edit case details (general editing, not case completion)
 */
async function editCaseDetails(caseId) {
    try {
        const caseItem = await apiRequest(`/cases/${caseId}`);
        
        // Check permissions
        if (caseItem.createdBy !== currentUser && !hasPermission('VIEW_ALL_CASES')) {
            showAlert('You can only edit cases you created');
            return;
        }
        
        // Load case into form for editing
        populateFormForEdit(caseItem);
        showSection('create-case');
        
        // Set form to edit mode
        const headerElement = document.querySelector('#create-case .section-header h2');
        const submitButton = document.getElementById('submit-btn');
        const form = document.getElementById('case-form');
        
        if (headerElement) headerElement.innerHTML = '<i class="fas fa-edit"></i> Edit Case';
        if (submitButton) submitButton.innerHTML = '<i class="fas fa-save"></i> Update Case';
        if (form) {
            form.dataset.editingCaseId = caseId;
            form.dataset.editMode = 'update';
        }
        
        showSuccess('Case loaded for editing.');
        
    } catch (error) {
        console.error('Error loading case for edit:', error);
        showAlert(`Error loading case: ${error.message}`);
    }
}

/**
 * Delete a case with confirmation
 */
async function deleteCase(caseId, caseNumber) {
    if (!confirm(`Delete case ${caseNumber}? This cannot be undone.`)) return;
    
    try {
        await apiRequest(`/cases/${caseId}?deletedBy=${currentUser}`, { method: 'DELETE' });
        showSuccess(`Case ${caseNumber} deleted successfully!`);
        await loadCases();
    } catch (error) {
        console.error('Error deleting case:', error);
        showAlert(`Error deleting case: ${error.message}`);
    }
}

// Helper function to check if user can edit/delete a case
function canEditCase(caseItem) {
    return caseItem.status === 'DRAFT' || 
           caseItem.createdBy === currentUser || 
           hasPermission('VIEW_ALL_CASES');
}

/**
 * Populate form with existing case data for editing or completion
 */
function populateFormForEdit(caseItem) {
    console.log('Populating form with case data:', caseItem);
    
    try {
        // Basic fields
        document.getElementById('description').value = caseItem.description || '';
        document.getElementById('entity').value = caseItem.entity || '';
        document.getElementById('alertId').value = caseItem.alertId || '';
        
        // Dropdowns - only set if value exists and is valid
        const caseTypeSelect = document.getElementById('caseType');
        if (caseItem.caseType && caseTypeSelect) {
            caseTypeSelect.value = caseItem.caseType;
        }
        
        const prioritySelect = document.getElementById('priority');
        if (caseItem.priority && prioritySelect) {
            prioritySelect.value = caseItem.priority;
        }
        
        const typologySelect = document.getElementById('typology');
        if (caseItem.typology && typologySelect) {
            typologySelect.value = caseItem.typology;
        }
        
        // Risk score
        const riskScoreInput = document.getElementById('riskScore');
        if (caseItem.riskScore && riskScoreInput) {
            riskScoreInput.value = caseItem.riskScore;
        }
        
        console.log('Form populated successfully');
        
    } catch (error) {
        console.error('Error populating form:', error);
        showAlert('Error loading case data into form: ' + error.message);
    }
}

// Test function to verify UI is working
function testUI() {
    console.log('Testing UI...');
    alert('UI is responding to clicks!');
    return true;
}

// Add global test function
window.testUI = testUI;

// Initialize the application when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM Content Loaded - Initializing app...');
    
    try {
        // Initialize role-based UI
        updateUIBasedOnRole();
        
        // Load initial data
        refreshDashboard();
        
        // Set up navigation event listeners
        document.querySelectorAll('.nav-btn').forEach(btn => {
            btn.addEventListener('click', function() {
                const section = this.dataset.section;
                showSection(section);
            });
        });
        
        console.log('App initialized successfully');
        
    } catch (error) {
        console.error('Error initializing app:', error);
        alert('Error initializing application: ' + error.message);
    }
});

// Make functions available globally for debugging
window.loadCases = loadCases;
window.refreshDashboard = refreshDashboard;
window.showSection = showSection;

function updateRoleHelp() {
    // Update help text based on current user role
    const userRole = currentUserData.role;
    
    // You can add role-specific help content here if needed
    // For now, just make it a no-op function to prevent the error
    console.log(`Help updated for role: ${userRole}`);
}