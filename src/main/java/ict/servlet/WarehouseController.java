package ict.servlet;

import java.io.IOException;

import ict.db.WarehouseDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

//@WebServlet(urlPatterns = { "/login", "/main" })
public class WarehouseController extends HttpServlet {

    private WarehouseDB db;

    public void init() {
        String dbUser = this.getServletContext().getInitParameter("dbUser");
        String dbPassword = this.getServletContext().getInitParameter("dbPassword");
        String dbUrl = this.getServletContext().getInitParameter("dbUrl");
        db = new WarehouseDB(dbUrl, dbUser, dbPassword);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

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