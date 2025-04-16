<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="ict.bean.UserBean, ict.bean.FruitBean, java.util.ArrayList" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %> <%-- Optional: for functions like length --%>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Fruit List</title>
    <style>
        body { font-family: sans-serif; margin: 0; background-color: #f4f4f4; }
        .container { background-color: #fff; padding: 20px; margin: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0,0,0,0.1); }
        h1 { color: #333; text-align: center; margin-top: 0; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; }
        th, td { border: 1px solid #ddd; padding: 10px; text-align: left; }
        th { background-color: #4CAF50; color: white; }
        tr:nth-child(even) { background-color: #f2f2f2; }
        tr:hover { background-color: #ddd; }
        .message {
            padding: 10px;
            margin-bottom: 15px;
            border-radius: 4px;
            text-align: center;
        }
        .error { background-color: #f2dede; color: #a94442; border: 1px solid #ebccd1; }
        .no-data { text-align: center; color: #777; margin-top: 20px; }
        .back-link { display: inline-block; margin-top: 20px; text-decoration: none; color: #337ab7; }
        .back-link:hover { text-decoration: underline; }
    </style>
</head>
<body>
    <%-- Include the menu bar --%>
    <jsp:include page="menu.jsp" />

    <div class="container">
        <h1>List of Fruits</h1>

        <%-- Display Error Message if forwarded from Servlet --%>
        <c:if test="${not empty errorMessage}">
            <div class="message error"><c:out value="${errorMessage}" /></div>
        </c:if>

        <%-- Check if the fruitList attribute exists and is not empty --%>
        <c:choose>
            <c:when test="${not empty fruitList}">
                <table>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Fruit Name</th>
                            <th>Source Country</th>
                        </tr>
                    </thead>
                    <tbody>
                        <%-- Iterate over the list of fruits passed from the servlet --%>
                        <c:forEach var="fruit" items="${fruitList}">
                            <tr>
                                <td><c:out value="${fruit.fruitId}" /></td>
                                <td><c:out value="${fruit.fruitName}" /></td>
                                <td><c:out value="${fruit.sourceCountry}" /></td>
                                <%-- Optional: Add Edit/Delete links here if needed later --%>
                                <%--
                                <td>
                                    <a href="${pageContext.request.contextPath}/manageFruits?action=edit&id=${fruit.fruitId}">Edit</a> |
                                    <a href="${pageContext.request.contextPath}/manageFruits?action=delete&id=${fruit.fruitId}" onclick="return confirm('Are you sure?')">Delete</a>
                                </td>
                                --%>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </c:when>
            <c:otherwise>
                <%-- Display message if no fruits are found (and no error occurred) --%>
                <c:if test="${empty errorMessage}">
                     <p class="no-data">No fruits found in the database.</p>
                </c:if>
            </c:otherwise>
        </c:choose>

         <a href="${pageContext.request.contextPath}/welcome.jsp" class="back-link">&laquo; Back to Welcome Page</a>

    </div> <%-- End container --%>

</body>
</html>
