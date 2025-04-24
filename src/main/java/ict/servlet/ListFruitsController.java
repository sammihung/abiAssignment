package ict.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import ict.bean.FruitBean;
import ict.db.FruitDB;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "ListFruitsController", urlPatterns = { "/listFruits" })
public class ListFruitsController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ListFruitsController.class.getName());
    private FruitDB fruitDb;

    @Override
    public void init() throws ServletException {
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");
        String dbUrl = getServletContext().getInitParameter("dbUrl");

        if (dbUrl == null || dbUser == null) {
            LOGGER.log(Level.SEVERE, "Database connection parameters missing in web.xml for ListFruitsController.");
            throw new ServletException("Database connection parameters missing.");
        }

        fruitDb = new FruitDB(dbUrl, dbUser, dbPassword);
        LOGGER.log(Level.INFO, "ListFruitsController initialized.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userInfo") == null) {
            LOGGER.log(Level.WARNING, "Unauthorized access attempt to GET /listFruits.");
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=AuthenticationRequired");
            return;
        }

        try {
            ArrayList<FruitBean> fruits = fruitDb.getAllFruits();
            request.setAttribute("fruitList", fruits);
            LOGGER.log(Level.INFO, "Fetched {0} fruits. Forwarding to listFruits.jsp", fruits.size());
            RequestDispatcher rd = request.getRequestDispatcher("/listFruits.jsp");
            rd.forward(request, response);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching fruits list.", e);
            request.setAttribute("errorMessage", "An error occurred while retrieving the fruit list.");
            RequestDispatcher rd = request.getRequestDispatcher("/listFruits.jsp");
            rd.forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}
