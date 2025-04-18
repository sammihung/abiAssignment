package ict.servlet;

import ict.bean.AggregatedNeedBean;
import ict.bean.UserBean;
import ict.bean.WarehouseBean;
import ict.db.ReservationDB;
import ict.db.WarehouseDB; // Need this to get warehouse country

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet for Warehouse Staff to view and approve aggregated fruit needs by country.
 */
@WebServlet(name = "NeedsApprovalController", urlPatterns = {"/needsApproval"})
public class NeedsApprovalController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(NeedsApprovalController.class.getName());
    private ReservationDB reservationDb;
    private WarehouseDB warehouseDb; // To get warehouse details

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
        warehouseDb = new WarehouseDB(dbUrl, dbUser, dbPassword); // Initialize WarehouseDB
        LOGGER.log(Level.INFO, "NeedsApprovalController initialized.");
    }

    /**
     * Handles GET requests: Displays aggregated needs for the staff's warehouse country.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // --- Security Check ---
        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;

        if (currentUser == null || !"Warehouse Staff".equalsIgnoreCase(currentUser.getRole()) ||
            currentUser.getWarehouseId() == null || currentUser.getWarehouseId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Unauthorized access attempt to GET /needsApproval.");
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=WarehouseStaffLoginRequired");
            return;
        }

        List<AggregatedNeedBean> needsList = Collections.emptyList(); // Default to empty list
        String warehouseCountry = "Unknown";

        try {
            int currentWarehouseId = Integer.parseInt(currentUser.getWarehouseId());

            // Get the country of the current user's warehouse
            // Assumes getWarehouseById is added to WarehouseDB
            WarehouseBean currentWarehouse = warehouseDb.getWarehouseById(currentWarehouseId);

            if (currentWarehouse != null && currentWarehouse.getCountry() != null) {
                warehouseCountry = currentWarehouse.getCountry();
                LOGGER.log(Level.INFO, "Warehouse Staff from WarehouseID={0} in Country={1} accessing needs.",
                           new Object[]{currentWarehouseId, warehouseCountry});

                // Fetch aggregated needs for this country
                needsList = reservationDb.getAggregatedNeedsByCountry(warehouseCountry);
                request.setAttribute("aggregatedNeedsList", needsList);
                request.setAttribute("warehouseCountry", warehouseCountry); // For display on page
            } else {
                 LOGGER.log(Level.WARNING, "Could not determine country for WarehouseID={0}", currentWarehouseId);
                 request.setAttribute("errorMessage", "Could not determine your warehouse's country to fetch needs.");
            }

        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Invalid Warehouse ID format for current user: " + currentUser.getWarehouseId(), e);
            request.setAttribute("errorMessage", "Invalid user profile (Warehouse ID).");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching needs for approval.", e);
            request.setAttribute("errorMessage", "An error occurred while retrieving needs data.");
        }

        // Forward to the JSP page
        RequestDispatcher rd = request.getRequestDispatcher("/needsApproval.jsp");
        rd.forward(request, response);
    }

    /**
     * Handles POST requests: Processes the approval action.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // --- Security Check ---
        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;

        if (currentUser == null || !"Warehouse Staff".equalsIgnoreCase(currentUser.getRole()) ||
            currentUser.getWarehouseId() == null || currentUser.getWarehouseId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Unauthorized POST attempt to /needsApproval.");
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=SessionExpired");
            return;
        }

        // --- Get parameters ---
        String fruitIdStr = request.getParameter("fruitId");
        String sourceCountry = request.getParameter("sourceCountry"); // Pass country for verification
        String message = "Approval action failed.";
        boolean success = false;

        try {
            int fruitId = Integer.parseInt(fruitIdStr);
            String newStatus = "Approved"; // Or could be passed as a parameter if more actions exist

            LOGGER.log(Level.INFO, "Processing approval for FruitID={0}, Country={1}",
                       new Object[]{fruitId, sourceCountry});

            // Verify the staff's warehouse country matches the fruit's source country (optional but good practice)
            int currentWarehouseId = Integer.parseInt(currentUser.getWarehouseId());
            WarehouseBean currentWarehouse = warehouseDb.getWarehouseById(currentWarehouseId);
            if(currentWarehouse == null || !currentWarehouse.getCountry().equalsIgnoreCase(sourceCountry)){
                 message = "Approval failed: You can only approve needs for your warehouse's country ("+ (currentWarehouse != null ? currentWarehouse.getCountry() : "Unknown") +").";
                 LOGGER.log(Level.WARNING, "Warehouse staff (WarehouseID={0}, Country={1}) attempted to approve needs for different country ({2})",
                            new Object[]{currentWarehouseId, (currentWarehouse != null ? currentWarehouse.getCountry() : "Unknown"), sourceCountry});
            } else {
                 // --- Call the DB method to approve reservations ---
                 boolean approved = reservationDb.approveReservationsForFruit(fruitId, sourceCountry, newStatus);
                 if (approved) {
                     message = "Reservations for the selected fruit approved successfully!";
                     success = true;
                 } else {
                     message = "No pending reservations found for the selected fruit to approve.";
                 }
            }

        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Invalid number format received for fruitId.", e);
            message = "Invalid input: Fruit ID must be a number.";
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing reservation approval.", e);
            message = "An unexpected error occurred during approval.";
        }

        // --- Redirect back to the GET handler to refresh the page ---
        String redirectUrl = "needsApproval"; // Relative URL to self (doGet)
        if (success) {
            redirectUrl += "?message=" + java.net.URLEncoder.encode(message, "UTF-8");
        } else {
            redirectUrl += "?error=" + java.net.URLEncoder.encode(message, "UTF-8");
        }
        response.sendRedirect(redirectUrl);
    }

    @Override
    public String getServletInfo() {
        return "Servlet for Warehouse Staff to approve aggregated fruit needs";
    }
}
