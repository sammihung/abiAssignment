<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page import="ict.bean.UserBean" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Stock Overview</title>
    <style>
        html, body {
            margin: 0;
            padding: 0;
        }
        .menu-bar {
            margin: 0;
            padding: 0;
        }
    </style>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <link rel="stylesheet" type="text/css" href="https://cdn.datatables.net/1.11.5/css/jquery.dataTables.css">
    <script type="text/javascript" charset="utf8" src="https://cdn.datatables.net/1.11.5/js/jquery.dataTables.js"></script>
    <style>
        body { font-family: sans-serif; margin: 0px; background-color: #f4f4f4; }
        .container { background-color: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0,0,0,0.1); max-width: 1000px; margin: auto; }
        h1, h2 { color: #333; text-align: center; margin-bottom: 10px; }
        h2 { margin-top: 30px; border-bottom: 1px solid #ccc; padding-bottom: 5px;}
        table { width: 100%; border-collapse: collapse; margin-top: 15px; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; cursor: pointer; }
        tr:nth-child(even) { background-color: #f9f9f9; }
        .error-message { padding: 10px; margin-bottom: 15px; border-radius: 4px; text-align: center; background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
        .back-link { display: block; text-align: center; margin-top: 30px; }
        .dataTables_wrapper { margin-top: 10px; margin-bottom: 20px; }
        .dataTables_filter input { margin-left: 5px;}
    </style>
</head>
<body>
    <jsp:include page="menu.jsp" />

    <%
        UserBean currentUser = (UserBean) session.getAttribute("userInfo");
        if (currentUser == null || (!"Bakery shop staff".equalsIgnoreCase(currentUser.getRole()) && !"Warehouse Staff".equalsIgnoreCase(currentUser.getRole()))) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=StaffLoginRequired");
            return;
        }
        String userRole = currentUser.getRole();
        boolean isShopStaff = "Bakery shop staff".equalsIgnoreCase(userRole);
        boolean isWarehouseStaff = "Warehouse Staff".equalsIgnoreCase(userRole);
    %>

    <div class="container">
        <h1>Stock Overview</h1>
        <p>Viewing stock relevant to your location: <strong><c:out value="${locationName}"/></strong></p>

        <c:if test="${not empty errorMessage}">
            <div class="error-message"><c:out value="${errorMessage}" /></div>
        </c:if>

        <h2>My Location Stock (<c:out value="${locationName}"/>)</h2>
        <table id="ownStockTable" class="display compact">
            <thead><tr><th>Fruit Name</th><th>Quantity</th></tr></thead>
            <tbody>
                <c:forEach var="item" items="${ownStock}">
                    <tr><td><c:out value="${item.fruitName}"/></td><td><c:out value="${item.quantity}"/></td></tr>
                </c:forEach>
                <c:if test="${empty ownStock}"><tr><td colspan="2">No stock data found for your location.</td></tr></c:if>
            </tbody>
        </table>

        <c:if test="${isShopStaff}">
            <h2>Other Shops in <c:out value="${locationCity}"/></h2>
            <table id="otherShopStockTable" class="display compact">
                <thead><tr><th>Shop Name</th><th>Fruit Name</th><th>Quantity</th></tr></thead>
                <tbody>
                    <c:forEach var="item" items="${otherShopStock}">
                        <tr><td><c:out value="${item.locationName}"/></td><td><c:out value="${item.fruitName}"/></td><td><c:out value="${item.quantity}"/></td></tr>
                    </c:forEach>
                    <c:if test="${empty otherShopStock}"><tr><td colspan="3">No stock data found for other shops in your city.</td></tr></c:if>
                </tbody>
            </table>
        </c:if>

        <h2>Central Warehouses in <c:out value="${locationCountry}"/></h2>
        <table id="centralStockTable" class="display compact">
            <thead><tr><th>Warehouse Name</th><th>Fruit Name</th><th>Quantity</th></tr></thead>
            <tbody>
                <c:forEach var="item" items="${centralWarehouseStock}">
                    <tr><td><c:out value="${item.locationName}"/></td><td><c:out value="${item.fruitName}"/></td><td><c:out value="${item.quantity}"/></td></tr>
                </c:forEach>
                <c:if test="${empty centralWarehouseStock}"><tr><td colspan="3">No stock data found for central warehouses in your country.</td></tr></c:if>
            </tbody>
        </table>

    </div>

    <script>
        $(document).ready( function () {
            $('#ownStockTable').DataTable({"paging": false, "info": false, "order": [[ 0, "asc" ]]});
            <c:if test="${isShopStaff}">
                $('#otherShopStockTable').DataTable({"order": [[ 0, "asc" ], [1, "asc"]]});
            </c:if>
            $('#centralStockTable').DataTable({"order": [[ 0, "asc" ], [1, "asc"]]});
        });
    </script>

</body>
</html>
