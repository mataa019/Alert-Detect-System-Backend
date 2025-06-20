# Alert Detection System - UI

A simple HTML/CSS/JavaScript frontend for the Alert Detection System.

## 📁 Structure

```
ui/
├── index.html          # Main HTML file
├── css/
│   └── styles.css      # All styling
├── js/
│   └── app.js          # All JavaScript functionality
└── README.md           # This file
```

## 🚀 How to Run

### 1. **Start Your Backend**
Make sure your Spring Boot application is running on `http://localhost:8080`

### 2. **Open the UI**
Simply open `index.html` in your web browser:
- Double-click the `index.html` file, or
- Right-click → "Open with" → Your preferred browser

### 3. **Test Connection**
- Open browser console (F12)
- Run: `testConnection()`
- Should show "Backend connection successful!"

## 🎯 Features

### ✅ **Dashboard**
- View case statistics
- Recent cases overview
- Quick navigation

### ✅ **Case Management**
- View all cases
- Filter by status
- Create new cases (Draft or Complete)
- View case details
- Complete draft cases

### ✅ **Task Management**
- View assigned tasks
- Complete "Complete Case Creation" tasks
- Task details view

### ✅ **Case Creation**
- **Draft Mode**: Minimal information required
- **Complete Mode**: Full case details with validation

## 🔧 Configuration

### **Backend URL**
If your backend runs on a different port, update the `API_BASE_URL` in `js/app.js`:

```javascript
const API_BASE_URL = 'http://localhost:YOUR_PORT/api';
```

### **Default User**
Change the default user in `js/app.js`:

```javascript
let currentUser = 'your-username';
```

## 📱 **Responsive Design**
- Works on desktop, tablet, and mobile
- Modern, clean interface
- Font Awesome icons

## 🎨 **UI Components**

### **Navigation**
- Dashboard - Overview and statistics
- Cases - All case management
- Tasks - Task management
- Create Case - New case creation

### **Forms**
- Real-time validation
- Error handling
- Loading states
- Success messages

### **Cards**
- Case cards with status badges
- Task cards with actions
- Clickable for details

## 🐛 **Troubleshooting**

### **CORS Issues**
If you get CORS errors:
1. Make sure your backend has CORS configured
2. Check that WebConfig.java includes your frontend URL
3. Restart your Spring Boot application

### **Connection Failed**
1. Verify backend is running on `http://localhost:8080`
2. Test endpoint manually: `http://localhost:8080/api/cases/test`
3. Check browser console for detailed errors

### **Cases Not Loading**
1. Check if you have cases in your database
2. Test API endpoint: `http://localhost:8080/api/cases`
3. Look for errors in browser console

## 🔄 **Development**

### **Adding New Features**
1. Add HTML structure in `index.html`
2. Add styling in `css/styles.css`
3. Add functionality in `js/app.js`

### **API Integration**
Use the `apiRequest()` function for all backend calls:

```javascript
const cases = await apiRequest('/cases');
const result = await apiRequest('/cases/create', {
    method: 'POST',
    body: JSON.stringify(data)
});
```

## 📊 **Valid Values**

### **Case Types**
- FRAUD_DETECTION
- MONEY_LAUNDERING
- SUSPICIOUS_ACTIVITY
- COMPLIANCE_VIOLATION
- AML, FRAUD, COMPLIANCE, SANCTIONS, KYC

### **Priorities**
- LOW, MEDIUM, HIGH, CRITICAL

### **Typologies**
- MONEY_LAUNDERING
- TERRORIST_FINANCING
- FRAUD
- SANCTIONS_VIOLATION

### **Statuses**
- DRAFT
- READY_FOR_ASSIGNMENT
- PENDING_CASE_CREATION_APPROVAL
- IN_INVESTIGATION
- COMPLETED
- CLOSED
- REJECTED

## 🚀 **Quick Start**

1. **Start backend** (Spring Boot app)
2. **Open** `index.html` in browser
3. **Test** connection using console: `testConnection()`
4. **Create** your first case using "Create Case" tab
5. **View** cases in "Cases" tab

That's it! Your UI is ready to work with your Spring Boot backend! 🎉
