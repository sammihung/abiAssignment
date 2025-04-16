package ict.servlet;

import ict.bean.FruitBean;
// Updated import
import ict.bean.BakeryShopBean;
import ict.bean.UserBean;
import ict.db.BorrowingDB;
import ict.db.FruitDB;
// Updated import
import ict.db.BakeryShopDB;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet to handle borrowing fruits between shops in the same city.
 */
@WebServlet(name = "BorrowingController", urlPatterns = {"/borrowFruit"})
public class BorrowingController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(BorrowingController.class.getName());
    private BorrowingDB borrowingDb;
    private FruitDB fruitDb;
    // Updated field type
    private BakeryShopDB bakeryShopDb; // To get current shop's city

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
        // Updated initialization
        bakeryShopDb = new BakeryShopDB(dbUrl, dbUser, dbPassword);
        LOGGER.log(Level.INFO, "BorrowingController initialized.");
    }

    /**
     * Handles GET requests: Displays the borrowing form.
     * May pre-fetch potential lenders if fruit/quantity are passed, or require user input first.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // --- Security Check: Ensure user is logged in and associated with a shop ---
        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;

        // Ensure user is logged in and has a valid shop ID associated
        if (currentUser == null || currentUser.getShopId() == null || currentUser.getShopId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Unauthorized access attempt to GET /borrowFruit.");
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=ShopLoginRequired");
            return;
        }
        // Optional: Role check

        try {
            // It's safer to parse the shop ID here and handle potential errors early
            int currentShopId;
            try {
                 currentShopId = Integer.parseInt(currentUser.getShopId());
            } catch (NumberFormatException e) {
                 LOGGER.log(Level.SEVERE, "Invalid Shop ID format for current user: " + currentUser.getShopId(), e);
                 request.setAttribute("errorMessage", "Invalid user profile (Shop ID). Cannot proceed.");
                 RequestDispatcher rd = request.getRequestDispatcher("/borrowFruit.jsp"); // Show error on form page
                 rd.forward(request, response);
                 return; // Stop further processing
            }


            // Fetch list of all fruits for the dropdown
            List<FruitBean> fruits = fruitDb.getAllFruits();
            request.setAttribute("allFruits", fruits);

            // --- Optional: Pre-fetch lenders if fruit and quantity are provided ---
            String preselectFruitIdStr = request.getParameter("fruitId");
            String preselectQuantityStr = request.getParameter("quantity");

            if (preselectFruitIdStr != null && preselectQuantityStr != null &&
                !preselectFruitIdStr.isEmpty() && !preselectQuantityStr.isEmpty()) {
                try {
                    int fruitId = Integer.parseInt(preselectFruitIdStr);
                    int quantity = Integer.parseInt(preselectQuantityStr);

                    // Get current shop's city using BakeryShopDB
                    // This relies on getShopById being added to BakeryShopDB
                    BakeryShopBean currentShop = bakeryShopDb.getShopById(currentShopId);
                    if (currentShop != null && currentShop.getCity() != null) {
                        List<Map<String, Object>> potentialLenders = borrowingDb.findPotentialLenders(fruitId, quantity, currentShop.getCity(), currentShopId);
                        request.setAttribute("potentialLenders", potentialLenders);
                        request.setAttribute("selectedFruitId", fruitId); // Retain selection
                        request.setAttribute("enteredQuantity", quantity); // Retain quantity
                         LOGGER.log(Level.INFO, "Pre-fetched {0} potential lenders for fruit {1}, qty {2}", new Object[]{potentialLenders.size(), fruitId, quantity});
                    } else {
                         LOGGER.log(Level.WARNING, "Could not find shop details or city for ShopID={0}", currentShopId);
                         request.setAttribute("errorMessage", "Could not determine your shop's city.");
                    }
                } catch (NumberFormatException e) {
                    request.setAttribute("errorMessage", "Invalid fruit ID or quantity provided for pre-fetch.");
                }
            }

            // Forward to the JSP page
            RequestDispatcher rd = request.getRequestDispatcher("/borrowFruit.jsp");
            rd.forward(request, response);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error preparing borrowing form.", e);
            request.setAttribute("errorMessage", "Error loading borrowing page.");
            RequestDispatcher rd = request.getRequestDispatcher("/borrowFruit.jsp");
            rd.forward(request, response);
        }
    }

    /**
     * Handles POST requests: Processes the borrowing submission.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // --- Security Check ---
        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;

        if (currentUser == null || currentUser.getShopId() == null || currentUser.getShopId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Unauthorized POST attempt to /borrowFruit.");
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=SessionExpired");
            return;
        }
        // Optional: Role check

        // --- Get form parameters ---
        String fruitIdStr = request.getParameter("fruitId");
        String quantityStr = request.getParameter("quantity");
        String lendingShopIdStr = request.getParameter("lendingShopId"); // ID of the shop selected to borrow from
        String message = "Borrowing processing failed.";
        boolean success = false;
        int parsedFruitId = -1; // Keep track for redirect on error
        String parsedQuantity = ""; // Keep track for redirect on error


        try {
            parsedFruitId = Integer.parseInt(fruitIdStr);
            int quantity = Integer.parseInt(quantityStr);
            parsedQuantity = quantityStr; // Store original string value
            int lendingShopId = Integer.parseInt(lendingShopIdStr);
            int borrowingShopId = Integer.parseInt(currentUser.getShopId()); // The shop initiating the request

            LOGGER.log(Level.INFO, "Processing borrowing request: FruitID={0}, Quantity={1}, LendingShopID={2}, BorrowingShopID={3}",
                       new Object[]{parsedFruitId, quantity, lendingShopId, borrowingShopId});

            // --- Call the core borrowing logic in BorrowingDB ---
            message = borrowingDb.createBorrowing(parsedFruitId, lendingShopId, borrowingShopId, quantity);

            // Check if the message indicates success
            if (message.toLowerCase().contains("success")) {
                success = true;
            }

        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Invalid number format received.", e);
            message = "Invalid input: Fruit ID, Quantity, and Lending Shop ID must be numbers.";
             // Store potentially invalid values if needed for re-population
             parsedFruitId = (fruitIdStr != null && !fruitIdStr.isEmpty()) ? tryParseInt(fruitIdStr) : -1;
             parsedQuantity = quantityStr != null ? quantityStr : "";

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing borrowing submission.", e);
            message = "An unexpected error occurred while processing the borrowing request.";
             // Store potentially valid values if exception occurred later
             parsedFruitId = (fruitIdStr != null && !fruitIdStr.isEmpty()) ? tryParseInt(fruitIdStr) : -1;
             parsedQuantity = quantityStr != null ? quantityStr : "";
        }

        // --- Redirect back to the form page with a status message ---
        String redirectUrl = "borrowFruit";
        try {
             if (success) {
                 redirectUrl += "?message=" + java.net.URLEncoder.encode(message, "UTF-8");
             } else {
                 redirectUrl += "?error=" + java.net.URLEncoder.encode(message, "UTF-8");
                 // Pass back original selections to re-populate the form
                 if (parsedFruitId != -1) {
                     redirectUrl += "&fruitId=" + parsedFruitId;
                 }
                 if (!parsedQuantity.isEmpty()) {
                    redirectUrl += "&quantity=" + java.net.URLEncoder.encode(parsedQuantity, "UTF-8");
                 }
                 // Note: We don't pass back lendingShopId as the list of lenders might need refreshing based on the (now potentially changed) quantity or fruit selection.
             }
        } catch (java.io.UnsupportedEncodingException e) {
             LOGGER.log(Level.SEVERE, "UTF-8 Encoding not supported!", e);
             // Fallback redirect without message encoding
             redirectUrl = "borrowFruit?error=EncodingError";
        }
        response.sendRedirect(redirectUrl);
    }

    // Helper method to safely parse int
    private int tryParseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1; // Indicate parsing failure
        }
    }


    @Override
    public String getServletInfo() {
        return "Servlet for handling fruit borrowing between shops";
    }
}
