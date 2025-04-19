<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="menu.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page import="ict.bean.UserBean" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Approve Fruit Needs</title>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <link rel="stylesheet" type="text/css" href="https://cdn.datatables.net/1.11.5/css/jquery.dataTables.css">
    <script type="text/javascript" charset="utf8" src="https://cdn.datatables.net/1.11.5/js/jquery.dataTables.js"></script>
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
        <h1>Approve Pending Fruit Needs for <c:out value="${warehouseCountry}"/></h1>

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

        <table id="needsTable" class="display">
            <thead>
                <tr>
                    <th>Source Country</th>
                    <th>Fruit Name</th>
                    <th>Total Pending Quantity</th>
                    <th>Action</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="need" items="${aggregatedNeedsList}">
                    <tr>
                        <td><c:out value="${need.sourceCountry}"/></td>
                        <td><c:out value="${need.fruitName}"/></td>
                        <td><c:out value="${need.totalNeededQuantity}"/></td>
                        <td>
                            <form action="<c:url value='/needsApproval'/>" method="POST" style="display:inline;">
                                <input type="hidden" name="fruitId" value="${need.fruitId}">
                                <input type="hidden" name="sourceCountry" value="${need.sourceCountry}">
                                <button type="submit" class="approve-button"
                                        onclick="return confirm('Approve all pending reservations for ${need.fruitName} from ${need.sourceCountry}?');">
                                    Approve
                                </button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty aggregatedNeedsList}">
                    <tr>
                        <td colspan="4">No pending needs found for your warehouse's country (<c:out value="${warehouseCountry}"/>).</td>
                    </tr>
                </c:if>
            </tbody>
        </table>

        <a href="javascript:history.back()" class="back-link">Back</a>
    </div>

    <script>
        // Initialize DataTables
        $(document).ready( function () {
            $('#needsTable').DataTable({
                 "order": [[ 1, "asc" ]] // Optional: Default sort by fruit name
            });
        });
    </script>

    <%-- Optional: Include footer --%>
    <%-- <jsp:include page="footer.jsp" /> --%>
</body>
</html>
