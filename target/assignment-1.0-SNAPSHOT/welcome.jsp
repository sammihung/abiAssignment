<%@ taglib prefix="my" uri="/WEB-INF/tlds/mytags" %>
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
        <p>Copyright &copy; <my:currentYear /> Acer International Bakery. All rights reserved.</p>
        <c:if test="${not empty param.message}">
            <p style="color: green;">${param.message}</p>
        </c:if>
        
    </body>
</html>