package ict.servlet;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger; // Contains inventory methods now

import ict.bean.FruitBean;
import ict.bean.InventoryBean;
import ict.bean.UserBean;
import ict.db.BorrowingDB;
import ict.db.FruitDB;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet; // Keep for getting user info
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "UpdateInventoryController", urlPatterns = { "/updateInventory" })
public class UpdateInventoryController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(UpdateInventoryController.class.getName());
    private BorrowingDB borrowingDb; // Contains inventory methods
    private FruitDB fruitDb;

    // init() method remains the same

    @Override
    public void init() throws ServletException {
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");
        String dbUrl = getServletContext().getInitParameter("dbUrl");

        if (dbUrl == null || dbUser == null /* || dbPassword == null */) {
            LOGGER.log(Level.SEVERE, "Database connection parameters missing in web.xml.");
            throw new ServletException("Database connection parameters missing.");
        }
        borrowingDb = new BorrowingDB(dbUrl, dbUser, dbPassword);
        fruitDb = new FruitDB(dbUrl, dbUser, dbPassword);
        LOGGER.log(Level.INFO, "UpdateInventoryController initialized.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // --- Filter already handled authentication and basic role check ---
        HttpSession session = request.getSession(false); // Get existing session
        UserBean currentUser = (UserBean) session.getAttribute("userInfo");

        try {
            // Get shop ID from the authenticated user
            int currentShopId = Integer.parseInt(currentUser.getShopId()); // Filter ensures shopId exists

            // Fetch current inventory for the shop
            List<InventoryBean> inventoryList = borrowingDb.getInventoryForShop(currentShopId);
            request.setAttribute("inventoryList", inventoryList);

            // Fetch all fruits for the dropdown
            List<FruitBean> allFruits = fruitDb.getAllFruits();
            request.setAttribute("allFruits", allFruits);

            LOGGER.log(Level.INFO,
                    "Fetched {0} inventory items and {1} fruits for ShopID={2}. Forwarding to updateInventory.jsp",
                    new Object[] { inventoryList.size(), allFruits.size(), currentShopId });

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

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // --- Filter already handled authentication and basic role check ---
        HttpSession session = request.getSession(false); // Get existing session
        UserBean currentUser = (UserBean) session.getAttribute("userInfo");

        // --- Get form parameters ---
        String fruitIdStr = request.getParameter("fruitId");
        String newQuantityStr = request.getParameter("newQuantity");
        String message = "Inventory update failed."; // Default message
        boolean success = false;

        try {
            int fruitId = Integer.parseInt(fruitIdStr);
            int newQuantity = Integer.parseInt(newQuantityStr);
            // ***** Resource-Level Authorization Implicit Check *****
            // Get the shop ID *only* from the logged-in user's session, not from the
            // request.
            // This ensures they can only update *their own* shop's inventory.
            int shopId = Integer.parseInt(currentUser.getShopId()); // Filter ensures shopId exists

            LOGGER.log(Level.INFO, "Processing inventory update: FruitID={0}, NewQuantity={1}, ShopID={2}",
                    new Object[] { fruitId, newQuantity, shopId });

            if (newQuantity < 0) {
                message = "Quantity cannot be negative.";
                LOGGER.log(Level.WARNING, "Invalid negative quantity received: {0}", newQuantity);
            } else {
                // --- Call the DB method to set the inventory quantity ---
                // The shopId passed here is guaranteed to be the user's own shopId
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
        }
        response.sendRedirect(redirectUrl);
    }

    // getServletInfo() remains the same
    @Override
    public String getServletInfo() {
        return "Servlet for updating shop fruit inventory";
    }
}