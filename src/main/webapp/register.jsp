<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="checkLogin.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Register</title>
    <script>
        // Function to fetch and populate shops
        function fetchShops() {
            fetch('BakeryShopController?action=getShops')
                .then(response => response.json())
                .then(data => {
                    const shopSelect = document.getElementById('shop');
                    shopSelect.innerHTML = ''; // Clear existing options
                    data.forEach(shop => {
                        const option = document.createElement('option');
                        option.value = shop.id;
                        option.textContent = shop.name;
                        shopSelect.appendChild(option);
                    });
                })
                .catch(error => console.error('Error fetching shops:', error));
        }

        // Function to fetch and populate warehouses
        function fetchWarehouses() {
            fetch('BakeryShopController?action=getWarehouses')
                .then(response => response.json())
                .then(data => {
                    const warehouseSelect = document.getElementById('warehouse');
                    warehouseSelect.innerHTML = ''; // Clear existing options
                    data.forEach(warehouse => {
                        const option = document.createElement('option');
                        option.value = warehouse.id;
                        option.textContent = warehouse.name;
                        warehouseSelect.appendChild(option);
                    });
                })
                .catch(error => console.error('Error fetching warehouses:', error));
        }

        // Call the appropriate function based on the user role
        document.addEventListener('DOMContentLoaded', () => {
            const userRole = '${user.role}';
            if (userRole === 'Bakery shop staff') {
                fetchShops();
            } else if (userRole === 'Warehouse Staff') {
                fetchWarehouses();
            }
        });
    </script>
</head>
<body>
    <h2>Register Form</h2>
    <form action="register" method="post">
        <label for="username">Username:</label>
        <input type="text" id="username" name="username" required><br><br>
        
        <label for="password">Password:</label>
        <input type="password" id="password" name="password" required><br><br>
        
        <label for="email">Email:</label>
        <input type="email" id="email" name="email" required><br><br>

        <c:choose>
            <c:when test="${user.role == 'Bakery shop staff'}">
                <label for="shop">Shop:</label>
                <select id="shop" name="shop" required></select><br><br>
            </c:when>
            <c:when test="${user.role == 'Warehouse Staff'}">
                <label for="warehouse">Warehouse:</label>
                <select id="warehouse" name="warehouse" required></select><br><br>
            </c:when>
        </c:choose>
        
        <input type="submit" value="Register">
    </form>
</body>
</html>@WebServlet(name = "BakeryShopController", urlPatterns = {"/BakeryShopController"})
public class BakeryShopController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        if ("getShops".equals(action)) {
            // Example: Fetch shops from the database
            List<Map<String, String>> shops = new ArrayList<>();
            shops.add(Map.of("id", "1", "name", "Shop A"));
            shops.add(Map.of("id", "2", "name", "Shop B"));
            shops.add(Map.of("id", "3", "name", "Shop C"));
            out.write(new Gson().toJson(shops)); // Use Gson to convert list to JSON
        } else if ("getWarehouses".equals(action)) {
            // Example: Fetch warehouses from the database
            List<Map<String, String>> warehouses = new ArrayList<>();
            warehouses.add(Map.of("id", "1", "name", "Warehouse X"));
            warehouses.add(Map.of("id", "2", "name", "Warehouse Y"));
            warehouses.add(Map.of("id", "3", "name", "Warehouse Z"));
            out.write(new Gson().toJson(warehouses)); // Use Gson to convert list to JSON
        }
    }
}