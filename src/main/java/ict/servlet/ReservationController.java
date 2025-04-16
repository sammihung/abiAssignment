package ict.servlet;

import ict.bean.FruitBean;
import ict.bean.UserBean; // To get the shop ID of the logged-in user
import ict.db.FruitDB;
import ict.db.ReservationDB;
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
 * Servlet to handle fruit reservations from source.
 */
@WebServlet(name = "ReservationController", urlPatterns = {"/reserveFruit"})
public class ReservationController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ReservationController.class.getName());
    private ReservationDB reservationDb;
    private FruitDB fruitDb;
    // Assume ShopDB exists or adapt BakeryShopDB
    // private ShopDB shopDb;

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

        reservationDb = new ReservationDB(dbUrl, dbUser, dbPassword);
        fruitDb = new FruitDB(dbUrl, dbUser, dbPassword);
        // shopDb = new ShopDB(dbUrl, dbUser, dbPassword);
        LOGGER.log(Level.INFO, "ReservationController initialized.");
    }

    /**
     * Handles GET requests: Displays the reservation form.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // --- Security Check: Ensure user is logged in and is shop staff ---
        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;

        if (currentUser == null || currentUser.getShopId() == null || currentUser.getShopId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Unauthorized access attempt to GET /reserveFruit. User not logged in or not associated with a shop.");
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=ShopLoginRequired");
            return;
        }
         // Optional: Check role specifically if needed
         // if (!"Bakery shop staff".equalsIgnoreCase(currentUser.getRole())) { ... }


        try {
            // Fetch list of fruits for the dropdown
            List<FruitBean> fruits = fruitDb.getAllFruits();
            request.setAttribute("allFruits", fruits);
            LOGGER.log(Level.INFO, "Fetched {0} fruits for reservation form.", fruits.size());

            // Forward to the JSP page
            RequestDispatcher rd = request.getRequestDispatcher("/reserveFruit.jsp");
            rd.forward(request, response);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error preparing reservation form.", e);
            // Handle error - maybe redirect to an error page or back with a message
            request.setAttribute("errorMessage", "Error loading reservation page.");
             RequestDispatcher rd = request.getRequestDispatcher("/reserveFruit.jsp"); // Show error on the same page
            rd.forward(request, response);
        }
    }

    /**
     * Handles POST requests: Processes the reservation submission.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // --- Security Check: Ensure user is logged in and is shop staff ---
        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;

        if (currentUser == null || currentUser.getShopId() == null || currentUser.getShopId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Unauthorized POST attempt to /reserveFruit.");
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=SessionExpired");
            return;
        }
         // Optional: Role check

        // --- Get form parameters ---
        String fruitIdStr = request.getParameter("fruitId");
        String quantityStr = request.getParameter("quantity");
        String message = "Reservation processing failed."; // Default message
        boolean success = false;

        try {
            int fruitId = Integer.parseInt(fruitIdStr);
            int quantity = Integer.parseInt(quantityStr);
            int shopId = Integer.parseInt(currentUser.getShopId()); // Get shop ID from logged-in user

             LOGGER.log(Level.INFO, "Processing reservation request: FruitID={0}, Quantity={1}, ShopID={2}",
                       new Object[]{fruitId, quantity, shopId});


            if (quantity <= 0) {
                 message = "Quantity must be a positive number.";
                 LOGGER.log(Level.WARNING, "Invalid quantity received: {0}", quantity);
            } else {
                 // --- Call the core reservation logic in ReservationDB ---
                 message = reservationDb.createReservationFromSource(fruitId, shopId, quantity);
                 // Check if the message indicates success (you might want a more robust way, like a boolean return)
                 if (message.toLowerCase().contains("success")) {
                      success = true;
                 }
            }

        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Invalid number format received for fruitId or quantity.", e);
            message = "Invalid input: Fruit ID and Quantity must be numbers.";
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing reservation submission.", e);
            message = "An unexpected error occurred while processing your reservation.";
        }

        // --- Redirect back to the form page with a status message ---
        // Using redirect attributes or URL parameters to pass the message
        String redirectUrl = "reserveFruit"; // Relative URL to self
        if (success) {
             redirectUrl += "?message=" + java.net.URLEncoder.encode(message, "UTF-8");
        } else {
             redirectUrl += "?error=" + java.net.URLEncoder.encode(message, "UTF-8");
             // Optionally retain form values if redirecting back to the same form on error
             // request.setAttribute("selectedFruitId", fruitIdStr);
             // request.setAttribute("enteredQuantity", quantityStr);
             // You might need to re-fetch the fruit list as well if forwarding instead of redirecting
        }

        response.sendRedirect(redirectUrl);

        /*
        // Alternative: Forward back to the JSP (useful if retaining form values on error)
        if (success) {
            request.setAttribute("message", message);
        } else {
            request.setAttribute("errorMessage", message);
            // Re-fetch fruit list if needed for the form dropdown
             List<FruitBean> fruits = fruitDb.getAllFruits();
             request.setAttribute("allFruits", fruits);
             // Retain submitted values
             request.setAttribute("selectedFruitId", fruitIdStr);
             request.setAttribute("enteredQuantity", quantityStr);
        }
         RequestDispatcher rd = request.getRequestDispatcher("/reserveFruit.jsp");
         rd.forward(request, response);
        */
    }

    @Override
    public String getServletInfo() {
        return "Servlet for handling fruit reservations from source";
    }
}
