# 🚨 Alert Detection System

A Spring Boot + Flowable BPMN-based financial crime case management system with automated workflow processing and REST API endpoints.

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Installation & Setup](#installation--setup)
- [API Documentation](#api-documentation)
- [Database Schema](#database-schema)
- [Workflow Process](#workflow-process)
- [Frontend Integration](#frontend-integration)
- [Testing](#testing)
- [Contributing](#contributing)

## 🎯 Overview

The Alert Detection System is designed for financial institutions to manage and investigate suspicious activities, money laundering cases, and fraud detection. It provides automated workflow management using Flowable BPMN engine with comprehensive case and task management capabilities.

## ✨ Features

### Core Features
- 📝 **Manual Case Creation** - Create cases with validation and unique case number generation
- 🔄 **Automated Workflow** - BPMN-based case investigation process
- 👥 **Task Management** - Assignment, completion, and tracking of investigation tasks
- 📊 **Status Management** - Track cases through different investigation stages
- 🔍 **Audit Logging** - Complete audit trail for all case actions
- 🌐 **REST API** - RESTful endpoints for frontend integration
- 🔗 **CORS Support** - Cross-origin resource sharing for web applications

### Business Features
- 🎯 **Risk Scoring** - Automated risk assessment for cases
- 📈 **Priority Management** - High, Medium, Low priority classification
- 👤 **Customer Management** - Customer details integration
- 📋 **Case Types** - Support for various financial crime types
- ⚡ **Real-time Updates** - Live status updates and notifications

## 🛠 Tech Stack

### Backend
- **Java 17** - Programming language
- **Spring Boot 3.5.0** - Application framework
- **Flowable 6.8.0** - BPMN workflow engine
- **PostgreSQL** - Primary database
- **Spring Data JPA** - Data persistence
- **Maven** - Dependency management

### Frontend (Optional)
- **React** - UI framework
- **Vite** - Build tool
- **Axios** - HTTP client

## 📋 Prerequisites

Before running this application, ensure you have:

- ☑️ **Java 17** or higher
- ☑️ **Maven 3.6+**
- ☑️ **PostgreSQL 12+**
- ☑️ **IntelliJ IDEA** (recommended) or any Java IDE
- ☑️ **Node.js 16+** (for frontend)
- ☑️ **Git**

## 🚀 Installation & Setup

### 1. Clone Repository
```bash
git clone <repository-url>
cd "Alert detect system"
```

### 2. Database Setup
```sql
-- Connect to PostgreSQL and create database
CREATE DATABASE alert_detect_db;

-- Grant permissions (if needed)
GRANT ALL PRIVILEGES ON DATABASE alert_detect_db TO postgres;
```

### 3. Configure Application
Update `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/alert_detect_db
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 4. Build & Run
```bash
# Using Maven
./mvnw clean install
./mvnw spring-boot:run

# Using IntelliJ IDEA
# Right-click on AlertDetectSystemApplication.java -> Run
```

### 5. Verify Installation
- Backend: http://localhost:8080/api/cases/test
- H2 Console (if using H2): http://localhost:8080/h2-console

## 📚 API Documentation

### Base URL
```
http://localhost:8080/api
```

### 📋 Case Management Endpoints

#### Create New Case
```http
POST /api/cases/create
Content-Type: application/json

{
  "caseType": "FRAUD_DETECTION",
  "priority": "HIGH",
  "description": "Suspicious transaction detected",
  "riskScore": 85.5,
  "customerDetails": {
    "customerId": "CUST001",
    "customerName": "John Doe"
  }
}
```

#### Get All Cases
```http
GET /api/cases
```

#### Get Case by ID
```http
GET /api/cases/{caseId}
```

#### Update Case Status
```http
PUT /api/cases/{caseId}/status
Content-Type: application/json

{
  "status": "UNDER_INVESTIGATION",
  "comment": "Starting investigation"
}
```

#### Get Cases by Status
```http
GET /api/cases/by-status/{status}
```

**Available Status Values:**
- `DRAFT`
- `READY_FOR_ASSIGNMENT`
- `UNDER_INVESTIGATION`
- `PENDING_APPROVAL`
- `CLOSED`
- `REJECTED`

### 📝 Task Management Endpoints

#### Get My Tasks
```http
GET /api/tasks/my/{assignee}
```

#### Get Group Tasks
```http
GET /api/tasks/group/{groupId}
```

#### Get Task by ID
```http
GET /api/tasks/{taskId}
```

#### Get Tasks by Case
```http
GET /api/tasks/by-case/{caseId}
```

#### Assign Task
```http
POST /api/tasks/assign
Content-Type: application/json

{
  "taskId": "task-123",
  "assignee": "john.doe",
  "comment": "Assigning to senior investigator"
}
```

#### Complete Task
```http
POST /api/tasks/complete
Content-Type: application/json

{
  "taskId": "task-123",
  "variables": {
    "decision": "APPROVED",
    "comments": "Investigation completed"
  }
}
```

#### Create Task
```http
POST /api/tasks/create
Content-Type: application/json

{
  "taskName": "Review Documents",
  "assignee": "john.doe",
  "status": "PENDING",
  "caseId": "123e4567-e89b-12d3-a456-426614174000"
}
```

## 🗄 Database Schema

### Core Tables
- **`case_model`** - Main case information
- **`task_model`** - Task records
- **`audit_log_model`** - Audit trail
- **Flowable Tables** - Workflow engine tables (auto-created)

### Key Entities
```java
CaseModel {
  UUID id;
  String caseNumber;
  String caseType;
  String priority;
  CaseStatus status;
  String description;
  Double riskScore;
  String createdBy;
  LocalDateTime createdDate;
  // ... more fields
}

TaskModel {
  UUID id;
  String taskName;
  String assignee;
  String status;
  UUID caseId;
  LocalDateTime createdDate;
  // ... more fields
}
```

## 🔄 Workflow Process

The system uses Flowable BPMN for automated case processing:

1. **Case Creation** → Automatic workflow start
2. **Initial Review** → Task assignment
3. **Investigation** → Evidence gathering
4. **Approval Process** → Management review
5. **Case Closure** → Final documentation

BPMN file location: `src/main/resources/case-process.bpmn20.xml`

## 🌐 Frontend Integration

### CORS Configuration
The backend supports CORS for:
- `http://localhost:5173` (Vite dev server)
- `http://localhost:5174` (Alternative port)

### Sample Frontend Code
```javascript
// Fetch all cases
const fetchCases = async () => {
  const response = await fetch('http://localhost:8080/api/cases');
  const cases = await response.json();
  return cases;
};

// Create new case
const createCase = async (caseData) => {
  const response = await fetch('http://localhost:8080/api/cases/create', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(caseData)
  });
  return response.json();
};
```

## 🧪 Testing

### Backend Testing
```bash
# Run all tests
./mvnw test

# Test specific endpoint
curl http://localhost:8080/api/cases/test
```

### Sample Test Data
```bash
# Create test case
curl -X POST http://localhost:8080/api/cases/create \
  -H "Content-Type: application/json" \
  -d '{
    "caseType": "MONEY_LAUNDERING",
    "priority": "HIGH",
    "description": "Test case for API testing",
    "riskScore": 75.0
  }'
```

## 🔧 Configuration

### Application Properties
```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/alert_detect_db
spring.datasource.username=postgres
spring.datasource.password=Admin

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Flowable
flowable.database-schema-update=true
flowable.async-executor-activate=true
flowable.check-process-definitions=true
```

### Development vs Production
- **Development**: Use H2 in-memory database for quick testing
- **Production**: Use PostgreSQL with proper security configurations

## 📁 Project Structure

```
Alert detect system/
├── src/
│   ├── main/
│   │   ├── java/com/example/alert_detect_system/
│   │   │   ├── controller/          # REST controllers
│   │   │   ├── service/             # Business logic
│   │   │   ├── Model/               # Entity models
│   │   │   ├── repo/                # Data repositories
│   │   │   ├── dto/                 # Data transfer objects
│   │   │   ├── config/              # Configuration classes
│   │   │   ├── workflow/            # Flowable workflow services
│   │   │   └── AlertDetectSystemApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-test.properties
│   │       └── case-process.bpmn20.xml
│   └── test/                        # Unit tests
├── target/                          # Build output
├── pom.xml                         # Maven dependencies
└── README.md                       # This file
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/new-feature`)
3. Commit changes (`git commit -am 'Add new feature'`)
4. Push to branch (`git push origin feature/new-feature`)
5. Create a Pull Request

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 📞 Support

For support and questions:
- Create an issue in the repository
- Email: support@alertdetection.com
- Documentation: [Project Wiki](wiki-link)

## 🚧 Roadmap

### Phase 1 (Current)
- ✅ Basic case management
- ✅ Workflow automation
- ✅ REST API endpoints

### Phase 2 (Planned)
- 🔲 User authentication & authorization
- 🔲 Advanced reporting & analytics
- 🔲 Email notifications
- 🔲 File attachment support

### Phase 3 (Future)
- 🔲 Machine learning integration
- 🔲 Real-time dashboards
- 🔲 Mobile application
- 🔲 Advanced workflow designer

---

**Built with ❤️ for financial crime prevention**
