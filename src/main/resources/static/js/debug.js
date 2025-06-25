// Debug script to test UI interactions
console.log('=== UI DEBUG SCRIPT LOADED ===');

// Test if functions exist
console.log('Functions available:');
console.log('- editCaseDetails:', typeof editCaseDetails);
console.log('- deleteCase:', typeof deleteCase);
console.log('- showCaseDetails:', typeof showCaseDetails);
console.log('- loadCases:', typeof loadCases);

// Test if global variables exist
console.log('Global variables:');
console.log('- currentUser:', currentUser);
console.log('- currentUserData:', currentUserData);
console.log('- allCases:', allCases);

// Test if DOM elements are accessible
console.log('DOM elements:');
console.log('- cases-container:', document.getElementById('cases-container'));
console.log('- nav buttons:', document.querySelectorAll('.nav-btn'));

// Add click listeners to test
document.addEventListener('click', function(e) {
    console.log('Click detected on:', e.target);
    console.log('Target class:', e.target.className);
    console.log('Target onclick:', e.target.onclick);
});

// Test navigation
function testNavigation() {
    console.log('Testing navigation...');
    showSection('cases');
    loadCases();
}

window.testNavigation = testNavigation;
console.log('Run testNavigation() in console to test navigation');
