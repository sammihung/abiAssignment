<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="menu.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="ict.bean.UserBean" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Approve Borrow Requests</title>
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
        .dataTables_wrapper { margin-top: 20px; }
        .action-button { padding: 5px 10px; border: none; border-radius: 4px; cursor: pointer; margin-right: 5px; color: white; }
        .approve-btn { background-color: #28a745; } 
        .reject-btn { background-color: #dc3545; }
        .action-button:hover { opacity: 0.9; }
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
        <h1>Approve Incoming Borrow Requests</h1>
        <p>Review requests from other shops wanting to borrow items from your shop (ID: <c:out value="${userInfo.shopId}"/>).</p>

        <c:if test="${not empty param.message}"> <div class="message"><c:out value="${param.message}" /></div> </c:if>
        <c:if test="${not empty param.error}"> <div class="error-message"><c:out value="${param.error}" /></div> </c:if>
        <c:if test="${not empty errorMessage}"> <div class="error-message"><c:out value="${errorMessage}" /></div> </c:if>

        <table id="approveBorrowTable" class="display">
            <thead>
                <tr>
                    <th>Request ID</th>
                    <th>Requesting Shop</th>
                    <th>Fruit</th>
                    <th>Quantity Requested</th>
                    <th>Request Date</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="req" items="${pendingBorrowRequests}">
                    <tr>
                        <td><c:out value="${req.borrowingId}"/></td>
                        <td><c:out value="${req.receivingShopName}"/> (ID: <c:out value="${req.receivingShopId}"/>)</td>
                        <td><c:out value="${req.fruitName}"/></td>
                        <td><c:out value="${req.quantity}"/></td>
                        <td><fmt:formatDate value="${req.borrowingDate}" pattern="yyyy-MM-dd" /></td>
                        <td>
                            <form action="<c:url value='/approveBorrow'/>" method="POST" style="display:inline;">
                                <input type="hidden" name="borrowingId" value="${req.borrowingId}">
                                <input type="hidden" name="action" value="approve">
                                <button type="submit" class="action-button approve-btn"
                                        onclick="return confirm('Approve request ${req.borrowingId} to lend ${req.quantity} x ${req.fruitName} to ${req.receivingShopName}?');">
                                    Approve
                                </button>
                            </form>
                            <form action="<c:url value='/approveBorrow'/>" method="POST" style="display:inline;">
                                <input type="hidden" name="borrowingId" value="${req.borrowingId}">
                                <input type="hidden" name="action" value="reject">
                                <button type="submit" class="action-button reject-btn"
                                        onclick="return confirm('Reject borrow request ${req.borrowingId}?');">
                                    Reject
                                </button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty pendingBorrowRequests && empty errorMessage}">
                    <tr>
                        <td colspan="6">No pending borrow requests found for your shop.</td>
                    </tr>
                </c:if>
            </tbody>
        </table>
    </div>

    <script>
        $(document).ready( function () {
            $('#approveBorrowTable').DataTable({
                 "order": [[ 4, "asc" ]] 
            });
        });
    </script>

</body>
</html>
