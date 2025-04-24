<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="menu.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page import="ict.bean.UserBean" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Order Fruits From Source</title>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <link rel="stylesheet" type="text/css" href="https://cdn.datatables.net/1.11.5/css/jquery.dataTables.css">
    <script type="text/javascript" charset="utf8" src="https://cdn.datatables.net/1.11.5/js/jquery.dataTables.js"></script>
    <style>
        body { font-family: sans-serif; margin: 0px; background-color: #f4f4f4; }
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
        .dataTables_wrapper { margin-top: 20px; }
        input[type="number"] { width: 70px; padding: 5px; text-align: right; border: 1px solid #ccc; border-radius: 4px;}
        .submit-button { display: block; width: 150px; margin: 20px auto; padding: 12px; background-color: #28a745; color: white; border: none; border-radius: 5px; font-size: 16px; cursor: pointer; text-align: center; }
        .submit-button:hover { background-color: #218838; }
        .low-stock { color: orange; font-weight: bold; }
        .no-stock { color: red; font-weight: bold; }
    </style>
</head>
<body>

    <%
        UserBean currentUser = (UserBean) session.getAttribute("userInfo");
        if (currentUser == null || !"Bakery shop staff".equalsIgnoreCase(currentUser.getRole()) || currentUser.getShopId() == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=ShopStaffLoginRequired");
            return;
        }
    %>

    <div class="container">
        <h1>Order Fruits From Source</h1>
        <p>Enter the quantity you wish to reserve for each fruit. Stock levels shown are at the primary source warehouse.</p>

        <c:if test="${not empty param.message}"> <div class="message"><c:out value="${param.message}" /></div> </c:if>
        <c:if test="${not empty param.error}"> <div class="error-message"><c:out value="${param.error}" /></div> </c:if>
        <c:if test="${not empty errorMessage}"> <div class="error-message"><c:out value="${errorMessage}" /></div> </c:if>

        <form action="<c:url value='/orderFromSource'/>" method="POST">
            <table id="orderTable" class="display">
                <thead>
                    <tr>
                        <th>Fruit Name</th>
                        <th>Source Country</th>
                        <th>Available Qty (Source)</th>
                        <th>Order Quantity</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="fruit" items="${orderableFruits}">
                        <tr>
                            <td><c:out value="${fruit.fruitName}"/> (ID: <c:out value="${fruit.fruitId}"/>)</td>
                            <td><c:out value="${fruit.sourceCountry}"/></td>
                            <td>
                                <c:choose>
                                    <c:when test="${fruit.availableSourceQuantity <= 0}">
                                        <span class="no-stock">0</span>
                                    </c:when>
                                    <c:when test="${fruit.availableSourceQuantity < 10}"> 
                                        <span class="low-stock"><c:out value="${fruit.availableSourceQuantity}"/></span>
                                    </c:when>
                                    <c:otherwise>
                                        <c:out value="${fruit.availableSourceQuantity}"/>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <input type="number" name="quantity_${fruit.fruitId}" min="0" value="0"
                                       ${fruit.availableSourceQuantity <= 0 ? 'disabled' : ''}> 
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty orderableFruits && empty errorMessage}">
                        <tr>
                            <td colspan="4">No fruits available for ordering.</td>
                        </tr>
                    </c:if>
                </tbody>
            </table>

            <button type="submit" class="submit-button">Submit Order</button>
        </form>
    </div>
    <script>
        $(document).ready( function () {
            $('#orderTable').DataTable({
                "paging": false, 
                "order": [[ 0, "asc" ]] 
            });
        });
    </script>

</body>
</html>
