<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="checkLogin.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>register</title>
</head>
<body>
    <h2>register form</h2>
    <form action="register" method="post">
        <label for="username">username:</label>
        <input type="text" id="username" name="username" required><br><br>
        
        <label for="password">password:</label>
        <input type="password" id="password" name="password" required><br><br>
        
        <label for="email">email:</label>
        <input type="email" id="email" name="email" required><br><br>

        <c:choose>
            <c:when test="${user.role == 'Bakery shop staff'}"></c:when>
            <label for="shop">shop:</label>
            <input type="text" id="shopSearch" placeholder="Search shop...">
            <select id="shop" name="shop" required>
                <c:forEach var="shop" items="${shopList}">
                <option value="${shop.id}">${shop.name}</option>
                </c:forEach>
            </select><br><br>
            </c:when>
            <c:when test="${user.role == 'Warehouse Staff'}">
            <label for="warehouse">warehouse:</label>
            <input type="text" id="warehouseSearch" placeholder="Search warehouse...">
            <select id="warehouse" name="warehouse" required>
                <c:forEach var="warehouse" items="${warehouseList}">
                <option value="${warehouse.id}">${warehouse.name}</option>
                </c:forEach>
            </select><br><br>
            </c:when>
        </c:choose>
        
        <input type="submit" value="register">
    </form>
</body>
</html>