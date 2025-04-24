package ict.servlet;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import ict.bean.UserBean;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class AuthFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(AuthFilter.class.getName());
    private static final Set<String> PUBLIC_PATHS = new HashSet<>(Arrays.asList(
            "/", "/login", "/login.jsp", "/register", "/register.jsp", "/resources/", "/assignment/resources/"));
    private static final Map<String, Set<String>> PROTECTED_RESOURCES = new HashMap<>();
    public static final String ROLE_SENIOR_MANAGEMENT = "Senior Management";
    public static final String ROLE_WAREHOUSE_STAFF = "Warehouse Staff";
    public static final String ROLE_BAKERY_SHOP_STAFF = "Bakery shop staff";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LOGGER.info("AuthFilter initialized.");
        Set<String> smOnly = Set.of(ROLE_SENIOR_MANAGEMENT);
        PROTECTED_RESOURCES.put("/viewAdvancedReport", smOnly);
        PROTECTED_RESOURCES.put("/viewConsumptionReport", smOnly);
        PROTECTED_RESOURCES.put("/viewInventoryReport", smOnly);
        PROTECTED_RESOURCES.put("/listUsers", smOnly);
        PROTECTED_RESOURCES.put("/updateUser", smOnly);
        PROTECTED_RESOURCES.put("/deleteUsers", smOnly);
        PROTECTED_RESOURCES.put("/manageFruits", smOnly);
        PROTECTED_RESOURCES.put("/updateFruit", smOnly);
        PROTECTED_RESOURCES.put("/deleteFruit", smOnly);
        PROTECTED_RESOURCES.put("/listAllInventory", smOnly);
        PROTECTED_RESOURCES.put("/adminCreateUser", smOnly);
        PROTECTED_RESOURCES.put("/viewForecastReport", smOnly);
        Set<String> wsOrSm = Set.of(ROLE_WAREHOUSE_STAFF, ROLE_SENIOR_MANAGEMENT);
        PROTECTED_RESOURCES.put("/updateWarehouseInventory", wsOrSm);
        PROTECTED_RESOURCES.put("/needsApproval", wsOrSm);
        PROTECTED_RESOURCES.put("/arrangeDelivery", wsOrSm);
        PROTECTED_RESOURCES.put("/checkoutToShop", wsOrSm);
        Set<String> bssOrSm = Set.of(ROLE_BAKERY_SHOP_STAFF, ROLE_SENIOR_MANAGEMENT);
        PROTECTED_RESOURCES.put("/updateInventory", bssOrSm);
        PROTECTED_RESOURCES.put("/orderFromSource", bssOrSm);
        PROTECTED_RESOURCES.put("/listReservations", bssOrSm);
        PROTECTED_RESOURCES.put("/batchBorrowFruit", bssOrSm);
        PROTECTED_RESOURCES.put("/approveBorrow", bssOrSm);
        Set<String> allStaffOrSm = Set.of(ROLE_BAKERY_SHOP_STAFF, ROLE_WAREHOUSE_STAFF, ROLE_SENIOR_MANAGEMENT);
        PROTECTED_RESOURCES.put("/listFruits", allStaffOrSm);
        PROTECTED_RESOURCES.put("/listBorrowings", allStaffOrSm);
        PROTECTED_RESOURCES.put("/listDeliveries", allStaffOrSm);
        PROTECTED_RESOURCES.put("/welcome.jsp", allStaffOrSm);
        PROTECTED_RESOURCES.put("/viewStaffStock", allStaffOrSm);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);
        String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        String servletPath = httpRequest.getServletPath();
        boolean isPublic = false;
        if (PUBLIC_PATHS.contains(servletPath)) {
            isPublic = true;
        } else {
            for (String publicPrefix : PUBLIC_PATHS) {
                if (publicPrefix.endsWith("/") && path.startsWith(publicPrefix)) {
                    isPublic = true;
                    break;
                }
            }
        }
        if (isPublic) {
            chain.doFilter(request, response);
            return;
        }
        UserBean currentUser = null;
        if (session != null) {
            currentUser = (UserBean) session.getAttribute("userInfo");
        }
        if (currentUser == null) {
            LOGGER.log(Level.WARNING, "Authentication required for path: {0}. Redirecting to login.", path);
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login.jsp?error=AuthenticationRequired");
            return;
        }
        String userRole = currentUser.getRole();
        if (userRole == null || userRole.trim().isEmpty()) {
            LOGGER.log(Level.SEVERE, "User {0} has missing role in session. Denying access to {1}",
                    new Object[] { currentUser.getUsername(), path });
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login.jsp?error=UserRoleMissing");
            return;
        }
        boolean authorized = false;
        Set<String> allowedRoles = PROTECTED_RESOURCES.get(servletPath);
        if (allowedRoles != null) {
            if (allowedRoles.contains(userRole)) {
                authorized = true;
            }
        } else {
            if (ROLE_SENIOR_MANAGEMENT.equals(userRole) || ROLE_WAREHOUSE_STAFF.equals(userRole)
                    || ROLE_BAKERY_SHOP_STAFF.equals(userRole)) {
                authorized = true;
            }
        }
        if (authorized) {
            chain.doFilter(request, response);
        } else {
            LOGGER.log(Level.WARNING, "Authorization denied for User {0} (Role: {1}) accessing path: {2}",
                    new Object[] { currentUser.getUsername(), userRole, path });
            request.setAttribute("errorMessage", "You do not have permission to access this resource.");
            RequestDispatcher rd = request.getRequestDispatcher("/accessDenied.jsp");
            if (rd != null) {
                rd.forward(request, response);
            } else {
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
            }
        }
    }

    @Override
    public void destroy() {
        LOGGER.info("AuthFilter destroyed.");
    }
}