package ict.servlet;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import ict.bean.BakeryShopBean;
import ict.bean.UserBean;
import ict.bean.WarehouseBean;
import ict.db.BakeryShopDB;
import ict.db.UserDB;
import ict.db.WarehouseDB;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "AdminCreateUserController", urlPatterns = { "/adminCreateUser" })
public class AdminCreateUserController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AdminCreateUserController.class.getName());
    private UserDB userDb;
    private BakeryShopDB bakeryShopDb;
    private WarehouseDB warehouseDb;

    @Override
    public void init() throws ServletException {
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");
        String dbUrl = getServletContext().getInitParameter("dbUrl");
        if (dbUrl == null || dbUser == null) {
            throw new ServletException("Database connection parameters missing.");
        }
        userDb = new UserDB(dbUrl, dbUser, dbPassword);
        bakeryShopDb = new BakeryShopDB(dbUrl, dbUser, dbPassword);
        warehouseDb = new WarehouseDB(dbUrl, dbUser, dbPassword);
        LOGGER.log(Level.INFO, "AdminCreateUserController initialized.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;
        if (currentUser == null || !"Senior Management".equalsIgnoreCase(currentUser.getRole())) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=ManagementLoginRequired");
            return;
        }

        List<BakeryShopBean> shops = Collections.emptyList();
        List<WarehouseBean> warehouses = Collections.emptyList();
        try {
            shops = bakeryShopDb.getBakeryShop();
            warehouses = warehouseDb.getAllWarehouses();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching shops or warehouses for create user form", e);
            request.setAttribute("errorMessage", "Could not load shop/warehouse list.");
        }

        request.setAttribute("allShops", shops);
        request.setAttribute("allWarehouses", warehouses);

        RequestDispatcher rd = request.getRequestDispatcher("/adminCreateUser.jsp");
        rd.forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;
        if (currentUser == null || !"Senior Management".equalsIgnoreCase(currentUser.getRole())) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=ManagementLoginRequired");
            return;
        }

        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String email = request.getParameter("email");
        String role = request.getParameter("role");
        String shopId = request.getParameter("shopId");
        String warehouseId = request.getParameter("warehouseId");

        String message = "User creation failed.";
        boolean success = false;

        if (username == null || username.trim().isEmpty() ||
                password == null || password.trim().isEmpty() ||
                email == null || email.trim().isEmpty() ||
                role == null || role.trim().isEmpty()) {
            message = "Username, Password, Email, and Role are required.";
        } else {
            if (AuthFilter.ROLE_BAKERY_SHOP_STAFF.equals(role) && (shopId == null || shopId.trim().isEmpty())) {
                message = "Shop Staff must be assigned to a Shop ID.";
            } else if (AuthFilter.ROLE_WAREHOUSE_STAFF.equals(role)
                    && (warehouseId == null || warehouseId.trim().isEmpty())) {
                message = "Warehouse Staff must be assigned to a Warehouse ID.";
            } else {
                if (AuthFilter.ROLE_BAKERY_SHOP_STAFF.equals(role)) {
                    warehouseId = null;
                } else if (AuthFilter.ROLE_WAREHOUSE_STAFF.equals(role)) {
                    shopId = null;
                } else if (AuthFilter.ROLE_SENIOR_MANAGEMENT.equals(role)) {
                    shopId = null;
                    warehouseId = null;
                }

                success = userDb.addUser(username, password, email, role, shopId, warehouseId);

                if (success) {
                    message = "User '" + username + "' created successfully!";
                } else {
                    message = "Failed to create user '" + username + "'. Username or email might already exist.";
                }
            }
        }

        if (success) {
            response.sendRedirect("listUsers?message=" + java.net.URLEncoder.encode(message, "UTF-8"));
        } else {
            request.setAttribute("errorMessage", message);
            request.setAttribute("prevUsername", username);
            request.setAttribute("prevEmail", email);
            request.setAttribute("prevRole", role);
            request.setAttribute("prevShopId", shopId);
            request.setAttribute("prevWarehouseId", warehouseId);
            doGet(request, response);
        }
    }

    @Override
    public String getServletInfo() {
        return "Admin Servlet for creating new users";
    }
}
