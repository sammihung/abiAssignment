<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="menu.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page import="ict.bean.UserBean" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Arrange Delivery</title>
    <style>
        html, body {
            margin: 0;
            padding: 0;
        }
        .menu-bar {
            margin: 0;
            padding: 0;
        }
        body { font-family: sans-serif; margin: 0px; background-color: #f4f4f4; }
        .container { background-color: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0,0,0,0.1); max-width: 950px; margin: auto; }
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
        .checkout-button { background-color: #17a2b8; color: white; padding: 5px 10px; border: none; border-radius: 4px; cursor: pointer; } 
        .checkout-button:hover { background-color: #138496; }
    </style>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <link rel="stylesheet" type="text/css" href="https://cdn.datatables.net/1.11.5/css/jquery.dataTables.css">
    <script type="text/javascript" charset="utf8" src="https://cdn.datatables.net/1.11.5/js/jquery.dataTables.js"></script>
</head>
<body>
    <%
        UserBean currentUser = (UserBean) session.getAttribute("userInfo");
        if (currentUser == null || !"Warehouse Staff".equalsIgnoreCase(currentUser.getRole()) || currentUser.getWarehouseId() == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=WarehouseStaffLoginRequired");
            return;
        }
    %>

    <div class="container">
        <h1>Arrange Delivery (Checkout)</h1>
        <p>This page shows approved reservation quantities grouped by fruit and target country, ready for delivery from your warehouse (ID: <c:out value="${userInfo.warehouseId}"/>).</p>

        <c:if test="${not empty param.message}">
            <div class="message"><c:out value="${param.message}" /></div>
        </c:if>
        <c:if test="${not empty param.error}">
            <div class="error-message"><c:out value="${param.error}" /></div>
        </c:if>
        <c:if test="${not empty errorMessage}">
            <div class="error-message"><c:out value="${errorMessage}" /></div>
        </c:if>

        <table id="deliveryNeedsTable" class="display">
            <thead>
                <tr>
                    <th>Fruit Name</th>
                    <th>Target Country</th>
                    <th>Total Approved Quantity</th>
                    <th>Action (Arrange Delivery)</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="need" items="${deliveryNeedsList}">
                    <tr>
                        <td><c:out value="${need.fruitName}"/> (ID: <c:out value="${need.fruitId}"/>)</td>
                        <td><c:out value="${need.targetCountry}"/></td>
                        <td><c:out value="${need.totalApprovedQuantity}"/></td>
                        <td>
                            <form action="<c:url value='/arrangeDelivery'/>" method="POST" style="display:inline;">
                                <input type="hidden" name="fruitId" value="${need.fruitId}">
                                <input type="hidden" name="targetCountry" value="${need.targetCountry}">
                                <%-- The quantity is calculated server-side based on fruitId and targetCountry --%>
                                <button type="submit" class="checkout-button"
                                        onclick="return confirm('Arrange delivery of ${need.totalApprovedQuantity} units of ${need.fruitName} to ${need.targetCountry}? This will update inventory and reservations.');">
                                    Checkout & Arrange Delivery
                                </button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty deliveryNeedsList}">
                    <tr>
                        <td colspan="4">No approved needs found requiring delivery arrangement from your warehouse.</td>
                    </tr>
                </c:if>
            </tbody>
        </table>
    </div>

    <script>
        $(document).ready( function () {
            $('#deliveryNeedsTable').DataTable({
                 "order": [[ 1, "asc" ], [0, "asc"]] 
            });
        });
    </script>

</body>
</html>
