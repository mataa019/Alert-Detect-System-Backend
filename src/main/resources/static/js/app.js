// API Configuration
const API_BASE_URL = 'http://localhost:8080/api';

// Global state
let currentMode = 'draft';
let allCases = [];
let currentUser = 'admin1'; // Default user set to a valid user

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
    admin1: {
        id: 'admin1',
        name: 'Sarah Admin',
        role: 'admin',
        display: 'Sarah Admin (Admin)'
    },
    analyst1: {
        id: 'analyst1',
        name: 'Jane Analyst',
        role: 'analyst',
        display: 'Jane Analyst (Analyst)'
    }
};

let currentUserData = USERS[currentUser];

// Role-based permissions
function hasPermission(action) {
    const userRole = (currentUserData.role || '').toUpperCase();
    switch (action) {
        case 'CREATE_CASE':
            return [USER_ROLES.ANALYST, USER_ROLES.ADMIN].includes(userRole);
        case 'APPROVE_CASE':
            return userRole === USER_ROLES.ADMIN;
        case 'ASSIGN_TASK':
            return userRole === USER_ROLES.ADMIN;
        case 'VIEW_ALL_CASES':
            return [USER_ROLES.ADMIN].includes(userRole);
        case 'COMPLETE_TASK':
            return [USER_ROLES.ANALYST, USER_ROLES.ADMIN].includes(userRole);
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
    const userRole = (currentUserData.role || '').toUpperCase();
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
    // Show/hide approvals section
    const approvalsSection = document.getElementById('approvals');
    if (approvalsSection) {
        approvalsSection.style.display = hasPermission('APPROVE_CASE') ? '' : 'none';
    }
    // Privilege message
    updatePrivilegeMessage(currentUserData);
    // Handle task assignee input visibility
    const assigneeGroup = document.getElementById('assignee-group');
    const assigneeInput = document.getElementById('assignee-input');
    if (assigneeGroup && assigneeInput) {
        if (hasPermission('VIEW_ALL_CASES')) {
            assigneeGroup.style.display = 'flex';
            assigneeInput.value = currentUser;
            assigneeInput.placeholder = 'Enter username to view their tasks';
        } else {
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

function getRiskLevel(riskScore) {
    if (!riskScore) return 'unknown';
    if (riskScore >= 80) return 'high';
    if (riskScore >= 50) return 'medium';
    return 'low';
}

// Utility to add cache-busting param to URLs
function cacheBust(url) {
    const sep = url.includes('?') ? '&' : '?';
    return url + sep + '_=' + Date.now();
}

// API Functions
async function apiRequest(endpoint, options = {}) {
    let url = `${API_BASE_URL}${endpoint}`;
    // Add cache busting for GET requests
    const method = (options.method || 'GET').toUpperCase();
    if (method === 'GET') {
        url = cacheBust(url);
    }
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
        // Map to case objects for stats
        const caseObjs = cases.map(item => item.case || item);
        let filteredCases = caseObjs;
        if (currentUserData.role === USER_ROLES.ANALYST) {
            filteredCases = caseObjs.filter(c => c.createdBy === currentUser);
        }
        // Update stats
        document.getElementById('total-cases').textContent = filteredCases.length;
        document.getElementById('draft-cases').textContent = 
            filteredCases.filter(c => c.status === 'DRAFT').length;
        document.getElementById('pending-cases').textContent = 
            filteredCases.filter(c => c.status === 'PENDING_CASE_CREATION_APPROVAL').length;
        // Add rejected cases stat if you want to display it
        const rejectedStat = document.getElementById('rejected-cases');
        if (rejectedStat) {
            rejectedStat.textContent = filteredCases.filter(c => c.status === 'REJECTED').length;
        }
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
        const rejectedStat = document.getElementById('rejected-cases');
        if (rejectedStat) rejectedStat.textContent = '0';
        document.getElementById('my-tasks').textContent = '0';
    }
}

async function loadRecentCases() {
    try {
        const cases = await apiRequest('/cases');
        // Map to case objects for display
        const caseObjs = cases.map(item => item.case || item);
        let filteredCases = caseObjs;
        if (currentUserData.role === USER_ROLES.ANALYST) {
            filteredCases = caseObjs.filter(c => c.createdBy === currentUser);
        }
        const recentCases = filteredCases.slice(0, 5); // Get last 5 cases for role
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

// Cases Functions
async function loadCases() {
    try {
        const cases = await apiRequest('/cases');
        // Map to case objects for filtering and display
        let filteredCases = cases.map(item => item.case || item);
        if (currentUserData.role === USER_ROLES.ANALYST) {
            filteredCases = filteredCases.filter(c => c.createdBy === currentUser);
        }
        allCases = filteredCases;
        displayCases(filteredCases);
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
    if (!cases || cases.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-folder-open"></i>
                <h3>No Cases Found</h3>
                <p>No cases match your current filter criteria.</p>
            </div>
        `;
        return;
    }
    container.innerHTML = cases.map(caseItem => {
        if (!caseItem) return '';
        const isDraft = caseItem.status === 'DRAFT';
        const isOwner = caseItem.createdBy === currentUser;
        return `
        <div class="case-card" onclick="showCaseDetails('${caseItem.id}')">
            <div class="case-header">
                <div class="case-number">${caseItem.caseNumber}</div>
                <div class="case-status status-${(caseItem.status||'').toLowerCase()}">${formatStatus(caseItem.status||'')}</div>
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
                ${isDraft ? 
                    `<button class="btn btn-warning btn-sm" onclick="event.stopPropagation(); editCase('${caseItem.id}')">
                        <i class="fas fa-edit"></i> Complete Case
                    </button>
                    ${isOwner ? `<button class="btn btn-danger btn-sm" onclick="event.stopPropagation(); abandonCase('${caseItem.id}')"><i class='fas fa-trash'></i> Abandon Case</button>` : ''}` : ''
                }
                <button class="btn btn-info btn-sm" onclick="event.stopPropagation(); showCaseDetails('${caseItem.id}')">
                    <i class="fas fa-eye"></i> View Details
                </button>
            </div>
        </div>
    `}).join('');
}

function filterCases() {
    const statusFilter = document.getElementById('status-filter').value;
    let filteredCases = allCases;
    if (statusFilter) {
        filteredCases = filteredCases.filter(caseItem => caseItem.status === statusFilter);
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
        // Use the correct endpoint for pending approvals
        const pendingCases = await apiRequest('/cases?pendingApproval=true');
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
    container.innerHTML = cases.map((item, idx) => {
        const caseItem = item.case;
        const taskId = item.taskId;
        const showActions = hasPermission('APPROVE_CASE') && caseItem.status === 'PENDING_CASE_CREATION_APPROVAL' && taskId;
        return `
        <div class="case-card approval-card" id="approval-card-${idx}">
            <div class="case-header">
                <div class="case-number">${caseItem.caseNumber}</div>
                <div class="case-status status-${caseItem.status.toLowerCase()}">${formatStatus(caseItem.status)}</div>
            </div>
            <div class="case-details">
                <div class="case-detail"><label>Type</label><span>${caseItem.caseType || 'N/A'}</span></div>
                <div class="case-detail"><label>Priority</label><span class="priority-${caseItem.priority?.toLowerCase() || 'normal'}">${caseItem.priority || 'N/A'}</span></div>
                <div class="case-detail"><label>Risk Score</label><span class="risk-score-${getRiskLevel(caseItem.riskScore)}">${caseItem.riskScore || 'N/A'}</span></div>
                <div class="case-detail"><label>Created By</label><span>${caseItem.createdBy}</span></div>
                <div class="case-detail"><label>Entity</label><span>${caseItem.entity || 'N/A'}</span></div>
                <div class="case-detail"><label>Alert ID</label><span>${caseItem.alertId || 'N/A'}</span></div>
            </div>
            <div class="case-description"><strong>Description:</strong><p>${caseItem.description || 'No description'}</p></div>
            <div class="approval-actions">
                ${showActions ? `
                    <button class="btn btn-success btn-sm" onclick="showInlineApprovalForm(${idx}, '${taskId}', '${caseItem.id}')">
                        <i class="fas fa-check"></i> Review & Approve/Reject
                    </button>
                ` : `
                    <div class="permission-notice">
                        <i class="fas fa-info-circle"></i>
                        You need admin privileges to approve cases
                    </div>
                `}
                <button class="btn btn-info btn-sm" onclick="showCaseDetails('${caseItem.id}')">
                    <i class="fas fa-eye"></i> View Details
                </button>
            </div>
            <div class="inline-approval-form" id="inline-approval-form-${idx}" style="display:none; margin-top:1rem; background:#f8f9fa; padding:1rem; border-radius:6px;">
                <label for="inline-approval-comments-${idx}">Comments (required):</label>
                <textarea id="inline-approval-comments-${idx}" rows="3" style="width:100%; margin-bottom:0.5rem;"></textarea>
                <div style="display:flex; gap:1rem; margin-top:0.5rem;">
                    <button class="btn btn-success" onclick="submitInlineApproval(${idx}, '${taskId}', '${caseItem.id}', true)"><i class='fas fa-check'></i> Approve</button>
                    <button class="btn btn-danger" onclick="submitInlineApproval(${idx}, '${taskId}', '${caseItem.id}', false)"><i class='fas fa-times'></i> Reject</button>
                    <button class="btn btn-secondary" onclick="hideInlineApprovalForm(${idx})">Cancel</button>
                </div>
            </div>
        </div>
    `}).join('');
}

window.showInlineApprovalForm = function(idx, taskId, caseId) {
    document.getElementById(`inline-approval-form-${idx}`).style.display = 'block';
};
window.hideInlineApprovalForm = function(idx) {
    document.getElementById(`inline-approval-form-${idx}`).style.display = 'none';
};
window.submitInlineApproval = async function(idx, taskId, caseId, approve) {
    const comments = document.getElementById(`inline-approval-comments-${idx}`).value.trim();
    if (!comments) {
        showAlert('Comments are required for both approval and rejection. Please provide your comments.');
        return;
    }
    try {
        await handleApprovalTask(taskId, caseId, approve, comments);
        // Show status message in the card instead of removing it
        const card = document.getElementById(`approval-card-${idx}`);
        if (card) {
            card.querySelector('.inline-approval-form').style.display = 'none';
            const statusMsg = document.createElement('div');
            statusMsg.className = 'approval-status-msg';
            statusMsg.style.margin = '1rem 0';
            statusMsg.style.fontWeight = 'bold';
            statusMsg.style.color = approve ? '#388e3c' : '#b71c1c';
            statusMsg.innerHTML = approve
                ? "<i class='fas fa-check-circle'></i> Case Approved"
                : "<i class='fas fa-times-circle'></i> Case Rejected";
            card.appendChild(statusMsg);
            // Optionally, fade out the card after a delay
            setTimeout(() => {
                card.style.transition = 'opacity 0.5s';
                card.style.opacity = 0;
                setTimeout(() => card.remove(), 600);
            }, 1500);
        }
        // Optionally, reload dashboard stats
        if (document.getElementById('dashboard').classList.contains('active')) {
            loadDashboard();
        }
        // If no more pending approvals, show empty state
        const container = document.getElementById('pending-approvals');
        setTimeout(() => {
            if (!container.querySelector('.case-card')) {
                container.innerHTML = `
                    <div class="empty-state">
                        <i class="fas fa-check-circle"></i>
                        <h3>No Pending Approvals</h3>
                        <p>All cases have been reviewed and approved.</p>
                    </div>
                `;
            }
        }, 1700);
    } catch (err) {
        showAlert('Error submitting approval: ' + (err.message || err));
    }
};

// Supervisor Approval Workflow
async function handleApprovalTask(taskId, caseId, approve, comments = '') {
    try {
        // Fetch case and task details
        const [caseItem, taskItem] = await Promise.all([
            apiRequest(`/cases/${caseId}`),
            apiRequest(`/tasks/${taskId}`)
        ]);
        // Check case status
        if (caseItem.status !== 'PENDING_CASE_CREATION_APPROVAL') {
            showAlert('Case is not pending approval. Approval/rejection not allowed.');
            return;
        }
        // Check if current user is the assignee; if not, claim the task
        if (!taskItem.assignee || taskItem.assignee !== currentUser) {
            if (confirm('You must claim this approval task before acting. Claim now?')) {
                await apiRequest(`/task/assign/${taskId}`, {
                    method: 'PUT',
                    body: JSON.stringify({ assignee: currentUser, updatedBy: currentUser })
                });
                showSuccess('Task claimed successfully. You can now approve or reject.');
            } else {
                showAlert('You must claim the task to proceed.');
                return;
            }
        }
        // Update the approval task status
        await apiRequest(`/task/update/${taskId}`, {
            method: 'PUT',
            body: JSON.stringify({
                action: approve ? 'APPROVE_CASE_CREATION' : 'REJECT_CASE_CREATION',
                updatedBy: currentUser,
                comments: comments
            })
        });
        if (approve) {
            // Create Investigate Case task
            await apiRequest(`/task/create/${caseId}`, {
                method: 'POST',
                body: JSON.stringify({
                    type: 'INVESTIGATE_CASE',
                    assignedGroup: 'Investigations',
                    createdBy: currentUser
                })
            });
            showSuccess('Case approved and investigation task created.');
        } else {
            showSuccess('Case rejected.');
        }
        // Refresh approvals and dashboard
        loadApprovals();
        if (document.getElementById('dashboard').classList.contains('active')) {
            loadDashboard();
        }
    } catch (error) {
        if (error.message && error.message.includes('403')) {
            showAlert('Unauthorized: You do not have permission to approve/reject this case.');
        } else {
            showAlert(`Error processing approval: ${error.message}`);
        }
        console.error('Error processing approval task:', error);
    }
}

// Update privilege message logic to only show for non-admins
function updatePrivilegeMessage(user) {
    const msg = document.getElementById('supervisor-approval-message');
    if (!msg) return;
    const role = (user && user.role) ? user.role.toUpperCase() : '';
    if (role !== 'ADMIN') {
        msg.style.display = 'flex';
    } else {
        msg.style.display = 'none';
    }
}

// Event Listeners
document.addEventListener('DOMContentLoaded', function() {
    // Navigation click handlers
    document.querySelectorAll('.nav-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const section = btn.getAttribute('data-section');
            if (section) {
                showSection(section);
            }
        });
    });
    // Initial UI setup
    updateUIBasedOnRole();
    refreshAllData();
});

// Add user dropdown event handler to avoid CSP issues
// This must be at the end of the file so switchUser is defined

document.addEventListener('DOMContentLoaded', function() {
    var userSelect = document.getElementById('user-select');
    if (userSelect) {
        userSelect.addEventListener('change', function(e) {
            console.log('Dropdown changed to:', e.target.value);
            if (typeof switchUser === 'function') {
                switchUser(e.target.value);
            }
        });
    }
});

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