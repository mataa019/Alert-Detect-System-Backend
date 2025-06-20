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
    
    try {
        const tasks = await apiRequest(`/tasks/my/${assignee}`);
        displayTasks(tasks);
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

function displayTasks(tasks) {
    const container = document.getElementById('tasks-container');
    
    if (tasks.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-tasks"></i>
                <h3>No Tasks Found</h3>
                <p>No tasks are currently assigned to this user.</p>
            </div>
        `;
        return;
    }
    
    container.innerHTML = tasks.map(task => `
        <div class="task-card">
            <div class="task-header">
                <div class="task-name">${task.name}</div>
            </div>
            <div class="task-details">
                <div class="case-detail">
                    <label>Task ID</label>
                    <span>${task.id}</span>
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
            </div>
            <div class="task-actions">
                ${task.name === 'Complete Case Creation' ? 
                    `<button class="btn btn-success btn-sm" onclick="completeTask('${task.id}')">
                        <i class="fas fa-check"></i> Complete Task
                    </button>` : ''
                }
                <button class="btn btn-info btn-sm" onclick="showTaskDetails('${task.id}')">
                    <i class="fas fa-eye"></i> View Details
                </button>
            </div>
        </div>
    `).join('');
}

async function completeTask(taskId) {
    if (!confirm('Are you sure you want to complete this task?')) {
        return;
    }
    
    try {
        await apiRequest(`/task/update/${taskId}`, {
            method: 'PUT',
            body: JSON.stringify({
                updatedBy: currentUser,
                action: 'COMPLETE_CASE_CREATION'
            })
        });
        
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
    
    try {
        const result = await apiRequest('/cases/create', {
            method: 'POST',
            body: JSON.stringify(requestBody)
        });
        
        let message = '';
        if (result.status === 'DRAFT') {
            message = `Draft case created successfully! Case Number: ${result.caseNumber}`;
        } else if (result.status === 'READY_FOR_ASSIGNMENT') {
            message = `Complete case created and workflow started! Case Number: ${result.caseNumber}`;
        } else {
            message = `Case created successfully! Case Number: ${result.caseNumber}`;
        }
        
        showSuccess(message);
        resetForm();
        
        // If we're on dashboard, refresh it
        if (document.getElementById('dashboard').classList.contains('active')) {
            loadDashboard();
        }
        
    } catch (error) {
        console.error('Error creating case:', error);
        showAlert(`Error creating case: ${error.message}`);
    }
}

function resetForm() {
    document.getElementById('case-form').reset();
    clearFormErrors();
    clearAlerts();
    
    // Reset mode to draft
    document.querySelector('input[name="mode"][value="draft"]').checked = true;
    toggleMode();
}

// Event Listeners
document.addEventListener('DOMContentLoaded', function() {
    // Navigation event listeners
    document.querySelectorAll('.nav-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            showSection(btn.dataset.section);
        });
    });
    
    // Modal close event listeners
    document.getElementById('case-modal').addEventListener('click', function(e) {
        if (e.target === this) {
            closeModal();
        }
    });
    
    // Form input event listeners to clear errors
    document.querySelectorAll('#case-form input, #case-form select, #case-form textarea').forEach(input => {
        input.addEventListener('input', function() {
            const formGroup = this.closest('.form-group');
            if (formGroup.classList.contains('error')) {
                formGroup.classList.remove('error');
                formGroup.querySelector('.error-message').textContent = '';
            }
        });
    });
    
    // Initial load
    loadDashboard();
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
