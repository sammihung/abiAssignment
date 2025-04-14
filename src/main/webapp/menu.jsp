<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="checkLogin.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Menu</title>
        <style>
            .menu-bar {
                background-color: #333;
                overflow: hidden;
            }
            .menu-bar a {
                float: left;
                display: block;
                color: #f2f2f2;
                text-align: center;
                padding: 14px 16px;
                text-decoration: none;
            }
            .menu-bar a:hover {
                background-color: #ddd;
                color: black;
            }
            .user-info {
                float: right;
                color: #f2f2f2;
                padding: 14px 16px;
            }
            .logout-form {
                float: right;
                margin: 0;
                padding: 14px 16px;
            }
            .logout-form input[type="submit"] {
                background: none;
                border: none;
                color: #f2f2f2;
                cursor: pointer;
                font-size: 16px;
                text-decoration: underline;
            }
            .logout-form input[type="submit"]:hover {
                color: #ddd;
            }
        </style>
    </head>
    <body>
        <jsp:useBean id="userInfo" class="ict.bean.UserBean" scope="session" />
        <div class="menu-bar">
            <a href="welcome.jsp">Home</a>
            <a href="${pageContext.request.contextPath}/listUsers">List Users</a>
            <c:choose>
                <c:when test="${userInfo.role == 'Bakery shop staff'}">
                    <a href="register.jsp">Register</a>
                </c:when>
                <c:when test="${userInfo.role == 'Warehouse Staff'}">
                    <a href="register.jsp">Register</a>
                </c:when>
                <c:when test="${userInfo.role == 'Senior Management'}">
                    <a href="manageUsers.jsp">Manage Users</a>
                </c:when>
                <c:otherwise>
                    <a href="help.jsp">Help</a>
                </c:otherwise>
            </c:choose>
            <div class="user-info">
                Hello, <c:out value="${userInfo.username}" />
            </div>
            <form class="logout-form" method="post" action="main">
                <input type="hidden" name="action" value="logout" />
                <input type="submit" value="Logout" />
            </form>
        </div>
    </body>
</html>