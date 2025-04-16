package ict.servlet;

import ict.bean.FruitBean;
import ict.bean.UserBean; // Assuming you use UserBean for session info
import ict.db.FruitDB;
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
 * Servlet to handle updating fruit information.
 * Handles GET requests to display the update form and POST requests to process the update.
 */
@WebServlet(name = "UpdateFruitController", urlPatterns = {"/updateFruit"})
public class UpdateFruitController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(UpdateFruitController.class.getName());
    private FruitDB fruitDb; // Use the FruitDB class

    @Override
    public void init() throws ServletException {
        // Initialize FruitDB using context parameters from web.xml
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");
        String dbUrl = getServletContext().getInitParameter("dbUrl");

        // Basic validation for DB parameters
        if (dbUrl == null || dbUser == null /*|| dbPassword == null*/) { // Allow empty password potentially
            LOGGER.log(Level.SEVERE, "Database connection parameters missing in web.xml for UpdateFruitController.");
            throw new ServletException("Database connection parameters missing.");
        }
        fruitDb = new FruitDB(dbUrl, dbUser, dbPassword);
        LOGGER.log(Level.INFO, "UpdateFruitController initialized.");
    }

    /**
     * Handles GET requests. Fetches the fruit data by ID and forwards to the updateFruit.jsp page.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // --- Security Check: Ensure user is logged in ---
        HttpSession session = request.getSession(false); // Don't create session if it doesn't exist
        if (session == null || session.getAttribute("userInfo") == null) {
            LOGGER.log(Level.WARNING, "Unauthorized access attempt to GET /updateFruit. Redirecting to login.");
            request.setAttribute("errorMessage", "Please login to update fruits."); // Set error message for login page
            response.sendRedirect(request.getContextPath() + "/login.jsp"); // Redirect to login
            return;
        }
        // Optional: Add role-based access control here if needed
        // UserBean loggedInUser = (UserBean) session.getAttribute("userInfo");
        // if (!"admin".equals(loggedInUser.getRole())) { ... handle unauthorized role ... }

        // --- Get Fruit ID from request parameter ---
        String fruitIdStr = request.getParameter("fruitId");
        if (fruitIdStr == null || fruitIdStr.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Fruit ID parameter is missing for GET request.");
            // Redirect back to the list with an error message
            response.sendRedirect("listFruits?error=MissingFruitId");
            return;
        }

        try {
            int fruitId = Integer.parseInt(fruitIdStr.trim());
            LOGGER.log(Level.INFO, "Attempting to fetch fruit for editing, ID: {0}", fruitId);

            // --- Fetch Fruit Data from Database ---
            FruitBean fruitToEdit = fruitDb.getFruitById(fruitId);

            if (fruitToEdit != null) {
                LOGGER.log(Level.INFO, "Fruit found, ID: {0}. Forwarding to updateFruit.jsp", fruitId);
                // Set the fruit bean as a request attribute for the JSP
                request.setAttribute("fruitToEdit", fruitToEdit);
                // Forward to the JSP page for editing
                RequestDispatcher rd = request.getRequestDispatcher("/updateFruit.jsp"); // Ensure this JSP exists
                rd.forward(request, response);
            } else {
                // Fruit not found for the given ID
                LOGGER.log(Level.WARNING, "Fruit not found for editing, ID: {0}", fruitId);
                // Redirect back to the list page with an error message
                response.sendRedirect("listFruits?error=FruitNotFound");
            }
        } catch (NumberFormatException e) {
            // Invalid fruit ID format
            LOGGER.log(Level.WARNING, "Invalid Fruit ID format in GET request: {0}", fruitIdStr);
            response.sendRedirect("listFruits?error=InvalidFruitIdFormat");
        } catch (Exception e) {
            // Catch other potential exceptions during DB access or forwarding
            LOGGER.log(Level.SEVERE, "Error fetching fruit for editing, ID: " + fruitIdStr, e);
            request.setAttribute("errorMessage", "An error occurred while fetching fruit data.");
            // Forward to an error page or back to the list (showing error on list page might be simpler)
            response.sendRedirect("listFruits?error=FetchError");
        }
    }

    /**
     * Handles POST requests. Processes the submitted form data to update the fruit.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // --- Security Check: Ensure user is logged in ---
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userInfo") == null) {
            LOGGER.log(Level.WARNING, "Unauthorized access attempt to POST /updateFruit. Redirecting to login.");
            request.setAttribute("errorMessage", "Your session has expired. Please login again.");
            response.sendRedirect(request.getContextPath() + "/login.jsp"); // Redirect to login
            return;
        }
        // Optional: Add role-based access control here

        // --- Retrieve form data ---
        String fruitIdStr = request.getParameter("fruitId"); // Get the hidden ID
        String fruitName = request.getParameter("fruitName");
        String sourceCountry = request.getParameter("sourceCountry");

        LOGGER.log(Level.INFO, "Received update request for Fruit ID: {0}, Name: {1}, Country: {2}",
                   new Object[]{fruitIdStr, fruitName, sourceCountry});

        // --- Validate Fruit ID ---
        int fruitId = -1;
        try {
            if (fruitIdStr == null || fruitIdStr.trim().isEmpty()) {
                throw new ServletException("Fruit ID is missing in the update request.");
            }
            fruitId = Integer.parseInt(fruitIdStr.trim());
        } catch (NumberFormatException | ServletException e) {
            LOGGER.log(Level.SEVERE, "Invalid or missing Fruit ID in POST request: {0}", fruitIdStr);
            // Redirect back to list, as we don't know which fruit to show the form for
            response.sendRedirect("listFruits?error=InvalidUpdateFruitId");
            return;
        }

        // --- Basic Validation for other fields ---
        if (fruitName == null || fruitName.trim().isEmpty() ||
            sourceCountry == null || sourceCountry.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Update failed for fruit ID {0}: Name or country is empty.", fruitId);
            request.setAttribute("errorMessage", "Both Fruit Name and Source Country are required.");
            // Re-populate the form with submitted values and forward back
            FruitBean submittedData = new FruitBean(fruitId, fruitName, sourceCountry);
            request.setAttribute("fruitToEdit", submittedData); // Use submitted data to refill form
            RequestDispatcher rd = request.getRequestDispatcher("/updateFruit.jsp");
            rd.forward(request, response);
            return;
        }

        // --- Call the database update method ---
        boolean success = fruitDb.updateFruit(fruitId, fruitName, sourceCountry);

        if (success) {
            LOGGER.log(Level.INFO, "Fruit updated successfully, ID: {0}. Redirecting to list.", fruitId);
            // Redirect back to the fruit list page with a success message
            response.sendRedirect("listFruits?message=Fruit+updated+successfully");
        } else {
            LOGGER.log(Level.WARNING, "Failed to update fruit, ID: {0}. Forwarding back to edit form.", fruitId);
            // If update fails, forward back to the edit page with an error message
            request.setAttribute("errorMessage", "Failed to update fruit. The name might already exist, or a database error occurred.");
            // Re-populate the form with submitted values
            FruitBean submittedData = new FruitBean(fruitId, fruitName, sourceCountry);
            request.setAttribute("fruitToEdit", submittedData); // Use submitted data
            RequestDispatcher rd = request.getRequestDispatcher("/updateFruit.jsp");
            rd.forward(request, response);
        }
    }

    @Override
    public String getServletInfo() {
        return "Servlet for updating fruit information";
    }
}
