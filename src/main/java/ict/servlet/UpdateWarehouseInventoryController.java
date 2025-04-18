package ict.servlet;

import ict.bean.FruitBean;
import ict.bean.InventoryBean;
import ict.bean.UserBean;
import ict.db.BorrowingDB; // Contains inventory methods now
import ict.db.FruitDB;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet to handle updating warehouse inventory levels by Warehouse Staff.
 */
@WebServlet(name = "UpdateWarehouseInventoryController", urlPatterns = {"/updateWarehouseInventory"})
public class UpdateWarehouseInventoryController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(UpdateWarehouseInventoryController.class.getName());
    private BorrowingDB borrowingDb; // Contains inventory methods
    private FruitDB fruitDb;

    @Override
    public void init() throws ServletException {
        // Initialize DB connections
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");
        String dbUrl = getServletContext().getInitParameter("dbUrl");

        if (dbUrl == null || dbUser == null /*|| dbPassword == null*/) {
            LOGGER.log(Level.SEVERE, "Database connection parameters missing in web.xml.");
            throw new ServletException("Database connection parameters missing.");
        }
        borrowingDb = new BorrowingDB(dbUrl, dbUser, dbPassword);
        fruitDb = new FruitDB(dbUrl, dbUser, dbPassword);
        LOGGER.log(Level.INFO, "UpdateWarehouseInventoryController initialized.");
    }

    /**
     * Handles GET requests: Displays current warehouse inventory and the update form.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // --- Security Check ---
        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;

        // Check if user is logged in, is Warehouse Staff, and has a warehouse ID
        if (currentUser == null || !"Warehouse Staff".equalsIgnoreCase(currentUser.getRole()) ||
            currentUser.getWarehouseId() == null || currentUser.getWarehouseId().trim().isEmpty()) {

            LOGGER.log(Level.WARNING, "Unauthorized access attempt to GET /updateWarehouseInventory. User: {0}, Role: {1}, WarehouseID: {2}",
                       new Object[]{ (currentUser != null ? currentUser.getUsername() : "null"),
                                     (currentUser != null ? currentUser.getRole() : "null"),
                                     (currentUser != null ? currentUser.getWarehouseId() : "null") });
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=WarehouseStaffLoginRequired");
            return;
        }

        try {
            int currentWarehouseId = Integer.parseInt(currentUser.getWarehouseId());

            // Fetch current inventory for the warehouse
            List<InventoryBean> inventoryList = borrowingDb.getInventoryForWarehouse(currentWarehouseId);
            request.setAttribute("inventoryList", inventoryList);

            // Fetch all fruits for the dropdown
            List<FruitBean> allFruits = fruitDb.getAllFruits();
            request.setAttribute("allFruits", allFruits);

            LOGGER.log(Level.INFO, "Fetched {0} inventory items and {1} fruits for WarehouseID={2}. Forwarding to updateWarehouseInventory.jsp",
                    new Object[]{inventoryList.size(), allFruits.size(), currentWarehouseId});

            // Forward to the JSP page
            RequestDispatcher rd = request.getRequestDispatcher("/updateWarehouseInventory.jsp");
            rd.forward(request, response);

        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Invalid Warehouse ID format for current user: " + currentUser.getWarehouseId(), e);
            request.setAttribute("errorMessage", "Invalid user profile (Warehouse ID). Cannot load inventory.");
            RequestDispatcher rd = request.getRequestDispatcher("/updateWarehouseInventory.jsp");
            rd.forward(request, response);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading warehouse inventory page.", e);
            request.setAttribute("errorMessage", "Error loading inventory management page.");
            RequestDispatcher rd = request.getRequestDispatcher("/updateWarehouseInventory.jsp");
            rd.forward(request, response);
        }
    }

    /**
     * Handles POST requests: Processes the warehouse inventory update submission.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // --- Security Check ---
        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;

        if (currentUser == null || !"Warehouse Staff".equalsIgnoreCase(currentUser.getRole()) ||
            currentUser.getWarehouseId() == null || currentUser.getWarehouseId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Unauthorized POST attempt to /updateWarehouseInventory.");
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=SessionExpired");
            return;
        }

        // --- Get form parameters ---
        String fruitIdStr = request.getParameter("fruitId");
        String newQuantityStr = request.getParameter("newQuantity");
        String message = "Warehouse inventory update failed."; // Default message
        boolean success = false;

        try {
            int fruitId = Integer.parseInt(fruitIdStr);
            int newQuantity = Integer.parseInt(newQuantityStr);
            int warehouseId = Integer.parseInt(currentUser.getWarehouseId());

            LOGGER.log(Level.INFO, "Processing warehouse inventory update: FruitID={0}, NewQuantity={1}, WarehouseID={2}",
                       new Object[]{fruitId, newQuantity, warehouseId});

            if (newQuantity < 0) {
                 message = "Quantity cannot be negative.";
                 LOGGER.log(Level.WARNING, "Invalid negative quantity received: {0}", newQuantity);
            } else {
                 // --- Call the DB method to set the warehouse inventory quantity ---
                 boolean updated = borrowingDb.setWarehouseInventoryQuantity(fruitId, warehouseId, newQuantity);
                 if (updated) {
                      message = "Warehouse inventory updated successfully!";
                      success = true;
                 } else {
                      message = "Failed to update warehouse inventory in database.";
                 }
            }

        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Invalid number format received for fruitId or newQuantity.", e);
            message = "Invalid input: Fruit ID and New Quantity must be numbers.";
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing warehouse inventory update.", e);
            message = "An unexpected error occurred while updating inventory.";
        }

        // --- Redirect back to the GET handler to refresh the page ---
        String redirectUrl = "updateWarehouseInventory"; // Relative URL to self (doGet)
        if (success) {
            redirectUrl += "?message=" + java.net.URLEncoder.encode(message, "UTF-8");
        } else {
            redirectUrl += "?error=" + java.net.URLEncoder.encode(message, "UTF-8");
        }
        response.sendRedirect(redirectUrl);
    }

    @Override
    public String getServletInfo() {
        return "Servlet for updating warehouse fruit inventory";
    }
}

