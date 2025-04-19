<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page import="ict.bean.UserBean" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${reportTitle}</title>
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
        .report-filters { margin-bottom: 20px; } /* Style filters if added */
        .dataTables_filter, .dataTables_info, .dataTables_paginate { margin-bottom: 15px; }
    </style>
</head>
<body>
    <%-- Optional: Include header/menu --%>
    <%-- <jsp:include page="menu.jsp" /> --%>

    <%-- Basic login & role check --%>
    <%
        UserBean currentUser = (UserBean) session.getAttribute("userInfo");
        if (currentUser == null || !"Senior Management".equalsIgnoreCase(currentUser.getRole())) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=ManagementLoginRequired");
            return;
        }
    %>

    <div class="container">
        <h1>${reportTitle}</h1>

        <%-- Display Messages/Errors --%>
        <c:if test="${not empty param.message}"> <div class="message"><c:out value="${param.message}" /></div> </c:if>
        <c:if test="${not empty param.error}"> <div class="error-message"><c:out value="${param.error}" /></div> </c:if>
        <c:if test="${not empty errorMessage}"> <div class="error-message"><c:out value="${errorMessage}" /></div> </c:if>

        <%-- TODO: Add filter options (e.g., dropdown to select report type: by country, by city) --%>
        <%-- <div class="report-filters"> ... </div> --%>

        <table id="inventoryReportTable" class="display">
            <thead>
                <tr>
                    <th>${groupByDimension}</th> <%-- Dynamic header based on grouping --%>
                    <th>Fruit Name</th>
                    <th>Total Quantity</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="item" items="${inventoryReportData}">
                    <tr>
                        <td><c:out value="${item.groupingDimension}"/></td>
                        <td><c:out value="${item.fruitName}"/> (ID: <c:out value="${item.fruitId}"/>)</td>
                        <td><c:out value="${item.totalQuantity}"/></td>
                    </tr>
                </c:forEach>
                <c:if test="${empty inventoryReportData}">
                    <tr>
                        <td colspan="3">No inventory data found for this report.</td>
                    </tr>
                </c:if>
            </tbody>
        </table>

        <a href="javascript:history.back()" class="back-link">Back</a>
    </div>

    <script>
        // Initialize DataTables
        $(document).ready( function () {
            $('#inventoryReportTable').DataTable({
                 "order": [[ 0, "asc" ], [1, "asc"]] // Optional: Sort by dimension, then fruit
            });
        });
    </script>

</body>
</html>
