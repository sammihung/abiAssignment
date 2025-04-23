<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="menu.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ page import="ict.bean.UserBean" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Checkout Delivery to Shops</title>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <link rel="stylesheet" type="text/css" href="https://cdn.datatables.net/1.11.5/css/jquery.dataTables.css">
    <script type="text/javascript" charset="utf8" src="https://cdn.datatables.net/1.11.5/js/jquery.dataTables.js"></script>
    <style>
        body { font-family: sans-serif; margin: 0px; background-color: #f4f4f4; }
        .container { background-color: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0,0,0,0.1); max-width: 1000px; margin: auto; }
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
        .checkout-button { background-color: #ffc107; color: #333; padding: 5px 10px; border: none; border-radius: 4px; cursor: pointer; font-weight: bold; } /* Yellow */
        .checkout-button:hover { background-color: #e0a800; }
         /* Status styling */
        .status-approved { color: green; font-weight: bold; }
        .status-shipped { color: blue; font-weight: bold; }
        .status-unknown { color: #555; font-style: italic; }
    </style>
</head>
<body>
    <%-- Optional: Include header --%>
    <%-- <jsp:include page="header.jsp" /> --%>

    <%-- Basic login & role check --%>
    <%
        UserBean currentUser = (UserBean) session.getAttribute("userInfo");
        if (currentUser == null || !"Warehouse Staff".equalsIgnoreCase(currentUser.getRole()) || currentUser.getWarehouseId() == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=WarehouseStaffLoginRequired");
            return;
        }
    %>

    <div class="container">
        <h1>Checkout Delivery to Shops</h1>
        <p>Dispatch items from your warehouse (ID: <c:out value="${userInfo.warehouseId}"/>) to fulfill approved/shipped reservations.</p>

        <%-- Display Messages/Errors --%>
        <c:if test="${not empty param.message}">
            <div class="message"><c:out value="${param.message}" /></div>
        </c:if>
        <c:if test="${not empty param.error}">
            <div class="error-message"><c:out value="${param.error}" /></div>
        </c:if>
        <c:if test="${not empty errorMessage}">
            <div class="error-message"><c:out value="${errorMessage}" /></div>
        </c:if>

        <table id="checkoutTable" class="display">
            <thead>
                <tr>
                    <th>Res. ID</th>
                    <th>Fruit</th>
                    <th>Destination Shop</th>
                    <th>Quantity</th>
                    <th>Status</th>
                    <th>Action</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="res" items="${fulfillableList}">
                    <tr>
                        <td><c:out value="${res.reservationId}"/></td>
                        <td><c:out value="${res.fruitName}"/></td>
                        <td><c:out value="${res.shopName}"/></td>
                        <td><c:out value="${res.quantity}"/></td>
                        <td>
                             <c:set var="statusClass" value="${res.status != null ? fn:toLowerCase(res.status) : 'unknown'}" />
                             <span class="status-${statusClass}">
                                <c:out value="${res.status != null ? res.status : 'Unknown'}"/>
                            </span>
                        </td>
                        <td>
                            <form action="<c:url value='/checkoutToShop'/>" method="POST" style="display:inline;">
                                <input type="hidden" name="reservationId" value="${res.reservationId}">
                                <button type="submit" class="checkout-button"
                                        onclick="return confirm('Checkout Reservation ${res.reservationId} (${res.quantity} x ${res.fruitName} to ${res.shopName})? This updates inventory and status.');">
                                    Checkout to Shop
                                </button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty fulfillableList && empty errorMessage}">
                    <tr>
                        <td colspan="6">No reservations found ready for checkout from this warehouse.</td>
                    </tr>
                </c:if>
            </tbody>
        </table>

        <a href="javascript:history.back()" class="back-link">Back</a>
    </div>

    <script>
        // Initialize DataTables
        $(document).ready( function () {
            $('#checkoutTable').DataTable({
                 "order": [[ 2, "asc" ],[1, "asc"]] // Optional: Sort by Shop, then Fruit
            });
        });
    </script>

    <%-- Optional: Include footer --%>
    <%-- <jsp:include page="footer.jsp" /> --%>
</body>
</html>
