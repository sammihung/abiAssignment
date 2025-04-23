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
        .error-message { background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
        .back-link { display: block; text-align: center; margin-top: 20px; }
        /* Updated Filter Form Styling */
        .report-filters { margin-bottom: 20px; padding: 15px; background-color: #f0f0f0; border-radius: 5px; display: flex; gap: 15px; align-items: flex-end; /* Align items at bottom */ }
        .report-filters label { margin-right: 5px; font-weight: bold;}
        .report-filters select { padding: 8px; border: 1px solid #ccc; border-radius: 4px; height: 36px; /* Match button height */}
        .report-filters button { padding: 9px 15px; background-color: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer; height: 36px; }
        .report-filters button:hover { background-color: #0056b3; }
        .dataTables_wrapper { margin-top: 20px; }
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
        <c:if test="${not empty errorMessage}">
            <div class="error-message"><c:out value="${errorMessage}" /></div>
        </c:if>

        <%-- Filter Form to select grouping --%>
        <form action="<c:url value='/viewInventoryReport'/>" method="GET" class="report-filters">
            <label for="groupBy">Group By:</label>
            <select id="groupBy" name="groupBy">
                <%-- Options for grouping, pre-select the current one --%>
                <option value="sourceCountry" ${selectedGroupBy == 'sourceCountry' ? 'selected' : ''}>Fruit Source Country</option>
                <option value="shop" ${selectedGroupBy == 'shop' ? 'selected' : ''}>Shop</option>
                <option value="city" ${selectedGroupBy == 'city' ? 'selected' : ''}>City</option>
                <option value="country" ${selectedGroupBy == 'country' ? 'selected' : ''}>Location Country</option>
            </select>
            <button type="submit">Generate Report</button>
        </form>

        <table id="inventoryReportTable" class="display">
            <thead>
                <tr>
                    <%-- Table header label changes based on selected grouping --%>
                    <th>${groupByDimension}</th>
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
                <%-- Message if no data found for the selected grouping --%>
                <c:if test="${empty inventoryReportData}">
                    <tr>
                        <td colspan="3">No inventory data found for this report grouping.</td>
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
                 "order": [[ 0, "asc" ], [1, "asc"]] // Default sort by dimension, then fruit
                 // Add other DataTables options if needed (paging, searching, etc.)
            });
        });
    </script>

</body>
</html>
