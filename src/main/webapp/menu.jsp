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
            body { font-family: sans-serif; margin: 20px; background-color: #f4f4f4; }
            .container { background-color: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0,0,0,0.1); max-width: 900px; margin: auto; }
            h1 { color: #333; text-align: center; }
            table { width: 100%; border-collapse: collapse; margin-top: 20px; }
            th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
            th { background-color: #f2f2f2; cursor: pointer; }
            tr:nth-child(even) { background-color: #f9f9f9; }
            .message, .error-message { padding: 10px; margin-bottom: 15px; border-radius: 4px; text-align: center; }
            .message { background-color: #d4edda; color: #155724; border: 1px solid #c3e6cb; }
            .error-message { background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
            .back-link { display: block; text-align: center; margin-top: 20px; }
            .dataTables_filter, .dataTables_info, .dataTables_paginate { margin-bottom: 15px; }
            .approve-button { background-color: #28a745; color: white; padding: 5px 10px; border: none; border-radius: 4px; cursor: pointer; }
            .approve-button:hover { background-color: #218838; }
        </style>
    </head>
    <body>
        <div class="menu-bar">
            <a href="<c:url value='/welcome.jsp'/>">Home</a>

            <%-- Role-specific links --%>
            <c:choose>
                <c:when test="${userInfo.role == 'Bakery shop staff'}">
                    <a href="<c:url value='/register.jsp'/>">Register</a>
                    <%-- ***** MODIFIED LINE BELOW ***** --%>
                    <a href="<c:url value='/orderFromSource'/>">Order from Source</a> <%-- Was /reserveFruit --%>
                    <%-- ***** END OF MODIFIED LINE ***** --%>
                    <a href="<c:url value='/borrowFruit'/>">Borrow Fruit</a>
                    <a href="<c:url value='/listReservations'/>">View Reservations</a>
                    <a href="<c:url value='/updateInventory'/>">Update Inventory</a>
                    <a href="<c:url value='/listBorrowings'/>">View Borrowings</a>
                    <a href="<c:url value='/approveBorrow'/>">Approve Borrow Requests</a>
                </c:when>
                <c:when test="${userInfo.role == 'Warehouse Staff'}">
                    <a href="<c:url value='/register.jsp'/>">Register</a>
                    <a href="<c:url value='/updateWarehouseInventory'/>">Update Warehouse Inventory</a>
                    <a href="<c:url value='/needsApproval'/>">Approve Needs</a>
                    <a href="<c:url value='/arrangeDelivery'/>">Arrange Delivery</a>
                    <a href="<c:url value='/checkoutToShop'/>">Checkout to Shops</a>
                    <a href="<c:url value='/listDeliveries'/>">View Deliveries</a>
                </c:when>
                <c:when test="${userInfo.role == 'Senior Management'}">
                    <a href="<c:url value='/listUsers'/>">Manage Users</a>
                    <a href="<c:url value='/listFruits'/>">Manage Fruit Types</a>
                    <a href="<c:url value='/viewAdvancedReport'/>">Advanced Reports</a>
                    <a href="<c:url value='/viewInventoryReport'/>">Inventory Report</a>
                    <a href="<c:url value='/listBorrowings'/>">View Borrowings</a>
                    <a href="<c:url value='/listDeliveries'/>">View Deliveries</a>
                    <a href="<c:url value='/listAllInventory'/>">View All Inventory</a>
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
