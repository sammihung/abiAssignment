package ict.servlet;

import java.io.IOException;
import java.util.Collections; // Assuming UserDB has getAllUsersAsMap and getUsersGroupedByRole
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import ict.bean.UserBean;
import ict.db.UserDB;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession; // Import Collections

/**
 * Servlet to list users. Shows all users for Senior Management,
 * otherwise shows users with the same role as the logged-in user.
 */
@WebServlet(name = "ListUsersController", urlPatterns = { "/listUsers" }) // Ensure annotation is present
public class ListUsersController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ListUsersController.class.getName());
    private UserDB userDb;

    @Override
    public void init() throws ServletException {
        // Initialize UserDB
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");
        String dbUrl = getServletContext().getInitParameter("dbUrl");
        if (dbUrl == null || dbUser == null /* || dbPassword == null */) {
            throw new ServletException("Database connection parameters missing.");
        }
        userDb = new UserDB(dbUrl, dbUser, dbPassword);
        LOGGER.log(Level.INFO, "ListUsersController initialized.");
    }

    /**
     * Handles GET requests: Fetches and displays user list based on role.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // --- Security Check (Basic Login Check - More specific checks via Filter/RBAC)
        // ---
        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;

        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=LoginRequired");
            return;
        }

        // Declare variable with the correct type Map<String, List<UserBean>>
        Map<String, List<UserBean>> usersByRole = Collections.emptyMap(); // Initialize to empty map
        String listTitle = "User List"; // Default title

        try {
            String userRole = currentUser.getRole();

            // --- Role-Based Data Fetching ---
            if ("Senior Management".equalsIgnoreCase(userRole)) {
                listTitle = "All Users";
                // Call the method to get all users (ensure this exists in UserDB)
                usersByRole = userDb.getAllUsersAsMap();
                LOGGER.log(Level.INFO, "Fetching all users for Senior Management.");
            } else if (userRole != null && !userRole.trim().isEmpty()) {
                listTitle = "Users with Role: " + userRole;
                // *** THIS IS THE MODIFIED LINE ***
                // Call the new method that returns Map<String, List<UserBean>>
                usersByRole = userDb.getUsersGroupedByRole(userRole);
                // *** END OF MODIFIED LINE ***
                LOGGER.log(Level.INFO, "Fetching users for role: {0}", userRole);
            } else {
                // Handle cases where role might be missing (though filter should prevent this)
                LOGGER.log(Level.WARNING, "User {0} has missing role, cannot list users.", currentUser.getUsername());
                request.setAttribute("errorMessage", "Your user profile is missing role information.");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching user list.", e);
            request.setAttribute("errorMessage", "Error retrieving user data.");
        }

        // Set attributes and forward to the JSP
        request.setAttribute("usersByRole", usersByRole);
        request.setAttribute("listTitle", listTitle); // Pass title to JSP
        RequestDispatcher rd = request.getRequestDispatcher("/listUsers.jsp"); // Ensure this JSP exists
        rd.forward(request, response);
    }

    @Override
    public String getServletInfo() {
        return "Servlet for listing users based on role";
    }
}
