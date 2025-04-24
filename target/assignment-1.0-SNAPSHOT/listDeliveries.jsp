<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="ict.bean.UserBean" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${listTitle}</title>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <link rel="stylesheet" type="text/css" href="https://cdn.datatables.net/1.11.5/css/jquery.dataTables.css">
    <script type="text/javascript" charset="utf8" src="https://cdn.datatables.net/1.11.5/js/jquery.dataTables.js"></script>
    <style>
        body { font-family: sans-serif; margin: 20px; background-color: #f4f4f4; }
        .container { background-color: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0,0,0,0.1); max-width: 1100px; margin: auto; }
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
    </style>
</head>
<body>
     <jsp:include page="menu.jsp" />
    <%
        UserBean currentUser = (UserBean) session.getAttribute("userInfo");
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=LoginRequired");
            return;
        }
    %>

    <div class="container">
        <h1>${listTitle}</h1>

        <%-- Display Messages/Errors --%>
        <c:if test="${not empty errorMessage}">
            <div class="error-message"><c:out value="${errorMessage}" /></div>
        </c:if>

        <table id="deliveriesTable" class="display">
            <thead>
                <tr>
                    <th>Delivery ID</th>
                    <th>Fruit</th>
                    <th>From Warehouse</th>
                    <th>To Warehouse</th>
                    <th>Quantity</th>
                    <th>Date</th>
                    <th>Status</th>
                    <%-- Add Actions like 'Mark Received' if needed later --%>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="delivery" items="${deliveryList}">
                    <tr>
                        <td><c:out value="${delivery.deliveryId}"/></td>
                        <td><c:out value="${delivery.fruitName}"/></td>
                        <td><c:out value="${delivery.fromWarehouseName}"/></td>
                        <td><c:out value="${delivery.toWarehouseName}"/></td>
                        <td><c:out value="${delivery.quantity}"/></td>
                        <td><fmt:formatDate value="${delivery.deliveryDate}" pattern="yyyy-MM-dd" /></td>
                        <td><c:out value="${delivery.status}"/></td>
                    </tr>
                </c:forEach>
                <c:if test="${empty deliveryList && empty errorMessage}">
                    <tr>
                        <td colspan="7">No delivery records found.</td>
                    </tr>
                </c:if>
            </tbody>
        </table>

       
    </div>

    <script>
        $(document).ready( function () {
            $('#deliveriesTable').DataTable({
                 "order": [[ 5, "desc" ]] 
            });
        });
    </script>

</body>
</html>
