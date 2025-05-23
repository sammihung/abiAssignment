<%@page contentType="text/html" pageEncoding="UTF-8"%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page import="ict.bean.UserBean" %>
<%@ page import="ict.servlet.AuthFilter" %> 

<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Create New User</title>
        <style>
            body {
                font-family: sans-serif;
                padding: 0px;
                background-color: #f4f4f4;
            }
            .container {
                background-color: #fff;
                padding: 20px;
                border-radius: 8px;
                box-shadow: 0 0 10px rgba(0,0,0,0.1);
                max-width: 500px;
                margin: auto;
            }
            h1 {
                color: #333;
                text-align: center;
            }
            .form-group {
                margin-bottom: 15px;
            }
            label {
                display: block;
                margin-bottom: 5px;
                font-weight: bold;
            }
            input[type="text"], input[type="email"], input[type="password"], select {
                width: 95%;
                padding: 10px;
                border: 1px solid #ccc;
                border-radius: 4px;
                box-sizing: border-box;
            }
            button {
                background-color: #28a745;
                color: white;
                padding: 12px 20px;
                border: none;
                border-radius: 4px;
                cursor: pointer;
                font-size: 16px;
            }
            button:hover {
                background-color: #218838;
            }
            .message.error {
                padding: 10px;
                margin-bottom: 15px;
                border-radius: 4px;
                text-align: center;
                background-color: #f8d7da;
                color: #721c24;
                border: 1px solid #f5c6cb;
            }
            .back-link {
                display: block;
                text-align: center;
                margin-top: 20px;
            }
            
            #shopGroup, #warehouseGroup {
                display: none;
            }
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
            <h1>Create New User Account</h1>

            <c:if test="${not empty errorMessage}">
                <div class="message error"><c:out value="${errorMessage}"/></div>
            </c:if>

            <form action="<c:url value='/adminCreateUser'/>" method="POST">
                <div class="form-group">
                    <label for="username">Username:</label>
                    <input type="text" id="username" name="username" value="<c:out value='${prevUsername}'/>" required>
                </div>
                <div class="form-group">
                    <label for="password">Password:</label>
                    <input type="password" id="password" name="password" required>
                    
                </div>
                <div class="form-group">
                    <label for="email">Email:</label>
                    <input type="email" id="email" name="email" value="<c:out value='${prevEmail}'/>" required>
                </div>
                <div class="form-group">
                    <label for="role">Role:</label>
                    <select id="role" name="role" required onchange="toggleLocationSelects()">
                        <option value="">-- Select Role --</option>
                        
                        <option value="<%= AuthFilter.ROLE_BAKERY_SHOP_STAFF%>" ${prevRole == AuthFilter.ROLE_BAKERY_SHOP_STAFF ? 'selected' : ''}>Bakery Shop Staff</option>
                        <option value="<%= AuthFilter.ROLE_WAREHOUSE_STAFF%>" ${prevRole == AuthFilter.ROLE_WAREHOUSE_STAFF ? 'selected' : ''}>Warehouse Staff</option>
                        <option value="<%= AuthFilter.ROLE_SENIOR_MANAGEMENT%>" ${prevRole == AuthFilter.ROLE_SENIOR_MANAGEMENT ? 'selected' : ''}>Senior Management</option>
                    </select>
                </div>

                
                <div class="form-group" id="shopGroup">
                    <label for="shopId">Assign to Shop:</label>
                    <select id="shopId" name="shopId">
                        <option value="">-- Select Shop --</option>
                        <c:forEach var="shop" items="${allShops}">
                            <option value="${shop.shop_id}" ${prevShopId == shop.shop_id ? 'selected' : ''}>
                                <c:out value="${shop.shop_name}"/> (<c:out value="${shop.city}"/>)
                            </option>
                        </c:forEach>
                    </select>
                </div>

                
                <div class="form-group" id="warehouseGroup">
                    <label for="warehouseId">Assign to Warehouse:</label>
                    <select id="warehouseId" name="warehouseId">
                        <option value="">-- Select Warehouse --</option>
                        <c:forEach var="wh" items="${allWarehouses}">
                            <option value="${wh.warehouse_id}" ${prevWarehouseId == wh.warehouse_id ? 'selected' : ''}>
                                <c:out value="${wh.warehouse_name}"/> (<c:out value="${wh.city}"/>, <c:out value="${wh.country}"/>)
                            </option>
                        </c:forEach>
                    </select>
                </div>

                <button type="submit">Create User</button>
            </form>

        </div>

        <script>
            function toggleLocationSelects() {
                var role = document.getElementById('role').value;
                var shopGroup = document.getElementById('shopGroup');
                var warehouseGroup = document.getElementById('warehouseGroup');
                var shopSelect = document.getElementById('shopId');
                var warehouseSelect = document.getElementById('warehouseId');

                shopGroup.style.display = 'none';
                warehouseGroup.style.display = 'none';
                shopSelect.required = false; 
                warehouseSelect.required = false;

                if (role === '<%= AuthFilter.ROLE_BAKERY_SHOP_STAFF%>') {
                    shopGroup.style.display = 'block';
                    shopSelect.required = true; 
                } else if (role === '<%= AuthFilter.ROLE_WAREHOUSE_STAFF%>') {
                    warehouseGroup.style.display = 'block';
                    warehouseSelect.required = true; 
                }
            }

            document.addEventListener('DOMContentLoaded', toggleLocationSelects);
        </script>

    </body>
</html>