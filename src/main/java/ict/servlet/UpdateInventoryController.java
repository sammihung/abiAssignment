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
 * Servlet to handle updating shop inventory levels.
 */
@WebServlet(name = "UpdateInventoryController", urlPatterns = {"/updateInventory"})
public class UpdateInventoryController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(UpdateInventoryController.class.getName());
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
        // Assuming BorrowingDB constructor initializes needed dependencies or handles them internally
        borrowingDb = new BorrowingDB(dbUrl, dbUser, dbPassword);
        fruitDb = new FruitDB(dbUrl, dbUser, dbPassword);
        LOGGER.log(Level.INFO, "UpdateInventoryController initialized.");
    }

    /**
     * Handles GET requests: Displays current inventory and the update form.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // --- Security Check ---
        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;

        if (currentUser == null || currentUser.getShopId() == null || currentUser.getShopId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Unauthorized access attempt to GET /updateInventory.");
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=ShopLoginRequired");
            return;
        }
        // Optional: Role check

        try {
            int currentShopId = Integer.parseInt(currentUser.getShopId());

            // Fetch current inventory for the shop
            List<InventoryBean> inventoryList = borrowingDb.getInventoryForShop(currentShopId);
            request.setAttribute("inventoryList", inventoryList);

            // Fetch all fruits for the dropdown
            List<FruitBean> allFruits = fruitDb.getAllFruits();
            request.setAttribute("allFruits", allFruits);

            LOGGER.log(Level.INFO, "Fetched {0} inventory items and {1} fruits for ShopID={2}. Forwarding to updateInventory.jsp",
                    new Object[]{inventoryList.size(), allFruits.size(), currentShopId});

            // Forward to the JSP page
            RequestDispatcher rd = request.getRequestDispatcher("/updateInventory.jsp");
            rd.forward(request, response);

        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Invalid Shop ID format for current user: " + currentUser.getShopId(), e);
            request.setAttribute("errorMessage", "Invalid user profile (Shop ID). Cannot load inventory.");
            RequestDispatcher rd = request.getRequestDispatcher("/updateInventory.jsp");
            rd.forward(request, response);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading inventory page.", e);
            request.setAttribute("errorMessage", "Error loading inventory management page.");
            RequestDispatcher rd = request.getRequestDispatcher("/updateInventory.jsp");
            rd.forward(request, response);
        }
    }

    /**
     * Handles POST requests: Processes the inventory update submission.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // --- Security Check ---
        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;

        if (currentUser == null || currentUser.getShopId() == null || currentUser.getShopId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Unauthorized POST attempt to /updateInventory.");
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=SessionExpired");
            return;
        }
        // Optional: Role check

        // --- Get form parameters ---
        String fruitIdStr = request.getParameter("fruitId");
        String newQuantityStr = request.getParameter("newQuantity");
        String message = "Inventory update failed."; // Default message
        boolean success = false;

        try {
            int fruitId = Integer.parseInt(fruitIdStr);
            int newQuantity = Integer.parseInt(newQuantityStr);
            int shopId = Integer.parseInt(currentUser.getShopId());

            LOGGER.log(Level.INFO, "Processing inventory update: FruitID={0}, NewQuantity={1}, ShopID={2}",
                       new Object[]{fruitId, newQuantity, shopId});

            if (newQuantity < 0) {
                 message = "Quantity cannot be negative.";
                 LOGGER.log(Level.WARNING, "Invalid negative quantity received: {0}", newQuantity);
            } else {
                 // --- Call the DB method to set the inventory quantity ---
                 boolean updated = borrowingDb.setShopInventoryQuantity(fruitId, shopId, newQuantity);
                 if (updated) {
                      message = "Inventory updated successfully!";
                      success = true;
                 } else {
                      message = "Failed to update inventory in database.";
                 }
            }

        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Invalid number format received for fruitId or newQuantity.", e);
            message = "Invalid input: Fruit ID and New Quantity must be numbers.";
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing inventory update.", e);
            message = "An unexpected error occurred while updating inventory.";
        }

        // --- Redirect back to the GET handler to refresh the page ---
        String redirectUrl = "updateInventory"; // Relative URL to self (doGet)
        if (success) {
            redirectUrl += "?message=" + java.net.URLEncoder.encode(message, "UTF-8");
        } else {
            redirectUrl += "?error=" + java.net.URLEncoder.encode(message, "UTF-8");
            // Optionally pass back failed input if needed, though GET re-fetches anyway
            // redirectUrl += "&failedFruitId=" + fruitIdStr;
            // redirectUrl += "&failedQuantity=" + newQuantityStr;
        }
        response.sendRedirect(redirectUrl);
    }

    @Override
    public String getServletInfo() {
        return "Servlet for updating shop fruit inventory";
    }
}
