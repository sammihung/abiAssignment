package ict.servlet;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import ict.db.FruitDB;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "FruitController", urlPatterns = { "/manageFruits" })
public class FruitController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(FruitController.class.getName());
    private FruitDB fruitDb;

    @Override
    public void init() throws ServletException {
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");
        String dbUrl = getServletContext().getInitParameter("dbUrl");

        if (dbUrl == null || dbUser == null) {
            LOGGER.log(Level.SEVERE, "Database connection parameters missing in web.xml for FruitController.");
            throw new ServletException("Database connection parameters missing.");
        }

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

        LOGGER.log(Level.INFO, "Forwarding to createFruit.jsp");
        RequestDispatcher rd = request.getRequestDispatcher("/createFruit.jsp");
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

        String fruitName = request.getParameter("fruitName");
        String sourceCountry = request.getParameter("sourceCountry");

        LOGGER.log(Level.INFO, "Received POST request to add fruit. Name='{0}', Country='{1}'",
                new Object[] { fruitName, sourceCountry });

        if (fruitName == null || fruitName.trim().isEmpty() ||
                sourceCountry == null || sourceCountry.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Fruit name or source country is empty.");
            request.setAttribute("errorMessage", "Both Fruit Name and Source Country are required.");
            request.setAttribute("fruitNameValue", fruitName);
            request.setAttribute("sourceCountryValue", sourceCountry);
            RequestDispatcher rd = request.getRequestDispatcher("/createFruit.jsp");
            rd.forward(request, response);
            return;
        }

        boolean success = fruitDb.addFruit(fruitName, sourceCountry);

        if (success) {
            LOGGER.log(Level.INFO, "Fruit '{0}' added successfully.", fruitName);
            response.sendRedirect(request.getContextPath() + "/manageFruits?message=Fruit+added+successfully!");
        } else {
            LOGGER.log(Level.WARNING, "Failed to add fruit '{0}'.", fruitName);
            request.setAttribute("errorMessage",
                    "Failed to add fruit. The name might already exist, or a database error occurred.");
            request.setAttribute("fruitNameValue", fruitName);
            request.setAttribute("sourceCountryValue", sourceCountry);
            RequestDispatcher rd = request.getRequestDispatcher("/createFruit.jsp");
            rd.forward(request, response);
        }
    }
}
