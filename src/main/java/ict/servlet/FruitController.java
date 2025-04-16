package ict.servlet;

import ict.db.FruitDB; // Use the updated DB class
import ict.bean.FruitBean; // Use the updated Bean class (optional here, but good practice)
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet to handle creating and potentially listing Fruits.
 */
// Updated URL pattern
@WebServlet(name = "FruitController", urlPatterns = {"/manageFruits"})
public class FruitController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(FruitController.class.getName());
    private FruitDB fruitDb; // Use the updated DB class name

    @Override
    public void init() throws ServletException {
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");
        String dbUrl = getServletContext().getInitParameter("dbUrl");

        if (dbUrl == null || dbUser == null /*|| dbPassword == null*/) {
            LOGGER.log(Level.SEVERE, "Database connection parameters missing in web.xml for FruitController.");
            throw new ServletException("Database connection parameters missing.");
        }

        // Initialize the updated FruitDB
        fruitDb = new FruitDB(dbUrl, dbUser, dbPassword);
        LOGGER.log(Level.INFO, "FruitController initialized.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userInfo") == null) {
            LOGGER.log(Level.WARNING, "Unauthorized access attempt to GET /manageFruits.");
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=AuthenticationRequired");
            return;
        }

        // Forward to the JSP page to display the form for creating a fruit
        LOGGER.log(Level.INFO, "Forwarding to createFruit.jsp");
        RequestDispatcher rd = request.getRequestDispatcher("/createFruit.jsp"); // Updated JSP name
        rd.forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userInfo") == null) {
            LOGGER.log(Level.WARNING, "Unauthorized access attempt to POST /manageFruits.");
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=AuthenticationRequired");
            return;
        }

        // Retrieve form data - updated parameter names
        String fruitName = request.getParameter("fruitName");
        String sourceCountry = request.getParameter("sourceCountry");

        LOGGER.log(Level.INFO, "Received POST request to add fruit. Name='{0}', Country='{1}'",
                   new Object[]{fruitName, sourceCountry});

        // Basic Validation
        if (fruitName == null || fruitName.trim().isEmpty() ||
            sourceCountry == null || sourceCountry.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Fruit name or source country is empty.");
            request.setAttribute("errorMessage", "Both Fruit Name and Source Country are required.");
            // Forward back to the form with the error message and retain values
            request.setAttribute("fruitNameValue", fruitName);
            request.setAttribute("sourceCountryValue", sourceCountry);
            RequestDispatcher rd = request.getRequestDispatcher("/createFruit.jsp"); // Updated JSP name
            rd.forward(request, response);
            return;
        }

        // Attempt to add the fruit to the database using the updated DB method
        boolean success = fruitDb.addFruit(fruitName, sourceCountry);

        if (success) {
            LOGGER.log(Level.INFO, "Fruit '{0}' added successfully.", fruitName);
            // Redirect back to the form page with a success message
            response.sendRedirect(request.getContextPath() + "/manageFruits?message=Fruit+added+successfully!");
        } else {
            LOGGER.log(Level.WARNING, "Failed to add fruit '{0}'.", fruitName);
            // Forward back to the form with an error message and retain entered values
            request.setAttribute("errorMessage", "Failed to add fruit. The name might already exist, or a database error occurred.");
            request.setAttribute("fruitNameValue", fruitName); // Retain values
            request.setAttribute("sourceCountryValue", sourceCountry);
            RequestDispatcher rd = request.getRequestDispatcher("/createFruit.jsp"); // Updated JSP name
            rd.forward(request, response);
        }
    }
}
