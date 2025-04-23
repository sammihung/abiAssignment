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

/**
 * Servlet for Senior Management to create new user accounts.
 */
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

    /**
     * Handles GET requests: Displays the admin user creation form.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // --- Security Check: Only Senior Management ---
        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;
        if (currentUser == null || !"Senior Management".equalsIgnoreCase(currentUser.getRole())) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=ManagementLoginRequired");
            return;
        }

        // --- Fetch data for dropdowns ---
        List<BakeryShopBean> shops = Collections.emptyList();
        List<WarehouseBean> warehouses = Collections.emptyList();
        try {
            shops = bakeryShopDb.getBakeryShop(); // Assumes this method returns all shops
            warehouses = warehouseDb.getAllWarehouses(); // Assumes this method exists in WarehouseDB
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching shops or warehouses for create user form", e);
            request.setAttribute("errorMessage", "Could not load shop/warehouse list.");
        }

        request.setAttribute("allShops", shops);
        request.setAttribute("allWarehouses", warehouses);

        RequestDispatcher rd = request.getRequestDispatcher("/adminCreateUser.jsp");
        rd.forward(request, response);
    }

    /**
     * Handles POST requests: Processes the new user creation.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // --- Security Check: Only Senior Management ---
        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;
        if (currentUser == null || !"Senior Management".equalsIgnoreCase(currentUser.getRole())) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=ManagementLoginRequired");
            return;
        }

        // --- Get Parameters ---
        String username = request.getParameter("username");
        String password = request.getParameter("password"); // Plain text - BAD! Hash this.
        String email = request.getParameter("email");
        String role = request.getParameter("role");
        String shopId = request.getParameter("shopId");
        String warehouseId = request.getParameter("warehouseId");

        String message = "User creation failed.";
        boolean success = false;

        // --- Basic Validation ---
        if (username == null || username.trim().isEmpty() ||
                password == null || password.trim().isEmpty() || // Ensure password isn't empty
                email == null || email.trim().isEmpty() ||
                role == null || role.trim().isEmpty()) {
            message = "Username, Password, Email, and Role are required.";
        } else {
            // Role specific validation: Shop staff need shopId, Warehouse staff need
            // warehouseId
            if (AuthFilter.ROLE_BAKERY_SHOP_STAFF.equals(role) && (shopId == null || shopId.trim().isEmpty())) {
                message = "Shop Staff must be assigned to a Shop ID.";
            } else if (AuthFilter.ROLE_WAREHOUSE_STAFF.equals(role)
                    && (warehouseId == null || warehouseId.trim().isEmpty())) {
                message = "Warehouse Staff must be assigned to a Warehouse ID.";
            } else {
                // Clear irrelevant ID based on role
                if (AuthFilter.ROLE_BAKERY_SHOP_STAFF.equals(role)) {
                    warehouseId = null; // Shop staff shouldn't have warehouse ID
                } else if (AuthFilter.ROLE_WAREHOUSE_STAFF.equals(role)) {
                    shopId = null; // Warehouse staff shouldn't have shop ID
                } else if (AuthFilter.ROLE_SENIOR_MANAGEMENT.equals(role)) {
                    shopId = null; // Management might not be tied to one location
                    warehouseId = null;
                }

                // --- Call DB to add user ---
                // **SECURITY:** You MUST hash the password before calling addUser
                // String hashedPassword = YourPasswordHasher.hash(password);
                // success = userDb.addUser(username, hashedPassword, email, role, shopId,
                // warehouseId);

                // Using plain text password for now (BAD PRACTICE!)
                success = userDb.addUser(username, password, email, role, shopId, warehouseId);

                if (success) {
                    message = "User '" + username + "' created successfully!";
                } else {
                    message = "Failed to create user '" + username + "'. Username or email might already exist.";
                }
            }
        }

        // --- Redirect back to user list or create page ---
        if (success) {
            response.sendRedirect("listUsers?message=" + java.net.URLEncoder.encode(message, "UTF-8"));
        } else {
            // Set attributes to re-populate form on error and forward back
            request.setAttribute("errorMessage", message);
            request.setAttribute("prevUsername", username);
            request.setAttribute("prevEmail", email);
            request.setAttribute("prevRole", role);
            request.setAttribute("prevShopId", shopId);
            request.setAttribute("prevWarehouseId", warehouseId);
            // Re-fetch dropdown data for the form
            doGet(request, response); // Re-run doGet to fetch lists and forward
        }
    }

    @Override
    public String getServletInfo() {
        return "Admin Servlet for creating new users";
    }
}
