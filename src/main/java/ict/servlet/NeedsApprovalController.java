package ict.servlet;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import ict.bean.AggregatedNeedBean;
import ict.bean.UserBean;
import ict.bean.WarehouseBean;
import ict.db.ReservationDB;
import ict.db.WarehouseDB;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "NeedsApprovalController", urlPatterns = { "/needsApproval" })
public class NeedsApprovalController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(NeedsApprovalController.class.getName());
    private ReservationDB reservationDb;
    private WarehouseDB warehouseDb;

    @Override
    public void init() throws ServletException {
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");
        String dbUrl = getServletContext().getInitParameter("dbUrl");

        if (dbUrl == null || dbUser == null) {
            LOGGER.log(Level.SEVERE, "Database connection parameters missing in web.xml.");
            throw new ServletException("Database connection parameters missing.");
        }
        reservationDb = new ReservationDB(dbUrl, dbUser, dbPassword);
        warehouseDb = new WarehouseDB(dbUrl, dbUser, dbPassword);
        LOGGER.log(Level.INFO, "NeedsApprovalController initialized.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;

        if (currentUser == null || !"Warehouse Staff".equalsIgnoreCase(currentUser.getRole()) ||
                currentUser.getWarehouseId() == null || currentUser.getWarehouseId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Unauthorized access attempt to GET /needsApproval.");
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=WarehouseStaffLoginRequired");
            return;
        }

        List<AggregatedNeedBean> needsList = Collections.emptyList();
        String warehouseCountry = "Unknown";

        try {
            int currentWarehouseId = Integer.parseInt(currentUser.getWarehouseId());

            WarehouseBean currentWarehouse = warehouseDb.getWarehouseById(currentWarehouseId);

            if (currentWarehouse != null && currentWarehouse.getCountry() != null) {
                warehouseCountry = currentWarehouse.getCountry();
                LOGGER.log(Level.INFO, "Warehouse Staff from WarehouseID={0} in Country={1} accessing needs.",
                        new Object[] { currentWarehouseId, warehouseCountry });

                needsList = reservationDb.getAggregatedNeedsByCountry(warehouseCountry);
                request.setAttribute("aggregatedNeedsList", needsList);
                request.setAttribute("warehouseCountry", warehouseCountry);
            } else {
                LOGGER.log(Level.WARNING, "Could not determine country for WarehouseID={0}", currentWarehouseId);
                request.setAttribute("errorMessage", "Could not determine your warehouse's country to fetch needs.");
            }

        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Invalid Warehouse ID format for current user: " + currentUser.getWarehouseId(),
                    e);
            request.setAttribute("errorMessage", "Invalid user profile (Warehouse ID).");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching needs for approval.", e);
            request.setAttribute("errorMessage", "An error occurred while retrieving needs data.");
        }

        RequestDispatcher rd = request.getRequestDispatcher("/needsApproval.jsp");
        rd.forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;

        if (currentUser == null || !"Warehouse Staff".equalsIgnoreCase(currentUser.getRole()) ||
                currentUser.getWarehouseId() == null || currentUser.getWarehouseId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Unauthorized POST attempt to /needsApproval.");
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=SessionExpired");
            return;
        }

        String fruitIdStr = request.getParameter("fruitId");
        String sourceCountry = request.getParameter("sourceCountry");
        String message = "Approval action failed.";
        boolean success = false;

        try {
            int fruitId = Integer.parseInt(fruitIdStr);
            String newStatus = "Approved";

            LOGGER.log(Level.INFO, "Processing approval for FruitID={0}, Country={1}",
                    new Object[] { fruitId, sourceCountry });

            int currentWarehouseId = Integer.parseInt(currentUser.getWarehouseId());
            WarehouseBean currentWarehouse = warehouseDb.getWarehouseById(currentWarehouseId);
            if (currentWarehouse == null || !currentWarehouse.getCountry().equalsIgnoreCase(sourceCountry)) {
                message = "Approval failed: You can only approve needs for your warehouse's country ("
                        + (currentWarehouse != null ? currentWarehouse.getCountry() : "Unknown") + ").";
                LOGGER.log(Level.WARNING,
                        "Warehouse staff (WarehouseID={0}, Country={1}) attempted to approve needs for different country ({2})",
                        new Object[] { currentWarehouseId,
                                (currentWarehouse != null ? currentWarehouse.getCountry() : "Unknown"),
                                sourceCountry });
            } else {
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

        String redirectUrl = "needsApproval";
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
