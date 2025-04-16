<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %> <%-- JSTL core library --%>

<!DOCTYPE html>
<html>
<head>
    <title>Update User</title>
    <style>
        body { font-family: sans-serif; }
        .form-container { width: 500px; margin: 20px auto; padding: 20px; border: 1px solid #ccc; border-radius: 5px; background-color: #f9f9f9;}
        .form-group { margin-bottom: 15px; }
        .form-group label { display: block; margin-bottom: 5px; font-weight: bold;}
        .form-group input[type="text"],
        .form-group input[type="email"],
        .form-group input[type="password"],
        .form-group select {
            width: 100%;
            padding: 8px;
            border: 1px solid #ccc;
            border-radius: 3px;
            box-sizing: border-box; /* Include padding and border in element's total width/height */
        }
        .form-group input[readonly] { background-color: #eee; cursor: not-allowed; }
        .form-actions { margin-top: 20px; text-align: right; }
        .form-actions input[type="submit"],
        .form-actions a {
             padding: 10px 15px;
             border: none;
             border-radius: 3px;
             cursor: pointer;
             text-decoration: none;
             margin-left: 10px;
         }
         .form-actions input[type="submit"] { background-color: #28a745; color: white; }
         .form-actions a { background-color: #6c757d; color: white; }
         .error-message { color: #dc3545; background-color: #f8d7da; border: 1px solid #f5c6cb; padding: 10px; margin-bottom: 15px; border-radius: 4px; }
         .password-note { font-size: 0.9em; color: #666; margin-top: 5px; }
    </style>
</head>
<body>

<div class="form-container">
    <h2>Update User Details</h2>

    <%-- Display any error messages passed from the servlet --%>
    <c:if test="${not empty requestScope.errorMessage}">
        <div class="error-message">
            <c:out value="${requestScope.errorMessage}"/>
        </div>
    </c:if>

    <%-- Check if userToEdit object exists --%>
    <c:if test="${not empty userToEdit}">
        <form action="updateUser" method="post">
            <%-- Hidden field to send the user ID back to the server --%>
            <input type="hidden" name="userId" value="<c:out value='${userToEdit.userId}'/>">

            <div class="form-group">
                <label for="userIdDisplay">User ID:</label>
                <input type="text" id="userIdDisplay" value="<c:out value='${userToEdit.userId}'/>" readonly>
            </div>

            <div class="form-group">
                <label for="username">Username:</label>
                <input type="text" id="username" name="username" value="<c:out value='${userToEdit.username}'/>" required>
            </div>

            <div class="form-group">
                <label for="email">Email:</label>
                <input type="email" id="email" name="email" value="<c:out value='${userToEdit.email}'/>" required>
            </div>

             <div class="form-group">
                <label for="password">Password:</label>
                <input type="password" id="password" name="password" placeholder="Leave blank to keep current password">
                 <div class="password-note">Only enter a new password if you want to change it.</div>
            </div>

            <div class="form-group">
                <label for="role">Role:</label>
                <%-- Ideally, roles might come from a predefined list or database --%>
                <%-- For simplicity, allowing text input or using existing roles --%>
                 <select id="role" name="role" required>
                    <%-- Pre-select the current role --%>
                    <option value="Admin" ${userToEdit.role == 'Admin' ? 'selected' : ''}>Admin</option>
                    <option value="Bakery shop staff" ${userToEdit.role == 'Bakery shop staff' ? 'selected' : ''}>Bakery Shop Staff</option>
                    <option value="Warehouse Staff" ${userToEdit.role == 'Warehouse Staff' ? 'selected' : ''}>Warehouse Staff</option>
                    <%-- Add other roles as needed --%>
                    <%-- Fallback if role is not standard --%>
                     <c:if test="${userToEdit.role != 'Admin' && userToEdit.role != 'Bakery shop staff' && userToEdit.role != 'Warehouse Staff'}">
                        <option value="${userToEdit.role}" selected><c:out value="${userToEdit.role}"/></option>
                    </c:if>
                </select>
                <%-- Or simpler text input if roles aren't fixed:
                <input type="text" id="role" name="role" value="<c:out value='${userToEdit.role}'/>" required>
                 --%>
            </div>

            <%-- Optional: Add dropdowns for Shop ID and Warehouse ID if applicable --%>
            <%-- These would typically be populated from database queries (e.g., in the controller) --%>
            <div class="form-group">
                <label for="shopId">Shop ID (Optional):</label>
                <%-- Example: Replace with a <select> dropdown populated from BakeryShopDB --%>
                <input type="text" id="shopId" name="shopId" value="<c:out value='${userToEdit.shopId}'/>">
            </div>

            <div class="form-group">
                <label for="warehouseId">Warehouse ID (Optional):</label>
                 <%-- Example: Replace with a <select> dropdown populated from WarehouseDB --%>
                <input type="text" id="warehouseId" name="warehouseId" value="<c:out value='${userToEdit.warehouseId}'/>">
            </div>


            <div class="form-actions">
                 <a href="listUsers">Cancel</a>
                <input type="submit" value="Update User">
            </div>
        </form>
    </c:if>
     <c:if test="${empty userToEdit}">
        <p class="error-message">User data could not be loaded for editing.</p>
         <p><a href="listUsers">Back to User List</a></p>
    </c:if>
</div>

</body>
</html>