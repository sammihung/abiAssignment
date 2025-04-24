<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="ict.bean.UserBean" %>

<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Consumption Forecast Report</title>
        <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
        <link rel="stylesheet" type="text/css"
            href="https://cdn.datatables.net/1.13.6/css/jquery.dataTables.min.css">
        <script type="text/javascript" charset="utf8"
            src="https://cdn.datatables.net/1.13.6/js/jquery.dataTables.min.js"></script>

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
        .date-filter-form { margin-bottom: 20px; padding: 15px; background-color: #f0f0f0; border-radius: 5px; display: flex; gap: 15px; align-items: center; flex-wrap: wrap; }
        .date-filter-form label { margin-right: 5px; font-weight: bold;}
        .date-filter-form input[type="date"] { padding: 5px; border: 1px solid #ccc; border-radius: 4px; }
        .date-filter-form button { padding: 6px 12px; background-color: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer; }
        .date-filter-form button:hover { background-color: #0056b3; }
        .dataTables_wrapper { margin-top: 10px; }
        td.number { text-align: right; } 
    </style>
    </head>
    <body>

        <%@ include file="menu.jsp" %>

        <%
        UserBean currentUser = (UserBean) session.getAttribute("userInfo");
        if (currentUser == null ||
        !"Senior Management".equalsIgnoreCase(currentUser.getRole())) {
        response.sendRedirect(request.getContextPath() +
        "/login.jsp?error=ManagementLoginRequired");
        return;
        }
        %>

        <div class="container">
            <h1>Consumption Forecast Report</h1>
            <p>Shows the calculated average daily consumption (based on
                'Fulfilled' reservations) for each fruit per target country
                within the selected period. This can indicate the approximate
                daily quantity needed for delivery to meet demand.</p>

            <c:if test="${not empty errorMessage}">
                <div class="error-message"><c:out
                        value="${errorMessage}" /></div>
            </c:if>

            <form action="<c:url value='/viewForecastReport'/>" method="GET"
                class="date-filter-form">
                <label for="startDate">From:</label>
                <input type="date" id="startDate" name="startDate"
                    value="${selectedStartDate}">
                <label for="endDate">To:</label>
                <input type="date" id="endDate" name="endDate"
                    value="${selectedEndDate}">
                <button type="submit">Generate Forecast</button>
            </form>

            <table id="forecastTable" class="display">
                <thead>
                    <tr>
                        <th>Target Country</th>
                        <th>Fruit Name</th>
                        <th>Avg. Daily Consumption (Forecasted Need)</th>
                    </tr>
                </thead>
                <tbody>
                    <c:foreach var="item" items="${forecastReportData}">
                        <tr>
                            <td><c:out value="${item.targetCountry}" /></td>
                            <td><c:out value="${item.fruitName}" /> (ID: <c:out
                                    value="${item.fruitId}" />)</td>

                            <td class="number"><fmt:formatnumber
                                    value="${item.averageDailyConsumption}"
                                    pattern="#,##0.00" /></td>
                        </tr>
                    </c:foreach>
                    <c:if test="${empty forecastReportData}">
                        <tr>
                            <td colspan="3">No forecast data found for the
                                selected period. Ensure reservations have been
                                marked 'Fulfilled'.</td>
                        </tr>
                    </c:if>
                </tbody>
            </table>

        </div>

        <script>
        
        $(document).ready( function () {
            $('#forecastTable').DataTable({
                 "order": [[ 0, "asc" ], [1, "asc"]] 
                 
            });
        });
    </script>

    </body>
</html>