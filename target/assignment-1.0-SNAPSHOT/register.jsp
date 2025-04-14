<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="menu.jsp" %>
<!DOCTYPE html>
<html>
    <head>
        <meta charset="UTF-8">
        <title>Register</title>
 
    </head>
    <body>
        <h2>Register Form</h2>
        <!-- Display error message if available -->
        <c:if test="${not empty error}">
            <p style="color: red;">${error}</p>
        </c:if>
        <form action="register" method="post">
            <label for="username">Username:</label>
            <input type="text" id="username" name="username" required><br><br>

            <label for="password">Password:</label>
            <input type="password" id="password" name="password"
                required><br><br>

            <label for="email">Email:</label>
            <input type="email" id="email" name="email" required><br><br>
            <c:choose>
                <c:when test="${userInfo.role == 'Bakery shop staff'}">
                    <label for="shop">Shop:</label>
                    <input type="text" id="shopId" name="shopId"
                        value="${userInfo.shopId}" readonly><br><br>
                </c:when>
                <c:when test="${userInfo.role == 'Warehouse Staff'}">
                    <label for="warehouse">Warehouse:</label>
                    <input type="text" id="warehouseId" name="warehouseId"
                        value="${userInfo.warehouseId}" readonly><br><br>
                </c:when>
                <c:otherwise>
                    <p>Debug: User Role is not recognized or not set.</p>
                </c:otherwise>
            </c:choose>

            <input type="submit" value="Register">
        </form>
    </body>
</html>