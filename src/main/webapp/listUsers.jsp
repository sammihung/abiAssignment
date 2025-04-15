<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%-- Assuming menu.jsp provides the navigation menu --%>
<%@ include file="menu.jsp" %>
<%-- Import JSTL core library if not already implicitly available --%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!DOCTYPE html>
<html>
    <head>
        <meta charset="UTF-8">
        <title>List Users</title>
        <style>
            /* Basic table styling */
            table {
                width: 100%;
                border-collapse: collapse;
                margin-top: 15px;
            }
            table, th, td {
                border: 1px solid black;
            }
            th, td {
                padding: 8px;
                text-align: left;
                vertical-align: top; /* Align content to top */
            }
            th {
                background-color: #f2f2f2;
                cursor: pointer; /* Indicate headers are clickable for sorting */
            }
            /* Style for input fields within cells during edit mode */
            td.editable-cell input.cell-input {
                width: 95%; /* Use most of the cell width */
                padding: 4px;
                box-sizing: border-box; /* Include padding/border in width calculation */
                border: 1px solid #ccc; /* Subtle border for input */
            }
            /* Styling for buttons */
            button {
                padding: 5px 10px;
                margin-right: 5px;
                cursor: pointer;
            }
            /* Styling for error messages */
            .error-message {
                color: red;
                margin-top: 10px;
            }
        </style>
        <script>
            // --- Global Scope Variable ---
            // Track the current sorting state for each column {columnIndex: isAscending}
            const sortingState = {};

            // --- Sorting Function ---
            function sortTable(columnIndex) {
                const table = document.getElementById("usersTable");
                // Get tbody rows only for sorting data rows
                const tbody = table.tBodies[0];
                const rows = Array.from(tbody.rows);

                // Check if table exists and has rows
                if (!table || rows.length === 0)
                    return;

                // Determine sorting order (toggle)
                const isAscending = !sortingState[columnIndex];
                sortingState[columnIndex] = isAscending;

                // Sort the rows based on cell content (text or input value)
                rows.sort((rowA, rowB) => {
                    const cellA = rowA.cells[columnIndex];
                    const cellB = rowB.cells[columnIndex];

                    // Get content, checking if it's an input field
                    const cellAContent = cellA.querySelector('input.cell-input') ? cellA.querySelector('input.cell-input').value : cellA.textContent;
                    const cellBContent = cellB.querySelector('input.cell-input') ? cellB.querySelector('input.cell-input').value : cellB.textContent;

                    // Case-insensitive comparison
                    const valA = cellAContent.toLowerCase().trim();
                    const valB = cellBContent.toLowerCase().trim();

                    // Basic comparison logic
                    if (valA < valB)
                        return isAscending ? -1 : 1;
                    if (valA > valB)
                        return isAscending ? 1 : -1;
                    return 0; // Values are equal
                });

                // Re-append sorted rows to the tbody
                rows.forEach(row => tbody.appendChild(row));

                // Update column header indicators
                const headers = table.querySelectorAll("thead th"); // Select only thead th
                headers.forEach((header, index) => {
                    // Remove existing arrows more reliably using textContent
                    let currentHeaderText = header.textContent.replace(/[\u25B2\u25BC]/g, "").trim();
                    if (index < header.parentNode.cells.length - 1) { // Avoid adding arrow to the hidden select column header initially
                        if (index === columnIndex) {
                            // Add arrow to the sorted column header
                            header.innerHTML = currentHeaderText + (isAscending ? " &#9650;" : " &#9660;"); // Appending HTML entity
                        } else {
                            // Set text content directly for others
                            header.textContent = currentHeaderText;
                        }
                    } else {
                        // Ensure the select column header text doesn't get arrows
                        header.textContent = currentHeaderText;
                    }

                });
            }

            // --- Edit Mode Helper Functions ---

            // Helper function to convert a text cell to an input cell
            function switchToInput(cell) {
                const currentText = cell.textContent.trim();
                // Clear previous content
                while (cell.firstChild) {
                    cell.removeChild(cell.firstChild);
                }
                // Create input
                const inputElement = document.createElement('input');
                inputElement.type = 'text';
                inputElement.value = currentText;
                inputElement.classList.add('cell-input'); // Add class for styling/selection
                // Store original value in data attribute (useful for cancel functionality if needed later)
                inputElement.setAttribute('data-original-value', currentText);
                cell.appendChild(inputElement);
            }

            // Helper function to convert an input cell back to a text cell
            function switchToText(cell, saveChanges = false) {
                const inputElement = cell.querySelector('input.cell-input');
                if (inputElement) {
                    const currentValue = inputElement.value;
                    const originalValue = inputElement.getAttribute('data-original-value');

                    // --- !!! PLACEHOLDER FOR SAVE LOGIC !!! ---
                    // If 'saveChanges' is true AND the value actually changed, trigger save
                    if (saveChanges && currentValue !== originalValue) {
                        console.log(`Value changed in cell. Original: "${originalValue}", New: "${currentValue}". Triggering save...`);
                        // ** TODO: Implement AJAX Call to Save Data **
                        // You need:
                        // 1. User ID (from a sibling cell or data attribute on the row 'tr')
                        // 2. Column Identifier (e.g., column index or a data attribute on the 'th')
                        // 3. The 'currentValue'
                        // Use fetch() similar to deleteSelectedUsers to send data to a save endpoint.
                        // Example: saveCellData(userId, columnName, currentValue);
                    }
                    // --- End Placeholder ---

                    // Clear the input element
                    while (cell.firstChild) {
                        cell.removeChild(cell.firstChild);
                    }
                    // Set text content back (using the potentially updated value)
                    cell.textContent = currentValue;
            }
            }

            // --- Main Toggle Function ---
            function toggleEditMode() {
                const table = document.getElementById("usersTable");
                const editButton = document.getElementById("editButton");
                const deleteButton = document.getElementById("deleteButton");

                if (!table || !editButton || !deleteButton) {
                    console.error("Required elements (table, edit button, delete button) not found.");
                    return;
                }

                // Check current state based on button text *before* changing it
                const isCurrentlyInEditMode = editButton.innerText === "Done";

                // Toggle button text
                editButton.innerText = isCurrentlyInEditMode ? "Edit" : "Done";

                // Show/hide delete button
                deleteButton.style.display = isCurrentlyInEditMode ? "none" : "inline-block";

                // Show/hide the checkbox column header and cells
                const checkboxHeader = table.querySelector('th.checkbox-column');
                const checkboxCells = table.querySelectorAll('td.checkbox-column');

                if (checkboxHeader) {
                    checkboxHeader.style.display = isCurrentlyInEditMode ? "none" : ""; // Show when entering edit mode
                }
                checkboxCells.forEach(cbCell => {
                    cbCell.style.display = isCurrentlyInEditMode ? "none" : ""; // Show when entering edit mode
                });

                // Toggle editable cells between text and input
                const editableCells = table.querySelectorAll('td.editable-cell');
                editableCells.forEach(cell => {
                    if (isCurrentlyInEditMode) {
                        // LEAVING edit mode: Change input back to text, flag potential save
                        switchToText(cell, true);
                    } else {
                        // ENTERING edit mode: Change text to input
                        switchToInput(cell);
                    }
                });
            }

            // --- Delete Function ---
            function deleteSelectedUsers() {
                const checkboxes = document.querySelectorAll('input[name="selectUser"]:checked');
                const selectedUserIds = Array.from(checkboxes).map(checkbox => checkbox.value);

                if (selectedUserIds.length === 0) {
                    alert("Please select at least one user to delete.");
                    return;
                }

                if (confirm(`Are you sure you want to delete ${selectedUserIds.length} selected user(s)?`)) {
                    // **NOTE:** Adjust 'deleteUsers' to your actual server endpoint URL
                    fetch('deleteUsers', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            // Add any other required headers, like CSRF tokens if needed
                        },
                        body: JSON.stringify({userIds: selectedUserIds})
                    })
                            .then(response => {
                                if (!response.ok) {
                                    // Try to get error message from server if possible
                                    return response.text().then(text => {
                                        throw new Error(`Server error: ${response.status} - ${text || response.statusText}`);
                                    });
                                }
                                // Handle potential 'No Content' success response
                                if (response.status === 204) {
                                    return {success: true}; // Treat 204 as success
                                }
                                // Otherwise, expect JSON
                                return response.json();
                            })
                            .then(data => {
                                if (data && data.success !== false) {
                                    alert("Users deleted successfully.");
                                    location.reload(); // Reload the page to show updated list
                                } else {
                                    // Use server message if available, otherwise generic failure
                                    // Corrected version using string concatenation
                                    alert("Failed to delete users: " + (data?.message || 'Server indicated failure.'));
                                }
                            })
                            .catch(error => {
                                console.error("Error deleting users:", error);
                                alert("An error occurred while deleting users:\n" + error.message);
                            });
                }
            }

            // --- Ensure functions run after the DOM is ready ---
            // While toggleEditMode and deleteSelectedUsers are called via onclick,
            // it's good practice for any setup code.
            // document.addEventListener('DOMContentLoaded', function() {
            //     // Any setup code that needs the DOM can go here
            // });

        </script>
    </head>
    <body>

        <h1>Users List</h1> <%-- Changed title slightly --%>

        <%-- Display potential error message passed from Servlet --%>
        <c:if test="${not empty requestScope.error}">
            <p class="error-message">${requestScope.error}</p>
        </c:if>
        <c:if test="${not empty requestScope.message}">
            <p style="color: green;">${requestScope.message}</p>
        </c:if>

        <%-- Buttons for Edit/Done and Delete --%>
        <button id="editButton" onclick="toggleEditMode()">Edit</button>
        <button id="deleteButton" onclick="deleteSelectedUsers()" style="display: none;">Delete Selected</button>

        <%-- Table to display users --%>
        <table id="usersTable">
            <thead>
                <tr>
                    <%-- Make headers clickable for sorting --%>
                    <th onclick="sortTable(0)">User ID</th>
                    <th onclick="sortTable(1)">Username</th>
                    <th onclick="sortTable(2)">Email</th>
                    <th onclick="sortTable(3)">Role</th>
                    <th onclick="sortTable(4)">Shop ID</th>
                    <th onclick="sortTable(5)">Warehouse ID</th>
                        <%-- Hidden column for checkboxes, shown in edit mode --%>
                    <th class="checkbox-column" style="display: none;">Select</th>
                </tr>
            </thead>
            <tbody>
                <%-- Loop through users passed from the Servlet/Controller --%>
                <c:forEach var="user" items="${requestScope.users}">
                    <%-- Add a data attribute to the row for easy User ID access if needed for saving --%>
                    <tr data-userid="${user.userid}">
                        <%-- Add editable-cell class to cells that should become inputs --%>
                        <td class="editable-cell">${user.userid}</td> <%-- Typically UserID is not editable, remove class if so --%>
                        <td class="editable-cell">${user.username}</td>
                        <td class="editable-cell">${user.email}</td>
                        <td class="editable-cell">${user.role}</td>
                        <td class="editable-cell">${user.shopId}</td>
                        <td class="editable-cell">${user.warehouseId}</td>
                        <%-- Checkbox column, initially hidden --%>
                        <td class="checkbox-column" style="display: none;">
                            <input type="checkbox" name="selectUser" value="${user.userid}">
                        </td>
                    </tr>
                </c:forEach>
                <%-- Handle case where there are no users --%>
                <c:if test="${empty requestScope.users}">
                    <tr>
                        <td colspan="7" style="text-align: center;">No users found.</td>
                    </tr>
                </c:if>
            </tbody>
        </table>

    </body>
</html>