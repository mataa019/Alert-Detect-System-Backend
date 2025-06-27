# Alert Detection System

A Spring Boot + Flowable case management system with MySQL backend and a modern JavaScript frontend for managing, reviewing, and approving alert cases.

## Features
- Case creation, editing, and abandonment (analyst and admin)
- Admin-only approval/rejection workflow
- Role-based UI (admin/analyst)
- Inline approval forms and robust dashboard
- Audit logging for all case actions
- RESTful API for integration and automation

## User Roles
- **Admin (`admin1`)**: Can approve/reject cases, see all cases, manage tasks, and view audit logs.
- **Analyst (`analyst1`)**: Can create, edit, and abandon own cases, see own cases and tasks, but cannot approve/reject.

## API Endpoints

### Case Management
- `POST   /api/cases` — Create a new case
- `GET    /api/cases` — List/filter cases (by status, creator, or pending approval)
- `GET    /api/cases/{caseId}` — Get case details
- `PUT    /api/cases/{caseId}?action=complete|approve|update|status` — Update, approve/reject, or complete a case
- `DELETE /api/cases/{caseId}` — Delete a case
- `PUT    /api/cases/abandon/{caseId}` — Abandon a draft case
- `GET    /api/cases/recent` — Get recent cases for dashboard
- `GET    /api/cases/{caseId}/audit` — Get audit logs for a case

### Task Management
- `GET    /api/tasks/my/{userId}` — Get Flowable tasks for user
- `GET    /api/tasks/by-assignee/{userId}` — Get DB tasks for user
- `PUT    /api/tasks/complete/{taskId}` — Complete a Flowable task
- `PUT    /api/task/assign/{taskId}` — Assign a task
- `PUT    /api/task/update/{taskId}` — Update a task
- `POST   /api/task/create/{caseId}` — Create a new task for a case

## Running the System
1. **Backend:**
   - Configure MySQL in `src/main/resources/application.properties`.
   - Run the Spring Boot application (`mvnw spring-boot:run` or from your IDE).
2. **Frontend:**
   - Open `src/main/resources/static/index.html` in your browser.
   - All JS and CSS are included in the static resources.

## Testing with Postman
- Use the API endpoints above for all case and task operations.
- For admin actions (approval/rejection), set `updatedBy` to `admin1` in your request body.
- For analyst actions, use `analyst1`.

## Example: Approve a Case (Admin)
```
PUT /api/cases/{caseId}?action=approve
Body:
{
  "updatedBy": "admin1",
  "approved": true,
  "comments": "Looks good."
}
```

## Example: Reject a Case (Admin)
```
PUT /api/cases/{caseId}?action=approve
Body:
{
  "updatedBy": "admin1",
  "approved": false,
  "comments": "Insufficient info."
}
```

## Example: Abandon a Draft (Analyst)
```
PUT /api/cases/abandon/{caseId}
Body:
{
  "abandonedBy": "analyst1",
  "reason": "No longer needed."
}
```

## Key API Flows

### 1. Create Case (Draft & Complete)
**API:** `POST /api/cases` (Draft)
**User:** analyst1 or admin1
**Body:**
```json
{
  "caseType": "FRAUD_DETECTION",
  "priority": "HIGH",
  "description": "Suspicious transaction detected",
  "entity": "Customer123",
  "alertId": "ALERT-001",
  "typology": "MONEY_LAUNDERING",
  "riskScore": 85,
  "createdBy": "analyst1"
}
```
- This creates a new case in `DRAFT` status.

**API:** `PUT /api/cases/{caseId}?action=complete` (Complete Draft)
**User:** analyst1 or admin1
**Body:**
```json
{
  "updatedBy": "analyst1",
  "caseType": "FRAUD_DETECTION",
  "priority": "HIGH",
  "description": "All details provided",
  "entity": "Customer123",
  "alertId": "ALERT-001",
  "typology": "MONEY_LAUNDERING",
  "riskScore": 85
}
```
- This moves the case from `DRAFT` to `PENDING_CASE_CREATION_APPROVAL`.

### 2. Approve Case (Admin only)
**API:** `PUT /api/cases/{caseId}?action=approve`
**User:** admin1
**Body:**
```json
{
  "updatedBy": "admin1",
  "approved": true,
  "comments": "Case is valid and approved."
}
```
- This moves the case to `READY_FOR_ASSIGNMENT`.

### 3. Reject Case (Admin only)
**API:** `PUT /api/cases/{caseId}?action=approve`
**User:** admin1
**Body:**
```json
{
  "updatedBy": "admin1",
  "approved": false,
  "comments": "Insufficient information. Please revise."
}
```
- This moves the case to `REJECTED`.

### 4. Abandon Draft Case (Analyst only)
**API:** `PUT /api/cases/abandon/{caseId}`
**User:** analyst1
**Body:**
```json
{
  "abandonedBy": "analyst1",
  "reason": "No longer needed"
}
```
- This abandons a case in `DRAFT` status.

### 5. Assign or Unassign Task (Admin only)
**API:** `PUT /api/tasks/assign/{taskId}`
**User:** admin1
**Assign Body:**
```json
{
  "assignee": "admin1",
  "performedBy": "admin1"
}
```
**Unassign Body:**
```json
{
  "assignee": null,
  "performedBy": "admin1"
}
```
- To unassign, set `assignee` to `null` or an empty string. This will set the task status to `UNASSIGNED`, remove ownership, and return the task to the candidate group/work queue. Logs and audit trail are updated automatically.

### 6. Create Task (Admin only)
**API:** `POST /api/task/create/{caseId}`
**User:** admin1
**Body:**
```json
{
  "type": "INVESTIGATE_CASE",
  "assignedGroup": "Investigations",
  "createdBy": "admin1",
  "assignee": "admin1"
}
```
- The `assignee` field is optional. If provided, the task is assigned to that user. If omitted or set to `null`, the task will be unassigned and available in the candidate group/work queue.

## License
MIT
