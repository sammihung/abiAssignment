package ict.servlet;

import java.io.IOException;
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

@WebServlet(name = "UpdateFruitController", urlPatterns = { "/updateFruit" })
public class UpdateFruitController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(UpdateFruitController.class.getName());
    private FruitDB fruitDb;

    @Override
    public void init() throws ServletException {
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");
        String dbUrl = getServletContext().getInitParameter("dbUrl");

        if (dbUrl == null || dbUser == null) {
            throw new ServletException("Database connection parameters missing.");
        }
        fruitDb = new FruitDB(dbUrl, dbUser, dbPassword);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userInfo") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        String fruitIdStr = request.getParameter("fruitId");
        if (fruitIdStr == null || fruitIdStr.trim().isEmpty()) {
            response.sendRedirect("listFruits?error=MissingFruitId");
            return;
        }

        try {
            int fruitId = Integer.parseInt(fruitIdStr.trim());
            FruitBean fruitToEdit = fruitDb.getFruitById(fruitId);

            if (fruitToEdit != null) {
                request.setAttribute("fruitToEdit", fruitToEdit);
                RequestDispatcher rd = request.getRequestDispatcher("/updateFruit.jsp");
                rd.forward(request, response);
            } else {
                response.sendRedirect("listFruits?error=FruitNotFound");
            }
        } catch (NumberFormatException e) {
            response.sendRedirect("listFruits?error=InvalidFruitIdFormat");
        } catch (Exception e) {
            response.sendRedirect("listFruits?error=FetchError");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userInfo") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        String fruitIdStr = request.getParameter("fruitId");
        String fruitName = request.getParameter("fruitName");
        String sourceCountry = request.getParameter("sourceCountry");

        int fruitId = -1;
        try {
            if (fruitIdStr == null || fruitIdStr.trim().isEmpty()) {
                throw new ServletException("Fruit ID is missing in the update request.");
            }
            fruitId = Integer.parseInt(fruitIdStr.trim());
        } catch (NumberFormatException | ServletException e) {
            response.sendRedirect("listFruits?error=InvalidUpdateFruitId");
            return;
        }

        if (fruitName == null || fruitName.trim().isEmpty() ||
                sourceCountry == null || sourceCountry.trim().isEmpty()) {
            FruitBean submittedData = new FruitBean(fruitId, fruitName, sourceCountry);
            request.setAttribute("fruitToEdit", submittedData);
            RequestDispatcher rd = request.getRequestDispatcher("/updateFruit.jsp");
            rd.forward(request, response);
            return;
        }

        boolean success = fruitDb.updateFruit(fruitId, fruitName, sourceCountry);

        if (success) {
            response.sendRedirect("listFruits?message=Fruit+updated+successfully");
        } else {
            request.setAttribute("errorMessage",
                    "Failed to update fruit. The name might already exist, or a database error occurred.");
            FruitBean submittedData = new FruitBean(fruitId, fruitName, sourceCountry);
            request.setAttribute("fruitToEdit", submittedData);
            RequestDispatcher rd = request.getRequestDispatcher("/updateFruit.jsp");
            rd.forward(request, response);
        }
    }

    @Override
    public String getServletInfo() {
        return "Servlet for updating fruit information";
    }
}
