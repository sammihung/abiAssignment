package ict.servlet;

// Import the new bean
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map; // Assuming methods are here
import java.util.logging.Level; // Still needed? Maybe not if info is in BorrowableFruitInfoBean
import java.util.logging.Logger;

import ict.bean.BakeryShopBean;
import ict.bean.BorrowableFruitInfoBean;
import ict.bean.UserBean;
import ict.db.BakeryShopDB;
import ict.db.BorrowingDB;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Servlet to handle batch borrowing requests from other shops in the same city.
 * GET shows fruits with lender availability, POST submits request to one chosen
 * lender.
 */
@WebServlet(name = "BatchBorrowController", urlPatterns = { "/batchBorrowFruit" })
public class BatchBorrowController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(BatchBorrowController.class.getName());
    private BorrowingDB borrowingDb;
    // private FruitDB fruitDb; // May not be needed directly if using
    // BorrowableFruitInfoBean
    private BakeryShopDB bakeryShopDb;

    @Override
    public void init() throws ServletException {
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");
        String dbUrl = getServletContext().getInitParameter("dbUrl");
        if (dbUrl == null || dbUser == null) {
            throw new ServletException("Database connection parameters missing.");
        }
        borrowingDb = new BorrowingDB(dbUrl, dbUser, dbPassword);
        // fruitDb = new FruitDB(dbUrl, dbUser, dbPassword); // Initialize if still
        // needed elsewhere
        bakeryShopDb = new BakeryShopDB(dbUrl, dbUser, dbPassword);
        LOGGER.log(Level.INFO, "BatchBorrowController initialized.");
    }

    /**
     * Handles GET requests: Displays the batch borrowing form with lender
     * availability.
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

        List<BorrowableFruitInfoBean> borrowableFruits = Collections.emptyList();
        List<BakeryShopBean> potentialLenders = Collections.emptyList(); // Still need this for the dropdown

        try {
            int currentShopId = Integer.parseInt(currentUser.getShopId());
            BakeryShopBean currentShop = bakeryShopDb.getShopById(currentShopId); // Need shop details for city

            if (currentShop != null && currentShop.getCity() != null) {
                String city = currentShop.getCity();
                // Fetch fruits including lender info for the table display
                borrowableFruits = borrowingDb.getBorrowableFruitsWithLenderInfo(city, currentShopId);
                // Fetch other shops in the same city for the lender selection dropdown
                potentialLenders = borrowingDb.getOtherShopsInCity(city, currentShopId);

                request.setAttribute("currentCity", city);
                LOGGER.log(Level.INFO, "Fetched {0} borrowable fruits with lender info for city {1}",
                        new Object[] { borrowableFruits.size(), city });
            } else {
                LOGGER.log(Level.WARNING, "Could not determine city for current shop ID: {0}", currentShopId);
                request.setAttribute("errorMessage", "Could not determine your shop's city.");
            }

        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Invalid Shop ID format for user: " + currentUser.getUsername(), e);
            request.setAttribute("errorMessage", "Invalid user profile (Shop ID).");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error preparing batch borrow form.", e);
            request.setAttribute("errorMessage", "Error loading borrowing page.");
        }

        request.setAttribute("borrowableFruits", borrowableFruits); // Pass the enhanced list
        request.setAttribute("potentialLenders", potentialLenders); // Pass lenders for dropdown
        RequestDispatcher rd = request.getRequestDispatcher("/batchBorrowFruit.jsp");
        rd.forward(request, response);
    }

    /**
     * Handles POST requests: Processes the batch borrowing submission to a SINGLE
     * lender.
     * (This logic remains the same as before)
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

        String message = "Borrow request submission failed.";
        boolean success = false;
        List<Integer> requestedFruitIds = new ArrayList<>();
        List<Integer> requestedQuantities = new ArrayList<>();
        String lendingShopIdStr = request.getParameter("lendingShopId"); // Get the SINGLE chosen lender

        try {
            int borrowingShopId = Integer.parseInt(currentUser.getShopId());
            int lendingShopId = -1;

            if (lendingShopIdStr == null || lendingShopIdStr.trim().isEmpty()) {
                message = "Please select a shop to borrow from.";
            } else {
                lendingShopId = Integer.parseInt(lendingShopIdStr);

                // --- Parse multiple fruit quantities ---
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
                                    requestedFruitIds.add(fruitId);
                                    requestedQuantities.add(quantity);
                                }
                            }
                        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                            LOGGER.log(Level.WARNING, "Invalid quantity/fruit ID format for param: " + paramName, e);
                        }
                    }
                } // End parameter loop

                if (requestedFruitIds.isEmpty()) {
                    message = "No items requested (quantity must be greater than 0).";
                } else {
                    LOGGER.log(Level.INFO,
                            "Processing batch borrow request from ShopID={0} to ShopID={1} for {2} items.",
                            new Object[] { borrowingShopId, lendingShopId, requestedFruitIds.size() });
                    // --- Call DB method to create multiple pending requests TO THE SINGLE LENDER
                    // ---
                    // Assumes createMultipleBorrowRequests exists and handles this
                    message = borrowingDb.createMultipleBorrowRequests(borrowingShopId, lendingShopId,
                            requestedFruitIds, requestedQuantities);
                    if (message.toLowerCase().contains("success")) {
                        success = true;
                    }
                }
            }
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Invalid number format received for Shop ID.", e);
            message = "Invalid Shop ID provided.";
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing batch borrow submission.", e);
            message = "An unexpected error occurred.";
        }

        // --- Redirect back to the form page ---
        String redirectUrl = "batchBorrowFruit";
        if (success) {
            redirectUrl += "?message=" + java.net.URLEncoder.encode(message, "UTF-8");
        } else {
            redirectUrl += "?error=" + java.net.URLEncoder.encode(message, "UTF-8");
        }
        response.sendRedirect(redirectUrl);
    }

    @Override
    public String getServletInfo() {
        return "Servlet for batch borrowing fruits from other shops";
    }
}
