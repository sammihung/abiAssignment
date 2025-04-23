<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="menu.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %> <%-- JSTL Formatting library for dates --%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %> <%-- ADDED: JSTL Functions library --%>
<%@ page import="ict.bean.UserBean" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Reservations</title>
    <%-- Include jQuery and DataTables for Sorting/Filtering/Pagination --%>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <link rel="stylesheet" type="text/css" href="https://cdn.datatables.net/1.11.5/css/jquery.dataTables.css">
    <script type="text/javascript" charset="utf8" src="https://cdn.datatables.net/1.11.5/js/jquery.dataTables.js"></script>
    <%-- Basic Styling (Adapt as needed) --%>
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
        .dataTables_filter { margin-bottom: 15px; }
        /* Optional: Style for different statuses */
        .status-pending { color: orange; font-weight: bold; }
        .status-confirmed { color: green; font-weight: bold; }
        .status-borrowed { color: blue; font-weight: bold; } /* If using status for borrowings too */
        .status-cancelled { color: red; text-decoration: line-through; }
        /* Add default style for unknown statuses */
        .status-unknown { color: #555; font-style: italic; }
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
        <h1>My Fruit Reservations</h1>

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

        <table id="reservationsTable" class="display">
            <thead>
                <tr>
                    <th>Res. ID</th>
                    <th>Fruit Name</th>
                    <th>Quantity</th>
                    <th>Date</th>
                    <th>Status</th>
                    <%-- Optional: Add Actions column if needed (e.g., Cancel) --%>
                    <%-- <th>Actions</th> --%>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="res" items="${reservationList}">
                    <tr>
                        <td><c:out value="${res.reservationId}"/></td>
                        <td><c:out value="${res.fruitName}"/></td>
                        <td><c:out value="${res.quantity}"/></td>
                        <td>
                            <%-- Format the date --%>
                            <fmt:formatDate value="${res.reservationDate}" pattern="yyyy-MM-dd" />
                        </td>
                        <td>
                            <%-- Apply optional styling based on status (check for null status) --%>
                            <c:set var="statusClass" value="${res.status != null ? fn:toLowerCase(res.status) : 'unknown'}" />
                            <span class="status-${statusClass}">
                                <c:out value="${res.status != null ? res.status : 'Unknown'}"/>
                            </span>
                        </td>
                        <%-- Optional Actions Cell --%>
                        <%--
                        <td>
                            <c:if test="${res.status == 'Pending'}">
                                <form action="<c:url value='/cancelReservation'/>" method="POST" style="display:inline;">
                                    <input type="hidden" name="reservationId" value="${res.reservationId}">
                                    <button type="submit" onclick="return confirm('Cancel reservation ${res.reservationId}?');">Cancel</button>
                                </form>
                            </c:if>
                        </td>
                        --%>
                    </tr>
                </c:forEach>
                <c:if test="${empty reservationList}">
                    <tr>
                        <td colspan="5">You have no reservation records.</td>
                        <%-- Adjust colspan if Actions column is added --%>
                    </tr>
                </c:if>
            </tbody>
        </table>

        <a href="javascript:history.back()" class="back-link">Back</a>
    </div>

    <script>
        // Initialize DataTables
        $(document).ready( function () {
            $('#reservationsTable').DataTable({
                 "order": [[ 3, "desc" ]] // Optional: Default sort by date descending
            });
        });
    </script>

    <%-- Optional: Include footer --%>
    <%-- <jsp:include page="footer.jsp" /> --%>
</body>
</html>
