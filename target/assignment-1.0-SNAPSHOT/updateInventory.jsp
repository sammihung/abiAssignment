<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page import="ict.bean.UserBean" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Update Shop Inventory</title>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <link rel="stylesheet" type="text/css" href="https://cdn.datatables.net/1.11.5/css/jquery.dataTables.css">
    <script type="text/javascript" charset="utf8" src="https://cdn.datatables.net/1.11.5/js/jquery.dataTables.js"></script>
    <style>
        body { font-family: sans-serif; margin: 20px; background-color: #f4f4f4; }
        .container { background-color: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0,0,0,0.1); max-width: 800px; margin: auto; }
        h1, h2 { color: #333; text-align: center; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; margin-bottom: 30px; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
        tr:nth-child(even) { background-color: #f9f9f9; }
        .update-form { padding: 20px; border: 1px solid #ccc; border-radius: 5px; background-color: #f9f9f9; }
        .form-group { margin-bottom: 15px; }
        label { display: block; margin-bottom: 5px; font-weight: bold; }
        select, input[type="number"] { width: 95%; padding: 8px; border: 1px solid #ccc; border-radius: 4px; box-sizing: border-box; }
        button { background-color: #007bff; color: white; padding: 10px 15px; border: none; border-radius: 4px; cursor: pointer; }
        button:hover { background-color: #0056b3; }
        .message, .error-message { padding: 10px; margin-bottom: 15px; border-radius: 4px; text-align: center; }
        .message { background-color: #d4edda; color: #155724; border: 1px solid #c3e6cb; }
        .error-message { background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
        .back-link { display: block; text-align: center; margin-top: 20px; }
        .dataTables_filter, .dataTables_info, .dataTables_paginate { margin-bottom: 15px; }
    </style>
</head>
<body>
    <%-- Optional: Include header --%>
    <%-- <jsp:include page="header.jsp" /> --%>

    <%-- Basic login check --%>
    <%
        UserBean currentUser = (UserBean) session.getAttribute("userInfo");
        if (currentUser == null || currentUser.getShopId() == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=ShopLoginRequired");
            return;
        }
    %>

    <div class="container">
        <h1>Update Shop Inventory</h1>

        <%-- Display Messages/Errors from Redirect --%>
        <c:if test="${not empty param.message}">
            <div class="message"><c:out value="${param.message}" /></div>
        </c:if>
        <c:if test="${not empty param.error}">
            <div class="error-message"><c:out value="${param.error}" /></div>
        </c:if>
         <%-- Display Errors from Forward (e.g., page load error) --%>
         <c:if test="${not empty errorMessage}">
            <div class="error-message"><c:out value="${errorMessage}" /></div>
        </c:if>

        <%-- Section 1: Display Current Inventory --%>
        <h2>Current Stock Levels</h2>
        <table id="inventoryTable" class="display">
            <thead>
                <tr>
                    <th>Fruit Name</th>
                    <th>Current Quantity</th>
                    <%-- Optional: Add Inventory ID if needed --%>
                    <%-- <th>Inv. ID</th> --%>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="item" items="${inventoryList}">
                    <tr>
                        <td><c:out value="${item.fruitName}"/></td>
                        <td><c:out value="${item.quantity}"/></td>
                        <%-- <td><c:out value="${item.inventoryId}"/></td> --%>
                    </tr>
                </c:forEach>
                <c:if test="${empty inventoryList}">
                    <tr>
                        <td colspan="2">No inventory records found for this shop.</td>
                         <%-- Adjust colspan if more columns added --%>
                    </tr>
                </c:if>
            </tbody>
        </table>

        <%-- Section 2: Form to Update/Add Inventory --%>
        <h2>Set Inventory Level</h2>
        <div class="update-form">
            <form action="<c:url value='/updateInventory'/>" method="POST">
                <div class="form-group">
                    <label for="fruitId">Select Fruit:</label>
                    <select id="fruitId" name="fruitId" required>
                        <option value="">-- Select a Fruit --</option>
                        <c:forEach var="fruit" items="${allFruits}">
                            <option value="${fruit.fruitId}">
                                <c:out value="${fruit.fruitName}"/> (<c:out value="${fruit.sourceCountry}"/>)
                            </option>
                        </c:forEach>
                    </select>
                </div>
                <div class="form-group">
                    <label for="newQuantity">Set New Quantity:</label>
                    <input type="number" id="newQuantity" name="newQuantity" min="0" required placeholder="Enter the total quantity">
                </div>
                <button type="submit">Update/Set Quantity</button>
            </form>
        </div>

        <a href="javascript:history.back()" class="back-link">Back</a>

    </div>

    <script>
        // Initialize DataTables for the inventory table
        $(document).ready( function () {
            $('#inventoryTable').DataTable({
                // Optional configurations
                 "order": [[ 0, "asc" ]] // Sort by fruit name ascending
            });
        });
    </script>

    <%-- Optional: Include footer --%>
    <%-- <jsp:include page="footer.jsp" /> --%>
</body>
</html>
