package ict.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import ict.bean.UserBean;
import ict.db.UserDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(urlPatterns = { "/login", "/main" })
public class LoginController extends HttpServlet {

    private UserDB db;

    public void init() {
        String dbUser = this.getServletContext().getInitParameter("dbUser");
        String dbPassword = this.getServletContext().getInitParameter("dbPassword");
        String dbUrl = this.getServletContext().getInitParameter("dbUrl");
        db = new UserDB(dbUrl, dbUser, dbPassword);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        if ("logout".equals(action)) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            response.sendRedirect(request.getContextPath() + "/");
        } else {
            String username = request.getParameter("username");
            String password = request.getParameter("password");
            String targetURL;

            try {
                if (db.isValidUser(username, password)) {
                    HttpSession session = request.getSession();
                    UserBean bean = new UserBean();
                    bean.setUsername(username);
                    session.setAttribute("userInfo", bean);
                    targetURL = "/welcome.jsp";
                } else {
                    request.setAttribute("errorMessage", "Invalid username or password.");
                    targetURL = "/";
                }
            } catch (SQLException e) {
                Logger.getLogger(LoginController.class.getName()).log(Level.SEVERE,
                        "Database error during authentication", e);
                request.setAttribute("errorMessage", "Database error. Please try again later.");
                targetURL = "/";
            }

            request.getRequestDispatcher(targetURL).forward(request, response);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userInfo") == null) {
            request.getRequestDispatcher("/login.jsp").forward(request, response);
        } else {
            request.getRequestDispatcher("/page").forward(request, response);
        }
    }
}