<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %> 
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %> 

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Fruit List</title>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <link rel="stylesheet" type="text/css" href="https://cdn.datatables.net/1.11.5/css/jquery.dataTables.css">
    <script type="text/javascript" charset="utf8" src="https://cdn.datatables.net/1.11.5/js/jquery.dataTables.js"></script>
    <style>
        body { font-family: sans-serif; margin: 20px; }
        .container { max-width: 800px; margin: auto; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; cursor: pointer; } 
        tr:nth-child(even) { background-color: #f9f9f9; }
        .action-button { padding: 5px 10px; margin-right: 5px; text-decoration: none; border-radius: 4px; cursor: pointer; border: none; color: white; }
        .edit-button { background-color: #28a745; } 
        .edit-button:hover { background-color: #218838; }
        .delete-button { background-color: #dc3545; } 
        .delete-button:hover { background-color: #c82333; }
        .message, .error-message { padding: 10px; margin-bottom: 15px; border-radius: 4px; }
        .message { color: #155724; background-color: #d4edda; border: 1px solid #c3e6cb; } 
        .error-message { color: #721c24; background-color: #f8d7da; border: 1px solid #f5c6cb; } 
        .add-link { margin-bottom: 15px; display: inline-block; padding: 8px 12px; background-color: #007bff; color: white; text-decoration: none; border-radius: 4px; }
        .add-link:hover { background-color: #0056b3; }
        .dataTables_filter { margin-bottom: 15px; }
    </style>
</head>
<body>
    <jsp:include page="menu.jsp" />
<div class="container">
    <h2>Fruit List</h2>
    <c:if test="${not empty param.message}">
        <div class="message"><c:out value="${param.message}" /></div>
    </c:if>
    <c:if test="${not empty param.error}">
        <div class="error-message"><c:out value="${param.error}" /></div>
    </c:if>
     <c:if test="${not empty errorMessage}">
         <div class="error-message"><c:out value="${errorMessage}" /></div>
     </c:if>

    <a href="<c:url value='/manageFruits'/>" class="add-link">Add New Fruit</a>

    <table id="fruitsTable" class="display">
        <thead>
            <tr>
                <th>ID</th>
                <th>Fruit Name</th>
                <th>Source Country</th>
                <th>Actions</th> 
            </tr>
        </thead>
        <tbody>
            <c:forEach var="fruit" items="${fruitList}">
                <tr>
                    <td><c:out value="${fruit.fruitId}"/></td>
                    <td><c:out value="${fruit.fruitName}"/></td>
                    <td><c:out value="${fruit.sourceCountry}"/></td>
                    <td>
                        <a href="<c:url value='/updateFruit?fruitId=${fruit.fruitId}'/>" class="action-button edit-button">Edit</a>
                        <form action="<c:url value='/deleteFruit'/>" method="POST" style="display:inline;">
                            <input type="hidden" name="fruitId" value="${fruit.fruitId}">
                            <button type="submit" class="action-button delete-button"
                                    onclick="return confirm('Are you sure you want to delete the fruit \'${fn:escapeXml(fruit.fruitName)}\'?');">
                                Delete
                            </button>
                        </form>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty fruitList}">
                <tr>
                    <td colspan="4">No fruits found.</td>
                </tr>
            </c:if>
        </tbody>
    </table>
</div>

<script>
    $(document).ready( function () {
        $('#fruitsTable').DataTable({
         
        });
    });
</script>

</body>
</html>
