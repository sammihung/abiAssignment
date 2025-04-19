<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="menu.jsp" %>
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
    <link rel="stylesheet" type="text/css" href="https://cdn.datatables.net/1.11.5/css/jquery.dataTables.css">
    <script type="text/javascript" charset="utf8" src="https://cdn.datatables.net/1.11.5/js/jquery.dataTables.js"></script>
    <style>
        body { font-family: sans-serif; margin: 20px; background-color: #f4f4f4; }
        .container { background-color: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0,0,0,0.1); max-width: 1000px; margin: auto; }
        h1 { color: #333; text-align: center; }
        .filter-form { margin-bottom: 20px; padding: 15px; background-color: #f0f0f0; border-radius: 5px; display: flex; flex-wrap: wrap; gap: 15px; align-items: flex-end; }
        .filter-group { display: flex; flex-direction: column; }
        .filter-group label { margin-bottom: 5px; font-weight: bold; }
        .filter-group select, .filter-group input { padding: 8px; border: 1px solid #ccc; border-radius: 4px; min-width: 150px;}
        .filter-form button { padding: 9px 15px; background-color: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer; height: 36px; /* Align button height */}
        .filter-form button:hover { background-color: #0056b3; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; cursor: pointer; }
        .message, .error-message { padding: 10px; margin-bottom: 15px; border-radius: 4px; text-align: center; }
        .message { background-color: #d4edda; color: #155724; border: 1px solid #c3e6cb; }
        .error-message { background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
        .back-link { display: block; text-align: center; margin-top: 20px; }
        .dataTables_wrapper { margin-top: 20px; }
    </style>
</head>
<body>
    <%-- Optional: Include header/menu --%>
    <%-- <jsp:include page="menu.jsp" /> --%>

    <%-- Basic login & role check --%>
    <%
        UserBean currentUser = (UserBean) session.getAttribute("userInfo");
        if (currentUser == null || !"Senior Management".equalsIgnoreCase(currentUser.getRole())) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=ManagementLoginRequired");
            return;
        }
    %>

    <div class="container">
        <h1>${reportTitle}</h1>

        <%-- Display Messages/Errors --%>
        <c:if test="${not empty param.message}"> <div class="message"><c:out value="${param.message}" /></div> </c:if>
        <c:if test="${not empty param.error}"> <div class="error-message"><c:out value="${param.error}" /></div> </c:if>
        <c:if test="${not empty errorMessage}"> <div class="error-message"><c:out value="${errorMessage}" /></div> </c:if>

        <%-- Filter Form --%>
        <form action="<c:url value='/viewAdvancedReport'/>" method="GET" class="filter-form">
            <div class="filter-group">
                <label for="reportType">Report Type:</label>
                <select id="reportType" name="reportType">
                    <option value="needs" ${selectedReportType == 'needs' ? 'selected' : ''}>Aggregated Needs</option>
                    <option value="seasonalConsumption" ${selectedReportType == 'seasonalConsumption' ? 'selected' : ''}>Seasonal Consumption</option>
                    <%-- Add other report types here --%>
                </select>
            </div>
             <div class="filter-group">
                <label for="filterType">Filter By:</label>
                <select id="filterType" name="filterType">
                    <option value="none" ${selectedFilterType == 'none' ? 'selected' : ''}>None</option>
                    <option value="shop" ${selectedFilterType == 'shop' ? 'selected' : ''}>Shop</option>
                    <option value="city" ${selectedFilterType == 'city' ? 'selected' : ''}>City</option>
                    <option value="country" ${selectedFilterType == 'country' ? 'selected' : ''}>Country</option>
                </select>
            </div>
             <div class="filter-group">
                 <label for="filterValue">Filter Value:</label>
                 <%-- This input changes based on filterType selection (handled by JavaScript below) --%>
                 <select id="filterValueShop" name="filterValueShop" style="display: ${selectedFilterType == 'shop' ? 'block' : 'none'};">
                     <option value="">-- Select Shop --</option>
                     <c:forEach var="shop" items="${allShops}">
                         <%-- Adapt value/text based on BakeryShopBean --%>
                         <option value="${shop.shop_id}" ${selectedFilterType == 'shop' && selectedFilterValue == shop.shop_id ? 'selected' : ''}>
                             <c:out value="${shop.shop_name}" /> (<c:out value="${shop.city}" />)
                         </option>
                     </c:forEach>
                 </select>
                 <select id="filterValueCity" name="filterValueCity" style="display: ${selectedFilterType == 'city' ? 'block' : 'none'};">
                      <option value="">-- Select City --</option>
                     <c:forEach var="city" items="${allCities}">
                         <option value="${city}" ${selectedFilterType == 'city' && selectedFilterValue == city ? 'selected' : ''}>
                             <c:out value="${city}" />
                         </option>
                     </c:forEach>
                 </select>
                 <select id="filterValueCountry" name="filterValueCountry" style="display: ${selectedFilterType == 'country' ? 'block' : 'none'};">
                      <option value="">-- Select Country --</option>
                     <c:forEach var="country" items="${allCountries}">
                         <option value="${country}" ${selectedFilterType == 'country' && selectedFilterValue == country ? 'selected' : ''}>
                             <c:out value="${country}" />
                         </option>
                     </c:forEach>
                 </select>
                 <%-- Hidden input to store the actual filter value based on selection --%>
                 <input type="hidden" id="filterValue" name="filterValue" value="${selectedFilterValue}">
             </div>
            <div class="filter-group">
                <label for="startDate">From:</label>
                <input type="date" id="startDate" name="startDate" value="${selectedStartDate}">
            </div>
            <div class="filter-group">
                <label for="endDate">To:</label>
                <input type="date" id="endDate" name="endDate" value="${selectedEndDate}">
            </div>
            <button type="submit">Generate Report</button>
        </form>

        <%-- Report Data Table --%>
        <table id="reportTable" class="display">
            <thead>
                 <%-- Headers change based on report type --%>
                 <c:choose>
                     <c:when test="${selectedReportType == 'needs'}">
                         <tr>
                             <th>Fruit Name</th>
                             <th>Total Needed Quantity (Pending/Approved)</th>
                         </tr>
                     </c:when>
                     <c:when test="${selectedReportType == 'seasonalConsumption'}">
                          <tr>
                             <th>Season</th>
                             <th>Fruit Name</th>
                             <th>Total Consumed Quantity (Fulfilled)</th>
                         </tr>
                     </c:when>
                     <c:otherwise>
                          <tr><th>Report Data</th></tr> <%-- Default/Error --%>
                     </c:otherwise>
                 </c:choose>
            </thead>
            <tbody>
                 <c:choose>
                     <c:when test="${selectedReportType == 'needs'}">
                         <c:forEach var="item" items="${reportData}">
                             <tr>
                                 <td><c:out value="${item.fruitName}"/></td>
                                 <td><c:out value="${item.totalNeededQuantity}"/></td>
                             </tr>
                         </c:forEach>
                         <c:if test="${empty reportData}"><tr><td colspan="2">No needs data found for the selected filters.</td></tr></c:if>
                     </c:when>
                     <c:when test="${selectedReportType == 'seasonalConsumption'}">
                          <c:forEach var="item" items="${reportData}">
                             <tr>
                                 <td><c:out value="${item.season}"/></td>
                                 <td><c:out value="${item.fruitName}"/></td>
                                 <td><c:out value="${item.totalConsumedQuantity}"/></td>
                             </tr>
                         </c:forEach>
                          <c:if test="${empty reportData}"><tr><td colspan="3">No consumption data found for the selected filters.</td></tr></c:if>
                     </c:when>
                     <c:otherwise>
                          <tr><td>Please select a valid report type.</td></tr>
                     </c:otherwise>
                 </c:choose>
            </tbody>
        </table>

        <a href="javascript:history.back()" class="back-link">Back</a>
    </div>

    <script>
        $(document).ready( function () {
            // Initialize DataTable
            $('#reportTable').DataTable({
                 // Optional configurations like default sorting
                 // "order": [[ 0, "asc" ]]
            });

            // Show/hide filter value dropdown based on filter type
            function toggleFilterValueInput() {
                var filterType = $('#filterType').val();
                var currentFilterValue = $('#filterValue').val(); // Get hidden value before hiding

                $('#filterValueShop').hide().prop('disabled', true);
                $('#filterValueCity').hide().prop('disabled', true);
                $('#filterValueCountry').hide().prop('disabled', true);
                $('#filterValue').val(''); // Clear hidden value initially

                if (filterType === 'shop') {
                    $('#filterValueShop').show().prop('disabled', false);
                     // Try to reselect if type matches previous selection
                     if ('${selectedFilterType}' === 'shop') $('#filterValueShop').val(currentFilterValue || '${selectedFilterValue}');
                     $('#filterValue').val($('#filterValueShop').val()); // Update hidden on load/change
                } else if (filterType === 'city') {
                    $('#filterValueCity').show().prop('disabled', false);
                     if ('${selectedFilterType}' === 'city') $('#filterValueCity').val(currentFilterValue || '${selectedFilterValue}');
                     $('#filterValue').val($('#filterValueCity').val());
                } else if (filterType === 'country') {
                    $('#filterValueCountry').show().prop('disabled', false);
                     if ('${selectedFilterType}' === 'country') $('#filterValueCountry').val(currentFilterValue || '${selectedFilterValue}');
                     $('#filterValue').val($('#filterValueCountry').val());
                }
                 // If filterType is 'none', all dropdowns remain hidden/disabled and hidden value is empty
            }

            // Update hidden input when a filter dropdown changes
             $('#filterValueShop, #filterValueCity, #filterValueCountry').on('change', function() {
                 $('#filterValue').val($(this).val());
             });


            // Call on page load and when filter type changes
            toggleFilterValueInput();
            $('#filterType').on('change', toggleFilterValueInput);
        });
    </script>

</body>
</html>
