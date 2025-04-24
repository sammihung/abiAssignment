<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page import="ict.bean.UserBean" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>All Inventory Records</title>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <link rel="stylesheet" type="text/css" href="https://cdn.datatables.net/1.13.6/css/jquery.dataTables.min.css">
    <script type="text/javascript" charset="utf8" src="https://cdn.datatables.net/1.13.6/js/jquery.dataTables.min.js"></script>
    <style>
        body { font-family: sans-serif; margin: 20px; background-color: #f4f4f4; }
        .container { background-color: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0,0,0,0.1); max-width: 1000px; margin: auto; }
        h1 { color: #333; text-align: center; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; cursor: pointer; }
        tr:nth-child(even) { background-color: #f9f9f9; }
        .message, .error-message { padding: 10px; margin-bottom: 15px; border-radius: 4px; text-align: center; }
        .error-message { background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
        .back-link { display: block; text-align: center; margin-top: 20px; }
        .dataTables_wrapper { margin-top: 20px; }
    </style>
</head>
<body>
    <%-- Optional: Include header/menu --%>
    <%@ include file="menu.jsp" %> <%-- Make sure menu is included --%>

    <%-- Basic login & role check --%>
    <%
        UserBean currentUser = (UserBean) session.getAttribute("userInfo");
        if (currentUser == null || !"Senior Management".equalsIgnoreCase(currentUser.getRole())) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=ManagementLoginRequired");
            return;
        }
    %>

    <div class="container">
        <h1>All Inventory Records</h1>

        <%-- Display Messages/Errors --%>
        <c:if test="${not empty errorMessage}">
            <div class="error-message"><c:out value="${errorMessage}" /></div>
        </c:if>

        <table id="allInventoryTable" class="display">
            <thead>
                <tr>
                    <th>Location (Type)</th>
                    <th>Fruit Name</th>
                    <th>Quantity</th>
                    <th>Inv. ID</th> <%-- Optional --%>
                    <th>Shop ID</th> <%-- Optional --%>
                    <th>Warehouse ID</th> <%-- Optional --%>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="item" items="${inventoryList}">
                    <tr>
                        <td><c:out value="${item.locationName}"/></td>
                        <td><c:out value="${item.fruitName}"/></td>
                        <td><c:out value="${item.quantity}"/></td>
                        <td><c:out value="${item.inventoryId}"/></td>
                        <td><c:out value="${item.shopId != null ? item.shopId : 'N/A'}"/></td>
                        <td><c:out value="${item.warehouseId != null ? item.warehouseId : 'N/A'}"/></td>
                    </tr>
                </c:forEach>
                <c:if test="${empty inventoryList && empty errorMessage}">
                    <tr>
                        <td colspan="6">No inventory records found.</td>
                    </tr>
                </c:if>
            </tbody>
        </table>

     
    </div>

    <script>
        $(document).ready( function () {
            $('#allInventoryTable').DataTable({
                 "order": [[ 0, "asc" ], [1, "asc"]] // Sort by Location, then Fruit
            });
        });
    </script>

</body>
</html>
