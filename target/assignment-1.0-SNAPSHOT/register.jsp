<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="menu.jsp" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Register</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            background-color: #f4f4f9;
            margin: 0;
            padding: 0;
        }
        .menu-bar {
            margin-bottom: 20px; /* Add spacing below the menu */
        }
        .register-container {
            background-color: #ffffff;
            padding: 20px 30px;
            border-radius: 8px;
            box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
            width: 100%;
            max-width: 400px;
            margin: 20px auto; /* Center the container */
        }
        .register-container h2 {
            text-align: center;
            margin-bottom: 20px;
            color: #333333;
        }
        .form-group {
            margin-bottom: 15px;
        }
        .form-group label {
            display: block;
            margin-bottom: 5px;
            font-weight: bold;
            color: #555555;
        }
        .form-group input {
            width: 100%;
            padding: 10px;
            border: 1px solid #cccccc;
            border-radius: 4px;
            box-sizing: border-box;
        }
        .form-group input:focus {
            border-color: #007bff;
            outline: none;
        }
        .error-message {
            color: #d9534f;
            font-size: 0.9em;
            margin-bottom: 15px;
        }
        .submit-btn {
            width: 100%;
            padding: 10px;
            background-color: #007bff;
            color: #ffffff;
            border: none;
            border-radius: 4px;
            font-size: 1em;
            cursor: pointer;
        }
        .submit-btn:hover {
            background-color: #0056b3;
        }
       
    </style>
</head>
<body>
    <div class="register-container">
        <h2>Register Form</h2>
        <c:if test="${not empty error}">
            <div class="error-message">
                <c:out value="${error}" />
            </div>
        </c:if>
        <form action="register" method="post">
            <div class="form-group">
                <label for="username">Username</label>
                <input type="text" id="username" name="username" required>
            </div>
            <div class="form-group">
                <label for="password">Password</label>
                <input type="password" id="password" name="password" required>
            </div>
            <div class="form-group">
                <label for="email">Email</label>
                <input type="email" id="email" name="email" required>
            </div>
            <c:choose>
                <c:when test="${userInfo.role == 'Bakery shop staff'}">
                    <div class="form-group">
                        <label for="shop">Shop</label>
                        <input type="text" id="shopId" name="shopId" value="${userInfo.shopId}" readonly>
                    </div>
                </c:when>
                <c:when test="${userInfo.role == 'Warehouse Staff'}">
                    <div class="form-group">
                        <label for="warehouse">Warehouse</label>
                        <input type="text" id="warehouseId" name="warehouseId" value="${userInfo.warehouseId}" readonly>
                    </div>
                </c:when>
                <c:otherwise>
                    <p>Debug: User Role is not recognized or not set.</p>
                </c:otherwise>
            </c:choose>
            <button type="submit" class="submit-btn">Register</button>
        </form>
  
    </div>
</body>
</html>