<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ page import="ict.bean.UserBean" %>
<%@ include file="menu.jsp" %>
<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Consumption Report</title>
        <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
        <link rel="stylesheet" type="text/css"
            href="https://cdn.datatables.net/1.13.6/css/jquery.dataTables.min.css">
        <script type="text/javascript" charset="utf8"
            src="https://cdn.datatables.net/1.13.6/js/jquery.dataTables.min.js"></script>

        <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
        <style>
        body { font-family: sans-serif; margin: 20px; background-color: #f4f4f4; }
        .container { background-color: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0,0,0,0.1); max-width: 900px; margin: auto; }
        h1 { color: #333; text-align: center; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; margin-bottom: 30px; } 
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; cursor: pointer; }
        tr:nth-child(even) { background-color: #f9f9f9; }
        .message, .error-message { padding: 10px; margin-bottom: 15px; border-radius: 4px; text-align: center; }
        .message.success { background-color: #d4edda; color: #155724; border: 1px solid #c3e6cb; }
        .message.error { background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
        .back-link { display: block; text-align: center; margin-top: 20px; }
        .date-filter-form { margin-bottom: 20px; padding: 15px; background-color: #f0f0f0; border-radius: 5px; display: flex; gap: 15px; align-items: center; flex-wrap: wrap; }
        .date-filter-form label { margin-right: 5px; font-weight: bold;}
        .date-filter-form input[type="date"] { padding: 5px; border: 1px solid #ccc; border-radius: 4px; }
        .date-filter-form button { padding: 6px 12px; background-color: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer; }
        .date-filter-form button:hover { background-color: #0056b3; }
        .dataTables_wrapper { margin-top: 10px; }
        
        .chart-container {
             position: relative;
             margin: auto;
             margin-top: 30px; 
             height: 60vh; 
             width: 80vw; 
             max-width: 800px; 
             border: 1px solid #ddd;
             padding: 15px;
             border-radius: 5px;
             background-color: #fff;
        }
    </style>
    </head>
    <body>

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
            <h1>Fruit Consumption Report</h1>

            <c:if test="${not empty param.message}"> <div
                    class="message success"><c:out
                        value="${param.message}" /></div> </c:if>
            <c:if test="${not empty param.error}"> <div
                    class="message error"><c:out value="${param.error}" /></div>
            </c:if>
            <c:if test="${not empty errorMessage}"> <div
                    class="message error"><c:out
                        value="${errorMessage}" /></div> </c:if>

            <form action="<c:url value='/viewConsumptionReport'/>" method="GET"
                class="date-filter-form">
                <label for="startDate">From:</label>
                <input type="date" id="startDate" name="startDate"
                    value="${selectedStartDate}">
                <label for="endDate">To:</label>
                <input type="date" id="endDate" name="endDate"
                    value="${selectedEndDate}">
                <button type="submit">Apply Filter</button>
            </form>

            <div class="chart-container">
                <canvas id="consumptionChartCanvas"></canvas>
            </div>

            <h2>Data Table</h2>
            <table id="consumptionTable" class="display">
                <thead>
                    <tr>
                        <th>Fruit Name</th>
                        <th>Total Quantity Consumed (Fulfilled)</th>
                    </tr>
                </thead>
                <tbody>
                    <c:foreach var="item" items="${consumptionReportData}">
                        <tr>
                            <td><c:out value="${item.itemName}" /></td>
                            <td><c:out
                                    value="${item.totalConsumedQuantity}" /></td>
                        </tr>
                    </c:foreach>
                    <c:if test="${empty consumptionReportData}">
                        <tr>
                            <td colspan="2">No consumption data found for the
                                selected period.</td>
                        </tr>
                    </c:if>
                </tbody>
            </table>

        </div>

        <script
            src="https://cdn.datatables.net/1.13.6/js/jquery.dataTables.min.js"></script>

        <script>
    
        const reportData = [];
        <c:forEach var="item" items="${consumptionReportData}">
            
            reportData.push({
                label: "${fn:escapeXml(item.itemName)}", 
                value: ${item.totalConsumedQuantity}
            });
        </c:forEach>

        const labels = reportData.map(item => item.label);
        const dataValues = reportData.map(item => item.value);

        
        const ctx = document.getElementById('consumptionChartCanvas').getContext('2d');

        
        const consumptionChart = new Chart(ctx, {
            type: 'bar', 
            data: {
                labels: labels, 
                datasets: [{
                    label: 'Total Quantity Consumed', 
                    data: dataValues, 
                    backgroundColor: 'rgba(54, 162, 235, 0.6)', 
                    borderColor: 'rgba(54, 162, 235, 1)', 
                    borderWidth: 1
                }]
            },
            options: {
                responsive: true, 
                maintainAspectRatio: false, 
                scales: {
                    y: {
                        beginAtZero: true, 
                        title: {
                             display: true,
                             text: 'Quantity' 
                         }
                    },
                    x: {
                         title: {
                             display: true,
                             text: 'Fruit' 
                         }
                    }
                },
                plugins: {
                    legend: {
                        display: true, 
                        position: 'top',
                    },
                    title: {
                        display: true,
                        text: 'Fruit Consumption (${selectedStartDate} to ${selectedEndDate})' 
                    }
                }
            }
        });

        
        $(document).ready( function () {
            $('#consumptionTable').DataTable({
                "order": [[ 1, "desc" ]] 
            });
        });
    </script>

    </body>
</html>