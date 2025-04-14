<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="menu.jsp" %>
<!DOCTYPE html>
<html>
<head>
    <title>List Users</title>
    <style>
        table {
            width: 100%;
            border-collapse: collapse;
        }
        table, th, td {
            border: 1px solid black;
        }
        th, td {
            padding: 8px;
            text-align: left;
        }
        th {
            background-color: #f2f2f2;
            cursor: pointer;
        }
    </style>
    <script>
        // Track the current sorting state for each column
        const sortingState = {};

        // Function to sort the table
        function sortTable(columnIndex) {
            const table = document.getElementById("usersTable");
            const rows = Array.from(table.rows).slice(1); // Exclude header row

            // Determine sorting order (toggle between ascending and descending)
            const isAscending = !sortingState[columnIndex];
            sortingState[columnIndex] = isAscending;

            rows.sort((rowA, rowB) => {
                const cellA = rowA.cells[columnIndex].innerText.toLowerCase();
                const cellB = rowB.cells[columnIndex].innerText.toLowerCase();
                if (cellA < cellB) return isAscending ? -1 : 1;
                if (cellA > cellB) return isAscending ? 1 : -1;
                return 0;
            });

            rows.forEach(row => table.tBodies[0].appendChild(row)); // Reorder rows

            // Update column header to show sorting direction
            const headers = table.querySelectorAll("th");
            headers.forEach((header, index) => {
                if (index === columnIndex) {
                    header.innerHTML = header.innerHTML.replace(/[\u25B2\u25BC]/g, "") + (isAscending ? " &#9650;" : " &#9660;");
                } else {
                    header.innerHTML = header.innerHTML.replace(/[\u25B2\u25BC]/g, "");
                }
            });
        }

        // Function to toggle edit mode
        function toggleEditMode() {
            const table = document.getElementById("usersTable");
            const editButton = document.getElementById("editButton");
            const deleteButton = document.getElementById("deleteButton");
            const isEditing = editButton.innerText === "Done";

            // Toggle button text
            editButton.innerText = isEditing ? "Edit" : "Done";

            // Show or hide the checkbox column and delete button
            deleteButton.style.display = isEditing ? "none" : "inline-block";
            const rows = table.rows;
            for (let i = 0; i < rows.length; i++) {
                const cells = rows[i].cells;
                if (isEditing) {
                    if (cells[cells.length - 1].classList.contains("checkbox-column")) {
                        cells[cells.length - 1].style.display = "none";
                    }
                } else {
                    if (cells[cells.length - 1].classList.contains("checkbox-column")) {
                        cells[cells.length - 1].style.display = "";
                    }
                }
            }
        }

        // Function to delete selected users
        function deleteSelectedUsers() {
            const checkboxes = document.querySelectorAll('input[name="selectUser"]:checked');
            const selectedUserIds = Array.from(checkboxes).map(checkbox => checkbox.value);

            if (selectedUserIds.length === 0) {
                alert("Please select at least one user to delete.");
                return;
            }

            if (confirm("Are you sure you want to delete the selected users?")) {
                // Send selectedUserIds to the server for deletion
                fetch('deleteUsers', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({ userIds: selectedUserIds })
                })
                .then(response => response.json())
                .then(data => {
                    if (data.success) {
                        alert("Users deleted successfully.");
                        location.reload(); // Reload the page to reflect changes
                    } else {
                        alert("Failed to delete users.");
                    }
                })
                .catch(error => {
                    console.error("Error deleting users:", error);
                    alert("An error occurred while deleting users.");
                });
            }
        }
    </script>
</head>
<body>
    <h1>Users with the Same Role</h1>
    <button id="editButton" onclick="toggleEditMode()">Edit</button>
    <button id="deleteButton" onclick="deleteSelectedUsers()" style="display: none;">Delete</button>
    <!-- Display error message if available -->
    <c:if test="${not empty error}">
        <p style="color: red;">${error}</p>
    </c:if>
    <!-- Display users in a table -->
    <table id="usersTable">
        <thead>
            <tr>
                <th onclick="sortTable(0)">User ID</th>
                <th onclick="sortTable(1)">Username</th>
                <th onclick="sortTable(2)">Email</th>
                <th onclick="sortTable(3)">Role</th>
                <th onclick="sortTable(4)">Shop ID</th>
                <th onclick="sortTable(5)">Warehouse ID</th>
                <th class="checkbox-column" style="display: none;">Select</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var="user" items="${users}">
                <tr>
                    <td>${user.userid}</td>
                    <td>${user.username}</td>
                    <td>${user.email}</td>
                    <td>${user.role}</td>
                    <td>${user.shopId}</td>
                    <td>${user.warehouseId}</td>
                    <td class="checkbox-column" style="display: none;">
                        <input type="checkbox" name="selectUser" value="${user.userid}">
                    </td>
                </tr>
            </c:forEach>
        </tbody>
    </table>
</body>
</html>
