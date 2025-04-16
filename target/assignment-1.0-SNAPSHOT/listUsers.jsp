<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %> <%-- JSTL core library --%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %> <%-- JSTL functions library (optional) --%>

<!DOCTYPE html>
<html>
<head>
    <title>User List</title>
    <%-- Link to DataTables CSS --%>
    <link rel="stylesheet" type="text/css" href="https://cdn.datatables.net/1.13.6/css/jquery.dataTables.min.css">
    <style>
        body { font-family: sans-serif; }
        table { width: 100%; border-collapse: collapse; }
        /* Ensure table header and body styles are compatible with DataTables */
        #userTable th, #userTable td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        #userTable th { background-color: #f2f2f2; }
        /* Style the action buttons */
        .actions a, .actions button {
             margin-right: 5px;
             text-decoration: none;
             padding: 5px 10px;
             border: 1px solid #ccc;
             border-radius: 3px;
             cursor: pointer; /* Make button look clickable */
             font-size: 0.9em; /* Slightly smaller buttons */
        }
        .actions a.update { background-color: #ffc107; color: black; border-color: #dda800; }
        .actions button.delete { background-color: #dc3545; color: white; border-color: #c82333; }
        .message { padding: 10px; margin-bottom: 15px; border-radius: 4px; }
        .message.success { background-color: #d4edda; color: #155724; border: 1px solid #c3e6cb; }
        .message.error { background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }

        /* DataTables search/filter alignment */
        .dataTables_wrapper .dataTables_filter { float: right; text-align: right; margin-bottom: 10px; }
        .dataTables_wrapper .dataTables_length { float: left; margin-bottom: 10px;}
        .dataTables_wrapper .dataTables_info { clear: both; float: left; padding-top: 10px;}
        .dataTables_wrapper .dataTables_paginate { float: right; padding-top: 10px;}

    </style>
</head>
<body>

<h1>User List</h1>

<%-- Display messages passed via request parameters (e.g., after redirect) --%>
<c:if test="${not empty param.message}">
    <div class="message success"><c:out value="${param.message}"/></div>
</c:if>
<c:if test="${not empty param.error}">
    <div class="message error"><c:out value="${param.error}"/></div>
</c:if>
<%-- Display errors passed via request attributes (e.g., after forward) --%>
<c:if test="${not empty requestScope.error}">
     <div class="message error"><c:out value="${requestScope.error}"/></div>
 </c:if>
 <c:if test="${not empty requestScope.errorMessage}">
     <div class="message error"><c:out value="${requestScope.errorMessage}"/></div>
 </c:if>


<c:choose>
    <c:when test="${not empty users}">
        <%-- Add an ID to the table for DataTables to target --%>
        <table id="userTable" class="display">
            <thead>
                <tr>
                    <th>User ID</th>
                    <th>Username</th>
                    <th>Email</th>
                    <th>Role</th>
                    <th>Shop ID</th>
                    <th>Warehouse ID</th>
                    <th data-orderable="false">Actions</th> <%-- Disable sorting on the Actions column --%>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="user" items="${users}">
                    <%-- Add a unique ID to each row if needed for JS manipulation --%>
                    <tr id="user-row-${user.userid}">
                        <td><c:out value="${user.userid}"/></td>
                        <td><c:out value="${user.username}"/></td>
                        <td><c:out value="${user.email}"/></td>
                        <td><c:out value="${user.role}"/></td>
                        <td><c:out value="${user.shopId != null ? user.shopId : 'N/A'}"/></td>
                        <td><c:out value="${user.warehouseId != null ? user.warehouseId : 'N/A'}"/></td>
                        <td class="actions">
                            <%-- Update Link --%>
                            <a href="updateUser?userId=${user.userid}" class="update">Update</a>
                            <%-- Delete Button - Calls JavaScript function --%>
                            <button type="button" class="delete" onclick="deleteUser('${user.userid}')">Delete</button>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:when>
    <c:otherwise>
        <p>No users found for your role.</p>
    </c:otherwise>
</c:choose>

<p><a href="welcome.jsp">Back to Welcome Page</a></p> <%-- Link back to a main/welcome page --%>

<%-- Include jQuery (required by DataTables) --%>
<script src="https://code.jquery.com/jquery-3.7.0.min.js"></script>
<%-- Include DataTables JavaScript --%>
<script src="https://cdn.datatables.net/1.13.6/js/jquery.dataTables.min.js"></script>

<%-- JavaScript for DataTables Initialization and Delete Function --%>
<script>
    // Initialize DataTables for sorting, searching, pagination
    $(document).ready(function() {
        $('#userTable').DataTable({
            // Optional: Add configurations like default sorting order, paging options etc.
            // Example: Order by the first column (User ID) ascending by default
             "order": [[ 0, "asc" ]]
        });
    });

    // JavaScript function to handle user deletion via fetch API
    function deleteUser(userId) {
        if (confirm('Are you sure you want to delete user ID: ' + userId + '?')) {
            // Data structure expected by your DeleteUsersController
            const data = { userIds: [userId] }; // Send as an array with single ID

            fetch('deleteUsers', { // URL mapping for DeleteUsersController
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    // Add CSRF token header here if your application uses them
                },
                body: JSON.stringify(data)
            })
            .then(response => {
                // Check if response is JSON, otherwise handle as text
                 const contentType = response.headers.get("content-type");
                 if (contentType && contentType.indexOf("application/json") !== -1) {
                    return response.json();
                 } else {
                     // Handle non-JSON response (e.g., plain text error from server)
                     return response.text().then(text => { throw new Error("Received non-JSON response: " + text); });
                 }
             })
            .then(result => {
                console.log('Delete response:', result); // Log server response
                if (result.success) {
                    alert(result.message || 'User deleted successfully.');
                    // Option 1: Reload the whole page
                    // location.reload();

                    // Option 2: Remove the row from the table using DataTables API (more seamless)
                     var table = $('#userTable').DataTable();
                     // Find the row with the specific ID and remove it
                     table.row('#user-row-' + userId).remove().draw(false); // 'false' prevents resetting pagination

                } else {
                    alert('Error deleting user: ' + (result.message || 'Failed to delete user. Check server logs.'));
                }
            })
            .catch(error => {
                console.error('Error during fetch:', error);
                alert('An error occurred while trying to delete the user: ' + error.message);
            });
        }
    }
</script>

</body>
</html>