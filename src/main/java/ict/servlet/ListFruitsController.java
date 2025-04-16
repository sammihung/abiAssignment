package ict.servlet;

import ict.bean.FruitBean; // Import the bean
import ict.db.FruitDB;     // Import the DB class
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet to handle requests for listing all fruits.
 */
@WebServlet(name = "ListFruitsController", urlPatterns = {"/listFruits"}) // URL pattern for this servlet
public class ListFruitsController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ListFruitsController.class.getName());
    private FruitDB fruitDb;

    @Override
    public void init() throws ServletException {
        // Get DB connection details from web.xml context parameters
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");
        String dbUrl = getServletContext().getInitParameter("dbUrl");

        // Validate parameters
        if (dbUrl == null || dbUser == null /*|| dbPassword == null*/) { // Allow empty password
            LOGGER.log(Level.SEVERE, "Database connection parameters missing in web.xml for ListFruitsController.");
            throw new ServletException("Database connection parameters missing.");
        }

        // Initialize the FruitDB
        fruitDb = new FruitDB(dbUrl, dbUser, dbPassword);
        LOGGER.log(Level.INFO, "ListFruitsController initialized.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Security Check: Ensure user is logged in
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userInfo") == null) {
            LOGGER.log(Level.WARNING, "Unauthorized access attempt to GET /listFruits.");
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=AuthenticationRequired");
            return;
        }

        try {
            // Fetch all fruits from the database
            ArrayList<FruitBean> fruits = fruitDb.getAllFruits();

            // Set the list of fruits as a request attribute
            request.setAttribute("fruitList", fruits);
            LOGGER.log(Level.INFO, "Fetched {0} fruits. Forwarding to listFruits.jsp", fruits.size());

            // Forward the request to the JSP page for display
            RequestDispatcher rd = request.getRequestDispatcher("/listFruits.jsp");
            rd.forward(request, response);

        } catch (Exception e) { // Catch potential errors during DB access
            LOGGER.log(Level.SEVERE, "Error fetching fruits list.", e);
            // Set an error message and forward to an error page or the list page with the message
            request.setAttribute("errorMessage", "An error occurred while retrieving the fruit list.");
            RequestDispatcher rd = request.getRequestDispatcher("/listFruits.jsp"); // Forward to list page to show error
            rd.forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Typically, a list page doesn't handle POST, redirect GET requests
        doGet(request, response);
    }
}
