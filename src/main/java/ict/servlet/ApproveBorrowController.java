package ict.servlet;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import ict.bean.BorrowingBean;
import ict.bean.UserBean;
import ict.db.BorrowingDB;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "ApproveBorrowController", urlPatterns = { "/approveBorrow" })
public class ApproveBorrowController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ApproveBorrowController.class.getName());
    private BorrowingDB borrowingDb;

    @Override
    public void init() throws ServletException {
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");
        String dbUrl = getServletContext().getInitParameter("dbUrl");
        if (dbUrl == null || dbUser == null) {
            throw new ServletException("Database connection parameters missing.");
        }
        borrowingDb = new BorrowingDB(dbUrl, dbUser, dbPassword);
        LOGGER.log(Level.INFO, "ApproveBorrowController initialized.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        UserBean currentUser = (UserBean) session.getAttribute("userInfo");

        List<BorrowingBean> pendingRequests = Collections.emptyList();
        try {
            int lendingShopId = Integer.parseInt(currentUser.getShopId());
            pendingRequests = borrowingDb.getPendingBorrowRequests(lendingShopId);
            request.setAttribute("pendingBorrowRequests", pendingRequests);
            LOGGER.log(Level.INFO, "Fetched {0} pending borrow requests for lending ShopID={1}",
                    new Object[] { pendingRequests.size(), lendingShopId });

        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Invalid Shop ID format in session for user: " + currentUser.getUsername(), e);
            request.setAttribute("errorMessage", "Invalid user profile (Shop ID).");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching pending borrow requests.", e);
            request.setAttribute("errorMessage", "Error retrieving borrow requests.");
        }

        RequestDispatcher rd = request.getRequestDispatcher("/approveBorrow.jsp");
        rd.forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        UserBean currentUser = (UserBean) session.getAttribute("userInfo");

        String borrowingIdStr = request.getParameter("borrowingId");
        String action = request.getParameter("action");
        String message = "Action failed.";
        boolean success = false;

        try {
            int borrowingId = Integer.parseInt(borrowingIdStr);
            int currentShopId = Integer.parseInt(currentUser.getShopId());

            BorrowingBean requestDetails = borrowingDb.getBorrowingById(borrowingId);

            if (requestDetails == null) {
                message = "Borrow request not found.";
                LOGGER.log(Level.WARNING, "Attempt to {0} non-existent BorrowingID={1}",
                        new Object[] { action, borrowingId });
            } else if (requestDetails.getBorrowingShopId() != currentShopId) {
                message = "Authorization denied. You can only manage requests directed to your shop.";
                LOGGER.log(Level.WARNING,
                        "Auth Denied: Shop {0} tried to {1} BorrowingID={2} which belongs to Shop {3}",
                        new Object[] { currentShopId, action, borrowingId, requestDetails.getBorrowingShopId() });
            } else if (!"Pending".equalsIgnoreCase(requestDetails.getStatus())) {
                message = "Action failed: Request is no longer in 'Pending' status (Current: "
                        + requestDetails.getStatus() + ").";
                LOGGER.log(Level.WARNING, "Action '{0}' failed for BorrowingID={1}. Status is already {2}.",
                        new Object[] { action, borrowingId, requestDetails.getStatus() });
            } else {
                LOGGER.log(Level.INFO, "Processing action '{0}' for BorrowingID={1} by ShopID={2}",
                        new Object[] { action, borrowingId, currentShopId });

                if ("approve".equals(action)) {
                    message = borrowingDb.approveBorrowRequest(borrowingId, currentShopId);
                } else if ("reject".equals(action)) {
                    message = borrowingDb.rejectBorrowRequest(borrowingId, currentShopId);
                } else {
                    message = "Invalid action specified.";
                    LOGGER.log(Level.WARNING, "Invalid action received: {0}", action);
                }

                if (message.toLowerCase().contains("success") || message.toLowerCase().contains("approved")
                        || message.toLowerCase().contains("rejected")) {
                    success = true;
                }
            }

        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Invalid number format received for borrowingId.", e);
            message = "Invalid input: Borrowing ID must be a number.";
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing borrow approval/rejection.", e);
            message = "An unexpected error occurred.";
        }

        String redirectUrl = "approveBorrow";
        if (success) {
            redirectUrl += "?message=" + java.net.URLEncoder.encode(message, "UTF-8");
        } else {
            redirectUrl += "?error=" + java.net.URLEncoder.encode(message, "UTF-8");
        }
        response.sendRedirect(redirectUrl);
    }

    @Override
    public String getServletInfo() {
        return "Servlet for approving/rejecting borrow requests";
    }
}