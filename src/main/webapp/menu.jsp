<%@page %> <%-- Removed conflicting contentType attribute --%>
<%-- Assuming checkLogin.jsp verifies session and userInfo bean exists --%>
<%@ include file="checkLogin.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"> <%-- Keep meta tag for browser --%>
        <title>Menu</title>
        <style>
            body {
                margin: 0;
                font-family: sans-serif;
            } /* Reset body margin */
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
                font-size: 16px; /* Consistent font size */
            }
            .menu-bar a:hover {
                background-color: #ddd;
                color: black;
            }
            .user-info {
                float: right;
                color: #f2f2f2;
                padding: 14px 10px 14px 16px; /* Adjust padding */
                font-size: 16px; /* Consistent font size */
            }
            .logout-form {
                float: right;
                margin: 0;
                padding: 0; /* Remove padding if button provides it */
            }
            /* Style the logout button like other menu items */
            .logout-button {
                background: none;
                border: none;
                color: #f2f2f2;
                cursor: pointer;
                font-size: 16px; /* Consistent font size */
                padding: 14px 16px; /* Match link padding */
                display: block; /* Make it block */
                text-align: center;
                font-family: sans-serif; /* Ensure font matches */
            }
            .logout-button:hover {
                background-color: #ddd;
                color: black;
            }
        </style>
    </head>
    <body>
        <%-- Use jsp:useBean only if checkLogin.jsp doesn't already guarantee its presence --%>
        <%-- <jsp:useBean id="userInfo" class="ict.bean.UserBean" scope="session" /> --%>

        <div class="menu-bar">
            <a href="<c:url value='/welcome.jsp'/>">Home</a>

            <%-- Role-specific links --%>
            <c:choose>
                <c:when test="${userInfo.role == 'Bakery shop staff'}">
                    <a href="<c:url value='/register.jsp'/>">Register</a>
                    <a href="<c:url value='/reserveFruit'/>">Reserve Fruit</a>
                    <a href="<c:url value='/borrowFruit'/>">Borrow Fruit</a>
                    <a href="<c:url value='/listReservations'/>">View Reservations</a>
                    <a href="<c:url value='/updateInventory'/>">Update Inventory</a>
                </c:when>
                <c:when test="${userInfo.role == 'Warehouse Staff'}">
                    <a href="<c:url value='/register.jsp'/>">Register</a>
                    <a href="<c:url value='/updateWarehouseInventory'/>">Update Warehouse Inventory</a>
                    <a href="<c:url value='/needsApproval'/>">Approve Needs</a>
                    <a href="<c:url value='/arrangeDelivery'/>">Arrange Delivery</a>
                    <a href="<c:url value='/checkoutToShop'/>">Checkout to Shops</a>
                </c:when>
                <c:when test="${userInfo.role == 'Senior Management'}">
                    <a href="<c:url value='/listUsers'/>">Manage Users</a>
                    <a href="<c:url value='/listFruits'/>">Manage Fruit Types</a>
                    <a href="<c:url value='/viewAdvancedReport'/>">Advanced Reports</a>
                    <%-- Link to Inventory Report --%>
                    <a href="<c:url value='/viewInventoryReport'/>">Inventory Report</a>
                </c:when>
            </c:choose>

            <%-- Logout Form and User Info (Floated Right) --%>
            <form class="logout-form" method="post" action="<c:url value='/login'/>">
                <input type="hidden" name="action" value="logout" />
                <button type="submit" class="logout-button">Logout</button>
            </form>
            <div class="user-info">
                Hello, <c:out value="${userInfo.username}" /> (<c:out value="${userInfo.role}" />)
            </div>

        </div> <%-- End of menu-bar --%>

    </body>
</html>
