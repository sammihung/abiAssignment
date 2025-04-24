<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="ict.bean.UserBean" %> 
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!DOCTYPE html>
<html>
<head>
    
   
    <title>${listTitle}</title> 
 
    <link rel="stylesheet" type="text/css" href="https://cdn.datatables.net/1.13.6/css/jquery.dataTables.min.css">
    <style>
        html, body {
            margin: 0;
            padding: 0;
        }
        body { font-family: sans-serif; margin: 20px; }
        table { width: 100%; border-collapse: collapse; }
        #userTable th, #userTable td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        #userTable th { background-color: #f2f2f2; }
        .actions a, .actions button { margin-right: 5px; text-decoration: none; padding: 5px 10px; border: 1px solid #ccc; border-radius: 3px; cursor: pointer; font-size: 0.9em; }
        .actions a.update { background-color: #ffc107; color: black; border-color: #dda800; }
        .actions button.delete { background-color: #dc3545; color: white; border-color: #c82333; }
        .message { padding: 10px; margin-bottom: 15px; border-radius: 4px; }
        .message.success { background-color: #d4edda; color: #155724; border: 1px solid #c3e6cb; }
        .message.error { background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
        .dataTables_wrapper .dataTables_filter { float: right; text-align: right; margin-bottom: 10px; }
        .dataTables_wrapper .dataTables_length { float: left; margin-bottom: 10px;}
        .dataTables_wrapper .dataTables_info { clear: both; float: left; padding-top: 10px;}
        .dataTables_wrapper .dataTables_paginate { float: right; padding-top: 10px;}
        .user-data-row {}
        .create-user-button { display: inline-block; padding: 8px 15px; margin-bottom: 15px; background-color: #007bff; color: white; text-decoration: none; border-radius: 4px; border: none; cursor: pointer; }
        .create-user-button:hover { background-color: #0056b3; }
        #jsErrorDiv { display: none; }
    </style>
</head>
<body>
    <jsp:include page="menu.jsp" />
    <h1>${listTitle}</h1>
    <div id="messageArea"> 
        <c:if test="${not empty param.message}"> <div class="message success"><c:out value="${param.message}"/></div> </c:if>
        <c:if test="${not empty param.error}"> <div class="message error"><c:out value="${param.error}"/></div> </c:if>
        <c:if test="${not empty requestScope.errorMessage}"> <div class="message error"><c:out value="${requestScope.errorMessage}"/></div> </c:if>
        <div id="jsErrorDiv" class="message error"></div>
    </div>

    <c:if test="${userInfo.role == 'Senior Management'}">
        <a href="<c:url value='/adminCreateUser'/>" class="create-user-button">Create New User</a>
    </c:if>

    <c:choose>
        <c:when test="${not empty usersByRole}">
            <table id="userTable" class="display">
                <thead>
                    <tr>
                        <th>User ID</th>
                        <th>Username</th>
                        <th>Email</th>
                        <th>Role</th>
                        <th>Shop ID</th>
                        <th>Warehouse ID</th>
                        <th data-orderable="false">Actions</th>
                    </tr>
                </thead>
                <tbody>
                  
                    <c:forEach var="roleEntry" items="${usersByRole}">
                       
                        <c:forEach var="user" items="${roleEntry.value}">
                            <tr id="user-row-${user.userId}" class="user-data-row">
                                <td><c:out value="${user.userId}"/></td>
                                <td><c:out value="${user.username}"/></td>
                                <td><c:out value="${user.userEmail}"/></td>
                                <td><c:out value="${user.role}"/></td>
                                <td><c:out value="${user.shopId != null ? user.shopId : 'N/A'}"/></td>
                                <td><c:out value="${user.warehouseId != null ? user.warehouseId : 'N/A'}"/></td>
                                <td class="actions">
                                    <a href="updateUser?userId=${user.userId}" class="update">Update</a>
                                    <button type="button" class="delete" onclick="deleteUser('${user.userId}')">Delete</button>
                                </td>
                            </tr>
                        </c:forEach>
                    </c:forEach>
                </tbody>
            </table>
        </c:when>
        <c:otherwise>
            <c:if test="${empty requestScope.errorMessage}">
                 <p>No users found matching the criteria.</p>
            </c:if>
        </c:otherwise>
    </c:choose>


    <script src="https://code.jquery.com/jquery-3.7.0.min.js"></script>
    <script src="https://cdn.datatables.net/1.13.6/js/jquery.dataTables.min.js"></script>

    
    <script>
        $(document).ready(function() {
            $('#userTable').DataTable({
                 "order": [[ 3, "asc" ], [1, "asc"]] 
            });
        });

        function deleteUser(userId) {
            $('#jsErrorDiv').hide().text('');

            if (confirm('Are you sure you want to delete user ID: ' + userId + '?')) {
                const data = { userIds: [userId] };
                fetch('deleteUsers', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(data)
                })
                .then(response => {
                     if (!response.ok) {
                        return response.text().then(text => { throw new Error(text || `Server responded with status: ${response.status}`); });
                     }
                     const contentType = response.headers.get("content-type");
                     if (contentType && contentType.indexOf("application/json") !== -1) {
                         return response.json();
                     } else {
                         throw new Error("Received non-JSON response on successful delete.");
                     }
                 })
                .then(result => {
                    console.log('Delete response:', result);
                    if (result.success) {
                        alert(result.message || 'User deleted successfully.');
                        location.reload();
                    } else {
                        $('#jsErrorDiv').text('Error deleting user: ' + (result.message || 'Failed. Check server logs.')).show();
                    }
                })
                .catch(error => {
                    console.error('Error during fetch:', error);
                     $('#jsErrorDiv').text('An error occurred: ' + error.message).show();
                });
            }
        }
    </script>

</body>
</html>
