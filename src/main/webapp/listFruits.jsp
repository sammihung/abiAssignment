<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %> <%-- JSTL Core library --%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %> <%-- JSTL Functions (Optional, for string manipulation if needed) --%>
<%@ include file="menu.jsp" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Fruit List</title>
    <%-- Include jQuery and DataTables for Sorting/Filtering/Pagination --%>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <link rel="stylesheet" type="text/css" href="https://cdn.datatables.net/1.11.5/css/jquery.dataTables.css">
    <script type="text/javascript" charset="utf8" src="https://cdn.datatables.net/1.11.5/js/jquery.dataTables.js"></script>

    <%-- Basic Styling (Optional - Consider using a shared CSS file) --%>
    <style>
        body { font-family: sans-serif; margin: 20px; }
        .container { max-width: 800px; margin: auto; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; cursor: pointer; } /* Add cursor for sort indication */
        tr:nth-child(even) { background-color: #f9f9f9; }
        .action-button { padding: 5px 10px; margin-right: 5px; text-decoration: none; border-radius: 4px; cursor: pointer; border: none; color: white; }
        .edit-button { background-color: #28a745; } /* Green */
        .edit-button:hover { background-color: #218838; }
        .delete-button { background-color: #dc3545; } /* Red */
        .delete-button:hover { background-color: #c82333; }
        .message, .error-message { padding: 10px; margin-bottom: 15px; border-radius: 4px; }
        .message { color: #155724; background-color: #d4edda; border: 1px solid #c3e6cb; } /* Green */
        .error-message { color: #721c24; background-color: #f8d7da; border: 1px solid #f5c6cb; } /* Red */
        .add-link { margin-bottom: 15px; display: inline-block; padding: 8px 12px; background-color: #007bff; color: white; text-decoration: none; border-radius: 4px; }
        .add-link:hover { background-color: #0056b3; }
        /* DataTables Search Box Styling */
        .dataTables_filter { margin-bottom: 15px; }
    </style>
</head>
<body>

<div class="container">
    <h2>Fruit List</h2>

    <%-- Display messages or errors passed via URL parameters --%>
    <c:if test="${not empty param.message}">
        <div class="message"><c:out value="${param.message}" /></div>
    </c:if>
    <c:if test="${not empty param.error}">
        <div class="error-message"><c:out value="${param.error}" /></div>
    </c:if>
     <%-- Display errors passed via request attribute (e.g., from ListFruitsController itself) --%>
     <c:if test="${not empty errorMessage}">
         <div class="error-message"><c:out value="${errorMessage}" /></div>
     </c:if>

    <%-- Link to add a new fruit --%>
    <a href="<c:url value='/manageFruits'/>" class="add-link">Add New Fruit</a>

    <table id="fruitsTable" class="display">
        <thead>
            <tr>
                <th>ID</th>
                <th>Fruit Name</th>
                <th>Source Country</th>
                <th>Actions</th> <%-- Column for Edit/Delete buttons --%>
            </tr>
        </thead>
        <tbody>
            <%-- Loop through the fruitList attribute set by the ListFruitsController --%>
            <c:forEach var="fruit" items="${fruitList}">
                <tr>
                    <td><c:out value="${fruit.fruitId}"/></td>
                    <td><c:out value="${fruit.fruitName}"/></td>
                    <td><c:out value="${fruit.sourceCountry}"/></td>
                    <td>
                        <%-- Edit Link --%>
                        <a href="<c:url value='/updateFruit?fruitId=${fruit.fruitId}'/>" class="action-button edit-button">Edit</a>

                        <%-- Delete Button (within a form) --%>
                        <form action="<c:url value='/deleteFruit'/>" method="POST" style="display:inline;">
                            <input type="hidden" name="fruitId" value="${fruit.fruitId}">
                            <%-- JavaScript confirmation dialog before submitting --%>
                            <button type="submit" class="action-button delete-button"
                                    onclick="return confirm('Are you sure you want to delete the fruit \'${fn:escapeXml(fruit.fruitName)}\'?');">
                                Delete
                            </button>
                        </form>
                    </td>
                </tr>
            </c:forEach>
            <%-- Handle case where the list is empty --%>
            <c:if test="${empty fruitList}">
                <tr>
                    <td colspan="4">No fruits found.</td>
                </tr>
            </c:if>
        </tbody>
    </table>
</div>

<script>
    // Initialize DataTables on the table
    $(document).ready( function () {
        $('#fruitsTable').DataTable({
            // Optional configurations for DataTables
            // "paging": true,
            // "searching": true,
            // "ordering": true,
            // "info": true
        });
    });
</script>

</body>
</html>
