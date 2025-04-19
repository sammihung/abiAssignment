<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="menu.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %> <%-- JSTL Core library --%>
<%@ page import="ict.bean.UserBean" %> <%-- Import UserBean if needed directly --%>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Reserve Fruit From Source</title>
    <%-- Basic Styling (Adapt as needed) --%>
    <style>
        body { font-family: sans-serif; margin: 20px; background-color: #f4f4f4; }
        .container { background-color: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0,0,0,0.1); max-width: 600px; margin: auto; }
        h1 { color: #333; text-align: center; }
        .form-group { margin-bottom: 15px; }
        label { display: block; margin-bottom: 5px; font-weight: bold; color: #555; }
        select, input[type="number"] {
            width: 95%; /* Adjust as needed */
            padding: 10px;
            margin-bottom: 10px;
            border: 1px solid #ccc;
            border-radius: 4px;
            box-sizing: border-box; /* Include padding and border in element's total width/height */
        }
        button {
            background-color: #007bff; /* Blue */
            color: white;
            padding: 12px 20px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 16px;
            transition: background-color 0.3s ease;
        }
        button:hover { background-color: #0056b3; }
        .message, .error-message {
            padding: 10px;
            margin-bottom: 15px;
            border-radius: 4px;
            text-align: center;
        }
        .message { background-color: #d4edda; color: #155724; border: 1px solid #c3e6cb; } /* Green */
        .error-message { background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; } /* Red */
        .back-link { display: block; text-align: center; margin-top: 20px; }
    </style>
</head>
<body>
    <%-- Optional: Include header --%>
    <%-- <jsp:include page="header.jsp" /> --%>

    <%-- Check if user is logged in (basic check, more robust check in servlet) --%>
    <%
        UserBean currentUser = (UserBean) session.getAttribute("userInfo");
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=SessionExpired");
            return; // Stop processing the rest of the page
        }
        // You can display user/shop info here if needed
        // out.println("<p>Shop: " + currentUser.getShopId() + "</p>");
    %>

    <div class="container">
        <h1>Reserve Fruit From Source</h1>

        <%-- Display Messages from Redirect --%>
        <c:if test="${not empty param.message}">
            <div class="message"><c:out value="${param.message}" /></div>
        </c:if>
        <c:if test="${not empty param.error}">
            <div class="error-message"><c:out value="${param.error}" /></div>
        </c:if>
         <%-- Display Messages from Forward (if using forward alternative in servlet) --%>
         <c:if test="${not empty message}">
            <div class="message"><c:out value="${message}" /></div>
        </c:if>
        <c:if test="${not empty errorMessage}">
            <div class="error-message"><c:out value="${errorMessage}" /></div>
        </c:if>

        <form action="<c:url value='/reserveFruit'/>" method="post">
            <div class="form-group">
                <label for="fruitId">Select Fruit:</label>
                <select id="fruitId" name="fruitId" required>
                    <option value="">-- Select a Fruit --</option>
                    <%-- Populate dropdown from the list passed by the servlet --%>
                    <c:forEach var="fruit" items="${allFruits}">
                        <%-- You might want to retain selection on error using param or attribute --%>
                        <option value="${fruit.fruitId}" ${param.fruitId == fruit.fruitId ? 'selected' : ''}>
                            <c:out value="${fruit.fruitName}"/> (<c:out value="${fruit.sourceCountry}"/>)
                        </option>
                    </c:forEach>
                </select>
            </div>

            <div class="form-group">
                <label for="quantity">Quantity:</label>
                <%-- Use type="number" for better input control --%>
                <input type="number" id="quantity" name="quantity" min="1" required
                       value="<c:out value='${param.quantity}'/>"> <%-- Retain quantity on error --%>
            </div>

            <%-- Shop ID is taken from the session in the servlet, no need for a field here --%>

            <div>
                <button type="submit">Create Reservation</button>
            </div>
        </form>

<!--         <a href="javascript:history.back()" class="back-link">Back</a>-->
         <%-- Or link to a specific page --%>
          <a href="${pageContext.request.contextPath}/welcome.jsp" class="back-link">Back to Welcome</a> 
    </div>

    <%-- Optional: Include footer --%>
    <%-- <jsp:include page="footer.jsp" /> --%>
</body>
</html>
