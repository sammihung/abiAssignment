<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %> <%-- JSTL Core library --%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Update Fruit</title>
    <%-- Basic Styling (Optional - Consider using a shared CSS file) --%>
    <style>
        body { font-family: sans-serif; margin: 20px; }
        .container { max-width: 500px; margin: auto; padding: 20px; border: 1px solid #ccc; border-radius: 8px; }
        .form-group { margin-bottom: 15px; }
        label { display: block; margin-bottom: 5px; }
        input[type="text"] { width: 95%; padding: 8px; border: 1px solid #ccc; border-radius: 4px; }
        button { padding: 10px 15px; background-color: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer; }
        button:hover { background-color: #0056b3; }
        .error-message { color: red; margin-bottom: 15px; border: 1px solid red; padding: 10px; border-radius: 4px; background-color: #ffebeb; }
        .back-link { margin-top: 20px; display: block; }
    </style>
</head>
<body>

<div class="container">
    <h2>Update Fruit Information</h2>

    <%-- Display error message if forwarded from servlet --%>
    <c:if test="${not empty errorMessage}">
        <div class="error-message">
            <c:out value="${errorMessage}" />
        </div>
    </c:if>

    <%-- Ensure fruitToEdit object is available before rendering the form --%>
    <c:if test="${not empty fruitToEdit}">
        <form action="<c:url value='/updateFruit'/>" method="POST">
            <%-- Hidden field to send the fruit ID back to the servlet --%>
            <input type="hidden" name="fruitId" value="<c:out value='${fruitToEdit.fruitId}'/>">

            <div class="form-group">
                <label for="fruitName">Fruit Name:</label>
                <%-- Pre-fill the input with the current fruit name --%>
                <input type="text" id="fruitName" name="fruitName" value="<c:out value='${fruitToEdit.fruitName}'/>" required>
            </div>

            <div class="form-group">
                <label for="sourceCountry">Source Country:</label>
                <%-- Pre-fill the input with the current source country --%>
                <input type="text" id="sourceCountry" name="sourceCountry" value="<c:out value='${fruitToEdit.sourceCountry}'/>" required>
            </div>

            <button type="submit">Update Fruit</button>
        </form>
    </c:if>
    <%-- Handle case where fruitToEdit might be null (e.g., direct access without ID) --%>
    <c:if test="${empty fruitToEdit && empty errorMessage}">
         <p class="error-message">Fruit data not available for editing.</p>
    </c:if>

    <a href="<c:url value='/listFruits'/>" class="back-link">Back to Fruit List</a>

</div>

</body>
</html>
