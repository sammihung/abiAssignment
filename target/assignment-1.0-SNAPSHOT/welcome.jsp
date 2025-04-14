<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="menu.jsp" %>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Welcome</title>
    </head>
    <body>
        <h1>Welcome to the System</h1>
        <!-- Display success message if available -->
        <c:if test="${not empty param.message}">
            <p style="color: green;">${param.message}</p>
        </c:if>
        <p>Welcome to the ICT</p>
        <hr/>
        <a href="brandController?action=list">getAllBrands</a>
    </body>
</html>