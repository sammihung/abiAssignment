package ict.servlet;

import ict.bean.InventoryBean;
import ict.bean.UserBean;
import ict.db.BorrowingDB; // Assuming getAllInventory is here

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet; // Import the annotation
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
 * Servlet for Senior Management to view all inventory records.
 */
// *** ADD THIS ANNOTATION ***
@WebServlet(name = "ListAllInventoryController", urlPatterns = {"/listAllInventory"})
public class ListAllInventoryController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ListAllInventoryController.class.getName());
    private BorrowingDB borrowingDb; // Contains inventory methods

    @Override
    public void init() throws ServletException {
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");
        String dbUrl = getServletContext().getInitParameter("dbUrl");
        // Ensure parameters are present
        if (dbUrl == null || dbUser == null /*|| dbPassword == null*/) { // Allow empty password
            throw new ServletException("Database connection parameters missing in web.xml.");
        }
        borrowingDb = new BorrowingDB(dbUrl, dbUser, dbPassword);
        LOGGER.log(Level.INFO, "ListAllInventoryController initialized.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // --- Security Check ---
        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;
        // Ensure user is logged in and is Senior Management
        if (currentUser == null || !"Senior Management".equalsIgnoreCase(currentUser.getRole())) {
             LOGGER.log(Level.WARNING, "Unauthorized access attempt to GET /listAllInventory.");
             response.sendRedirect(request.getContextPath() + "/login.jsp?error=ManagementLoginRequired");
             return;
        }

        List<InventoryBean> inventoryList = Collections.emptyList();
        try {
            // Assuming getAllInventory method exists in BorrowingDB
            inventoryList = borrowingDb.getAllInventory();
             LOGGER.log(Level.INFO, "Fetched all inventory for Senior Management.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching all inventory.", e);
            request.setAttribute("errorMessage", "Error retrieving inventory data.");
        }

        request.setAttribute("inventoryList", inventoryList);
        RequestDispatcher rd = request.getRequestDispatcher("/listAllInventory.jsp"); // Ensure this JSP exists
        rd.forward(request, response);
    }

    @Override
    public String getServletInfo() {
        return "Servlet for listing all inventory records";
    }
}
