<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ page import="ict.bean.UserBean" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Advanced Reports</title>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <link rel="stylesheet" type="text/css" href="https://cdn.datatables.net/1.13.6/css/jquery.dataTables.min.css">
    <script type="text/javascript" charset="utf8" src="https://cdn.datatables.net/1.13.6/js/jquery.dataTables.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        body { font-family: sans-serif; margin: 20px; background-color: #f4f4f4; }
        .container { background-color: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0,0,0,0.1); max-width: 1000px; margin: auto; }
        h1 { color: #333; text-align: center; }
        
        table { width: 100%; border-collapse: collapse; margin-top: 20px; margin-bottom: 30px; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; cursor: pointer; }
        .message, .error-message { padding: 10px; margin-bottom: 15px; border-radius: 4px; text-align: center; }
        .message { background-color: #d4edda; color: #155724; border: 1px solid #c3e6cb; }
        .error-message { background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
        .back-link { display: block; text-align: center; margin-top: 20px; }
        .dataTables_wrapper { margin-top: 20px; }
        .chart-container { position: relative; margin: auto; margin-top: 30px; margin-bottom: 30px; height: 60vh; width: 80vw; max-width: 800px; border: 1px solid #ddd; padding: 15px; border-radius: 5px; background-color: #fff; }
        
        .report-type-form { margin-bottom: 20px; padding: 10px 0; }
        .report-type-form label { font-weight: bold; margin-right: 10px; }
        .report-type-form select { padding: 8px; border: 1px solid #ccc; border-radius: 4px; }
        .report-type-form button { padding: 9px 15px; background-color: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer; margin-left: 10px;}
        .report-type-form button:hover { background-color: #0056b3; }
    </style>
</head>
<body>
    <%@ include file="menu.jsp" %>

    
    <%
        UserBean currentUser = (UserBean) session.getAttribute("userInfo");
        if (currentUser == null || !"Senior Management".equalsIgnoreCase(currentUser.getRole())) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=ManagementLoginRequired");
            return;
        }
    %>

    <div class="container">
        <h1>${reportTitle}</h1>

        
        <c:if test="${not empty param.message}"> <div class="message success"><c:out value="${param.message}" /></div> </c:if>
        <c:if test="${not empty param.error}"> <div class="error-message"><c:out value="${param.error}" /></div> </c:if>
        <c:if test="${not empty errorMessage}"> <div class="error-message"><c:out value="${errorMessage}" /></div> </c:if>

        
        <form action="<c:url value='/viewAdvancedReport'/>" method="GET" class="report-type-form">
            <label for="reportType">Report Type:</label>
            <select id="reportType" name="reportType">
                <option value="needs" ${selectedReportType == 'needs' ? 'selected' : ''}>Aggregated Needs</option>
                <option value="seasonalConsumption" ${selectedReportType == 'seasonalConsumption' ? 'selected' : ''}>Seasonal Consumption</option>
                
            </select>
            <button type="submit">Generate Report</button>
        </form>

        
        <c:if test="${not empty reportData}">
            <c:choose>
                <c:when test="${selectedReportType == 'needs'}">
                    <div class="chart-container">
                         <canvas id="needsChartCanvas"></canvas>
                    </div>
                </c:when>
                <c:when test="${selectedReportType == 'seasonalConsumption'}">
                     <div class="chart-container">
                         <canvas id="seasonalChartCanvas"></canvas>
                    </div>
                </c:when>
            </c:choose>
        </c:if>

        
        <h2>Data Table</h2>
        <table id="reportTable" class="display">
             <thead>
                 <c:choose>
                     <c:when test="${selectedReportType == 'needs'}">
                         <tr><th>Fruit Name</th><th>Total Needed Quantity (Pending/Approved)</th></tr>
                     </c:when>
                     <c:when test="${selectedReportType == 'seasonalConsumption'}">
                          <tr><th>Season</th><th>Fruit Name</th><th>Total Consumed Quantity (Fulfilled)</th></tr>
                     </c:when>
                     <c:otherwise><tr><th>Report Data</th></tr></c:otherwise>
                 </c:choose>
            </thead>
            <tbody>
                 <c:choose>
                     <c:when test="${selectedReportType == 'needs'}">
                         <c:forEach var="item" items="${reportData}">
                             <tr><td><c:out value="${item.fruitName}"/></td><td><c:out value="${item.totalNeededQuantity}"/></td></tr>
                         </c:forEach>
                         <c:if test="${empty reportData}"><tr><td colspan="2">No needs data found.</td></tr></c:if>
                     </c:when>
                     <c:when test="${selectedReportType == 'seasonalConsumption'}">
                          <c:forEach var="item" items="${reportData}">
                              <tr><td><c:out value="${item.season}"/></td><td><c:out value="${item.fruitName}"/></td><td><c:out value="${item.totalConsumedQuantity}"/></td></tr>
                         </c:forEach>
                          <c:if test="${empty reportData}"><tr><td colspan="3">No consumption data found.</td></tr></c:if>
                     </c:when>
                     <c:otherwise><tr><td>Please select a valid report type.</td></tr></c:otherwise>
                 </c:choose>
            </tbody>
        </table>

        <a href="javascript:history.back()" class="back-link">Back</a>
    </div>

    <script>
        $(document).ready( function () {
            
            $('#reportTable').DataTable({});

            
            const reportType = "${selectedReportType}";
            const chartData = []; 

            
            <c:if test="${not empty reportData}">
                <c:forEach var="item" items="${reportData}">
                    chartData.push({
                        <c:if test="${selectedReportType == 'needs'}">
                            label: "${fn:escapeXml(item.fruitName)}", value: ${item.totalNeededQuantity}
                        </c:if>
                        <c:if test="${selectedReportType == 'seasonalConsumption'}">
                            season: "${fn:escapeXml(item.season)}", fruit: "${fn:escapeXml(item.fruitName)}", value: ${item.totalConsumedQuantity}
                        </c:if>
                    });
                </c:forEach>
            </c:if>

            
            if (reportType === 'needs' && chartData.length > 0 && typeof Chart !== 'undefined') {
                const labels = chartData.map(item => item.label);
                const dataValues = chartData.map(item => item.value);
                const ctx = document.getElementById('needsChartCanvas').getContext('2d');
                new Chart(ctx, {
                    type: 'bar',
                    data: { labels: labels, datasets: [{ label: 'Total Needed Quantity (Pending/Approved)', data: dataValues, backgroundColor: 'rgba(255, 159, 64, 0.6)', borderColor: 'rgba(255, 159, 64, 1)', borderWidth: 1 }] },
                    options: { responsive: true, maintainAspectRatio: false, scales: { y: { beginAtZero: true, title: { display: true, text: 'Quantity' } }, x: { title: { display: true, text: 'Fruit' } } }, plugins: { legend: { display: true, position: 'top' }, title: { display: true, text: 'Aggregated Needs (All Time)' } } } 
                });
            } else if (reportType === 'seasonalConsumption' && chartData.length > 0 && typeof Chart !== 'undefined') {
                const seasons = ['Spring', 'Summer', 'Autumn', 'Winter'];
                const fruits = [...new Set(chartData.map(item => item.fruit))];
                const fruitColors = ['rgba(255, 99, 132, 0.6)', 'rgba(54, 162, 235, 0.6)', 'rgba(255, 206, 86, 0.6)', 'rgba(75, 192, 192, 0.6)', 'rgba(153, 102, 255, 0.6)', 'rgba(255, 159, 64, 0.6)', 'rgba(199, 199, 199, 0.6)', 'rgba(83, 102, 255, 0.6)'];
                const chartDatasets = fruits.map((fruit, index) => ({
                    label: fruit,
                    data: seasons.map(season => chartData.find(item => item.season === season && item.fruit === fruit)?.value || 0),
                    backgroundColor: fruitColors[index % fruitColors.length],
                    borderColor: fruitColors[index % fruitColors.length].replace('0.6', '1'),
                    borderWidth: 1
                }));
                const ctx = document.getElementById('seasonalChartCanvas').getContext('2d');
                new Chart(ctx, {
                    type: 'bar',
                    data: { labels: seasons, datasets: chartDatasets },
                    options: { responsive: true, maintainAspectRatio: false, scales: { y: { beginAtZero: true, title: { display: true, text: 'Quantity Consumed' } }, x: { title: { display: true, text: 'Season' } } }, plugins: { legend: { display: true, position: 'top' }, title: { display: true, text: 'Seasonal Consumption (All Time)' } } } 
                });
            }

            

        });
    </script>

</body>
</html>