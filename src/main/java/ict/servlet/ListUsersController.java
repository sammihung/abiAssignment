package ict.servlet;

import java.io.IOException;
import java.util.Collections;
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
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "ListUsersController", urlPatterns = { "/listUsers" })
public class ListUsersController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ListUsersController.class.getName());
    private UserDB userDb;

    @Override
    public void init() throws ServletException {
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");
        String dbUrl = getServletContext().getInitParameter("dbUrl");
        if (dbUrl == null || dbUser == null) {
            throw new ServletException("Database connection parameters missing.");
        }
        userDb = new UserDB(dbUrl, dbUser, dbPassword);
        LOGGER.log(Level.INFO, "ListUsersController initialized.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;

        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=LoginRequired");
            return;
        }

        Map<String, List<UserBean>> usersByRole = Collections.emptyMap();
        String listTitle = "User List";

        try {
            String userRole = currentUser.getRole();

            if ("Senior Management".equalsIgnoreCase(userRole)) {
                listTitle = "All Users";
                usersByRole = userDb.getAllUsersAsMap();
                LOGGER.log(Level.INFO, "Fetching all users for Senior Management.");
            } else if (userRole != null && !userRole.trim().isEmpty()) {
                listTitle = "Users with Role: " + userRole;
                usersByRole = userDb.getUsersGroupedByRole(userRole);
                LOGGER.log(Level.INFO, "Fetching users for role: {0}", userRole);
            } else {
                LOGGER.log(Level.WARNING, "User {0} has missing role, cannot list users.", currentUser.getUsername());
                request.setAttribute("errorMessage", "Your user profile is missing role information.");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching user list.", e);
            request.setAttribute("errorMessage", "Error retrieving user data.");
        }

        request.setAttribute("usersByRole", usersByRole);
        request.setAttribute("listTitle", listTitle);
        RequestDispatcher rd = request.getRequestDispatcher("/listUsers.jsp");
        rd.forward(request, response);
    }

    @Override
    public String getServletInfo() {
        return "Servlet for listing users based on role";
    }
}
