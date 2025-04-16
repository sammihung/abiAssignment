<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page import="ict.bean.UserBean" %>

<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Borrow Fruit from Another Shop</title>
        <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
        <%-- Basic Styling (Adapt as needed) --%>
        <style>
            body {
                font-family: sans-serif;
                margin: 20px;
                background-color: #f4f4f4;
            }
            .container {
                background-color: #fff;
                padding: 20px;
                border-radius: 8px;
                box-shadow: 0 0 10px rgba(0,0,0,0.1);
                max-width: 700px;
                margin: auto;
            }
            h1 {
                color: #333;
                text-align: center;
            }
            .form-section {
                margin-bottom: 20px;
                padding-bottom: 15px;
                border-bottom: 1px solid #eee;
            }
            .form-section:last-child {
                border-bottom: none;
            }
            label {
                display: block;
                margin-bottom: 5px;
                font-weight: bold;
                color: #555;
            }
            select, input[type="number"], input[type="text"] {
                width: 95%; /* Adjust as needed */
                padding: 10px;
                margin-bottom: 10px;
                border: 1px solid #ccc;
                border-radius: 4px;
                box-sizing: border-box;
            }
            input[readonly] {
                background-color: #e9ecef;
            }
            button {
                background-color: #007bff;
                color: white;
                padding: 10px 15px;
                border: none;
                border-radius: 4px;
                cursor: pointer;
                font-size: 15px;
                transition: background-color 0.3s ease;
                margin-right: 10px;
            }
            button[type="submit"] {
                background-color: #28a745;
            } /* Green for final action */
            button:hover {
                opacity: 0.9;
            }
            .message, .error-message {
                padding: 10px;
                margin-bottom: 15px;
                border-radius: 4px;
                text-align: center;
            }
            .message {
                background-color: #d4edda;
                color: #155724;
                border: 1px solid #c3e6cb;
            }
            .error-message {
                background-color: #f8d7da;
                color: #721c24;
                border: 1px solid #f5c6cb;
            }
            .back-link {
                display: block;
                text-align: center;
                margin-top: 20px;
            }
            #lenderSection {
                margin-top: 15px;
            } /* Style for the dynamic section */
            #lenderSection label {
                margin-top: 10px;
            }
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
            <h1>Borrow Fruit from Shops in Your City</h1>

            <%-- Display Messages --%>
            <c:if test="${not empty param.message}">
                <div class="message"><c:out value="${param.message}" /></div>
            </c:if>
            <c:if test="${not empty param.error}">
                <div class="error-message"><c:out value="${param.error}" /></div>
            </c:if>
            <c:if test="${not empty errorMessage}">
                <div class="error-message"><c:out value="${errorMessage}" /></div>
            </c:if>

            <%-- Step 1: Form to select fruit and quantity, and find lenders --%>
            <form id="findLenderForm" action="<c:url value='/borrowFruit'/>" method="GET">
                <div class="form-section">
                    <h2>Step 1: Find Available Shops</h2>
                    <div>
                        <label for="fruitId">Select Fruit:</label>
                        <select id="fruitId" name="fruitId" required>
                            <option value="">-- Select a Fruit --</option>
                            <c:forEach var="fruit" items="${allFruits}">
                                <%-- Retain selection if page reloads with params --%>
                                <option value="${fruit.fruitId}" ${param.fruitId == fruit.fruitId || selectedFruitId == fruit.fruitId ? 'selected' : ''}>
                                    <c:out value="${fruit.fruitName}"/> (<c:out value="${fruit.sourceCountry}"/>)
                                </option>
                            </c:forEach>
                        </select>
                    </div>
                    <div>
                        <label for="quantity">Quantity Needed:</label>
                        <input type="number" id="quantity" name="quantity" min="1" required
                               value="<c:out value='${not empty param.quantity ? param.quantity : enteredQuantity}'/>"> <%-- Retain quantity --%>
                    </div>
                    <button type="submit">Find Shops</button>
                </div>
            </form>

            <%-- Step 2: Display potential lenders and form to confirm borrowing --%>
            <%-- This section is shown only if potentialLenders list is available --%>
            <c:if test="${not empty potentialLenders}">
                <form id="confirmBorrowForm" action="<c:url value='/borrowFruit'/>" method="POST">
                    <div class="form-section">
                        <h2>Step 2: Choose a Shop to Borrow From</h2>
                        <%-- Hidden fields to pass fruit and quantity to the POST request --%>
                        <input type="hidden" name="fruitId" value="${param.fruitId != null ? param.fruitId : selectedFruitId}">
                        <input type="hidden" name="quantity" value="${param.quantity != null ? param.quantity : enteredQuantity}">

                        <div>
                            <label for="lendingShopId">Select Lending Shop:</label>
                            <select id="lendingShopId" name="lendingShopId" required>
                                <option value="">-- Select a Shop --</option>
                                <c:forEach var="lender" items="${potentialLenders}">
                                    <option value="${lender.shopId}">
                                        <c:out value="${lender.shopName}"/> (Available: <c:out value="${lender.availableQuantity}"/>)
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                        <button type="submit">Confirm Borrowing</button>
                    </div>
                </form>
            </c:if>
            <%-- Message if lenders were searched but none found --%>
            <c:if test="${empty potentialLenders && (not empty param.fruitId || not empty selectedFruitId)}">
                <div class="error-message">No shops found in your city with enough stock of the selected fruit.</div>
            </c:if>


           <a href="${pageContext.request.contextPath}/welcome.jsp" class="back-link">Back to Welcome</a> 
        </div>

        <%-- Optional: Include footer --%>
        <%-- <jsp:include page="footer.jsp" /> --%>
    </body>
</html>
