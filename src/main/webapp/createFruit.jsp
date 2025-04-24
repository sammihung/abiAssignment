<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="menu.jsp" %>
<%@ page import="ict.bean.UserBean" %>
<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Create Fruit</title>

        <style>
        body { font-family: sans-serif; margin: 0px; background-color: #f4f4f4; }
        .container { background-color: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0,0,0,0.1); max-width: 500px; margin: auto; }
        h1 { color: #333; text-align: center; }
        label { display: block; margin-bottom: 5px; font-weight: bold; color: #555; }
        input[type="text"] { 
            width: calc(100% - 22px);
            padding: 10px;
            margin-bottom: 15px;
            border: 1px solid #ccc;
            border-radius: 4px;
            box-sizing: border-box;
        }
        button {
            background-color: #5cb85c;
            color: white;
            padding: 12px 20px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 16px;
            transition: background-color 0.3s ease;
        }
        button:hover { background-color: #4cae4c; }
        .message {
            padding: 10px;
            margin-bottom: 15px;
            border-radius: 4px;
            text-align: center;
        }
        .success { background-color: #dff0d8; color: #3c763d; border: 1px solid #d6e9c6; }
        .error { background-color: #f2dede; color: #a94442; border: 1px solid #ebccd1; }
        .back-link { display: block; text-align: center; margin-top: 20px; }
    </style>
    </head>
    <body>

        <%
        UserBean currentUser = (UserBean) session.getAttribute("userInfo");
        if (currentUser == null) {
        response.sendRedirect(request.getContextPath() +
        "/login.jsp?error=SessionExpired");
        return;
        }

        String successMessage = request.getParameter("message");
        String errorMessage = (String) request.getAttribute("errorMessage");

        String fruitNameValue = request.getAttribute("fruitNameValue") != null ?
        (String) request.getAttribute("fruitNameValue") : "";
        String sourceCountryValue = request.getAttribute("sourceCountryValue")
        != null ? (String) request.getAttribute("sourceCountryValue") : "";
        %>

        <div class="container">
            <h1>Create New Fruit</h1>

            <% if (successMessage != null && !successMessage.isEmpty()) { %>
            <div class="message success"><%= successMessage.replace("+", " ")
                %></div>
            <% } %>
            <% if (errorMessage != null && !errorMessage.isEmpty()) { %>
            <div class="message error"><%= errorMessage %></div>
            <% } %>

            <form action="${pageContext.request.contextPath}/manageFruits"
                method="post">
                <div>
                    <label for="fruitName">Fruit Name:</label>
                    <input type="text" id="fruitName" name="fruitName" required
                        value="<%= fruitNameValue %>">
                </div>
                <div>
                    <label for="sourceCountry">Source Country:</label>
                    <input type="text" id="sourceCountry" name="sourceCountry"
                        required value="<%= sourceCountryValue %>">
                </div>

                <div>
                    <button type="submit">Create Fruit</button>
                </div>
            </form>

        </div>

    </body>
</html>