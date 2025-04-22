package ict.servlet;

import ict.bean.OrderableFruitBean;
import ict.bean.UserBean;
import ict.db.BorrowingDB; // Assuming new methods are here

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet to handle ordering multiple fruits from source warehouses.
 */
@WebServlet(name = "OrderFromSourceController", urlPatterns = { "/orderFromSource" })
public class OrderFromSourceController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(OrderFromSourceController.class.getName());
    private BorrowingDB borrowingDb; // Contains order methods

    @Override
    public void init() throws ServletException {
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");
        String dbUrl = getServletContext().getInitParameter("dbUrl");
        if (dbUrl == null || dbUser == null) {
            throw new ServletException("Database connection parameters missing.");
        }
        borrowingDb = new BorrowingDB(dbUrl, dbUser, dbPassword);
        LOGGER.log(Level.INFO, "OrderFromSourceController initialized.");
    }

    /**
     * Handles GET requests: Displays the multi-item order form.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // --- Security Check: Shop Staff logged in ---
        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;
        if (currentUser == null || !"Bakery shop staff".equalsIgnoreCase(currentUser.getRole()) ||
                currentUser.getShopId() == null || currentUser.getShopId().trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=ShopStaffLoginRequired");
            return;
        }

        try {
            // Fetch list of fruits available for ordering from source
            List<OrderableFruitBean> orderableFruits = borrowingDb.getOrderableFruitsFromSource();
            request.setAttribute("orderableFruits", orderableFruits);
            LOGGER.log(Level.INFO, "Fetched {0} orderable fruits.", orderableFruits.size());

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching orderable fruits.", e);
            request.setAttribute("errorMessage", "Error loading order page.");
        }

        // Forward to the JSP page
        RequestDispatcher rd = request.getRequestDispatcher("/orderFromSource.jsp");
        rd.forward(request, response);
    }

    /**
     * Handles POST requests: Processes the batch order submission.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // --- Security Check ---
        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;
        if (currentUser == null || !"Bakery shop staff".equalsIgnoreCase(currentUser.getRole()) ||
                currentUser.getShopId() == null || currentUser.getShopId().trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=SessionExpired");
            return;
        }

        String message = "Order processing failed.";
        boolean success = false;
        List<Integer> orderedFruitIds = new ArrayList<>();
        List<Integer> orderedQuantities = new ArrayList<>();

        try {
            int shopId = Integer.parseInt(currentUser.getShopId());

            // --- Parse multiple fruit quantities from the form ---
            // Assumes form inputs are named "quantity_<fruitId>"
            Map<String, String[]> parameterMap = request.getParameterMap();
            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                String paramName = entry.getKey();
                if (paramName.startsWith("quantity_")) {
                    try {
                        String quantityStr = entry.getValue()[0];
                        if (quantityStr != null && !quantityStr.trim().isEmpty()) {
                            int quantity = Integer.parseInt(quantityStr.trim());
                            if (quantity > 0) { // Only add items with quantity > 0
                                int fruitId = Integer.parseInt(paramName.substring("quantity_".length()));
                                orderedFruitIds.add(fruitId);
                                orderedQuantities.add(quantity);
                                LOGGER.log(Level.FINER, "Parsed order item: FruitID={0}, Quantity={1}",
                                        new Object[] { fruitId, quantity });
                            }
                        }
                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                        LOGGER.log(Level.WARNING, "Invalid quantity or fruit ID format for parameter: " + paramName, e);
                        // Skip this item or handle error more strictly
                    }
                }
            }

            if (orderedFruitIds.isEmpty()) {
                message = "No items were selected for ordering (quantity must be greater than 0).";
            } else {
                LOGGER.log(Level.INFO, "Processing order for ShopID={0} with {1} items.",
                        new Object[] { shopId, orderedFruitIds.size() });
                // --- Call the DB method to process the batch ---
                message = borrowingDb.createMultipleReservations(shopId, orderedFruitIds, orderedQuantities);
                if (message.toLowerCase().contains("success")) {
                    success = true;
                }
            }

        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Invalid Shop ID format for user: " + currentUser.getUsername(), e);
            message = "Invalid user profile (Shop ID).";
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing order submission.", e);
            message = "An unexpected error occurred while submitting your order.";
        }

        // --- Redirect back to the order form page with a status message ---
        String redirectUrl = "orderFromSource"; // Relative URL to self (doGet)
        if (success) {
            redirectUrl += "?message=" + java.net.URLEncoder.encode(message, "UTF-8");
        } else {
            redirectUrl += "?error=" + java.net.URLEncoder.encode(message, "UTF-8");
            // Note: Retaining quantities on error is complex with batch forms,
            // usually just show the error and let user refill.
        }
        response.sendRedirect(redirectUrl);
    }

    @Override
    public String getServletInfo() {
        return "Servlet for ordering multiple fruits from source";
    }
}
