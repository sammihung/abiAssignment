package ict.servlet; // Or ict.filter

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

// Apply the filter to all URLs except specific public ones (defined below)
// Note: URL patterns here are examples, adjust based on your actual protected resources
// We will configure more specific mappings in web.xml for better control
// @WebFilter("/*") // Applying broadly initially, refine in web.xml
public class AuthFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(AuthFilter.class.getName());

    // Define public paths that don't require login
    private static final Set<String> PUBLIC_PATHS = new HashSet<>(Arrays.asList(
            "/", // Context root (might redirect to login or welcome page)
            "/login", // Login servlet/page itself
            "/login.jsp", // Login JSP
            "/register", // Registration servlet/page
            "/register.jsp", // Registration JSP
            "/resources/", // Static resources like CSS/JS (adjust path if different)
            "/assignment/resources/" // Jakarta REST resources (if any are public)
    ));

    // Define role-based access control mapping (URL pattern -> Set of allowed
    // roles)
    // Use ANT-style matching if needed, but basic prefix/exact matching is simpler
    // here.
    // Best configured in web.xml for standard security constraints, but doing it
    // here for demonstration.
    private static final Map<String, Set<String>> PROTECTED_RESOURCES = new HashMap<>();

    // Constants for roles (avoids magic strings)
    public static final String ROLE_SENIOR_MANAGEMENT = "Senior Management";
    public static final String ROLE_WAREHOUSE_STAFF = "Warehouse Staff";
    public static final String ROLE_BAKERY_SHOP_STAFF = "Bakery shop staff";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LOGGER.info("AuthFilter initialized.");

        // --- Define Resource Access Rules ---
        // Example: More specific rules can be defined here or ideally in web.xml

        // Senior Management Only
        Set<String> smOnly = Set.of(ROLE_SENIOR_MANAGEMENT);
        PROTECTED_RESOURCES.put("/viewAdvancedReport", smOnly);
        PROTECTED_RESOURCES.put("/viewConsumptionReport", smOnly);
        PROTECTED_RESOURCES.put("/viewInventoryReport", smOnly);
        PROTECTED_RESOURCES.put("/listUsers", smOnly); // Assuming only SM lists/manages users
        PROTECTED_RESOURCES.put("/updateUser", smOnly); // Assuming only SM updates users
        PROTECTED_RESOURCES.put("/deleteUsers", smOnly); // Assuming only SM deletes users
        PROTECTED_RESOURCES.put("/manageFruits", smOnly); // Example: If only SM manages fruit definitions
        PROTECTED_RESOURCES.put("/updateFruit", smOnly); // Example: If only SM manages fruit definitions
        PROTECTED_RESOURCES.put("/deleteFruit", smOnly); // Example: If only SM manages fruit definitions

        // Warehouse Staff Only (or SM)
        Set<String> wsOrSm = Set.of(ROLE_WAREHOUSE_STAFF, ROLE_SENIOR_MANAGEMENT);
        PROTECTED_RESOURCES.put("/updateWarehouseInventory", wsOrSm);
        PROTECTED_RESOURCES.put("/needsApproval", wsOrSm);
        PROTECTED_RESOURCES.put("/arrangeDelivery", wsOrSm);
        PROTECTED_RESOURCES.put("/checkoutToShop", wsOrSm);

        // Bakery Shop Staff Only (or SM)
        Set<String> bssOrSm = Set.of(ROLE_BAKERY_SHOP_STAFF, ROLE_SENIOR_MANAGEMENT);
        PROTECTED_RESOURCES.put("/updateInventory", bssOrSm);
        PROTECTED_RESOURCES.put("/reserveFruit", bssOrSm);
        PROTECTED_RESOURCES.put("/listReservations", bssOrSm); // List own reservations
        PROTECTED_RESOURCES.put("/borrowFruit", bssOrSm);
        PROTECTED_RESOURCES.put("/approveBorrow", bssOrSm); // Approve/reject borrows for *own* shop

        // Accessible by Multiple Roles (e.g., Viewing Lists) - SM can see all
        Set<String> allStaffOrSm = Set.of(ROLE_BAKERY_SHOP_STAFF, ROLE_WAREHOUSE_STAFF, ROLE_SENIOR_MANAGEMENT);
        PROTECTED_RESOURCES.put("/listFruits", allStaffOrSm); // Example: All staff can view fruits
        PROTECTED_RESOURCES.put("/listBorrowings", allStaffOrSm); // Specific filtering done in Controller
        PROTECTED_RESOURCES.put("/listDeliveries", allStaffOrSm); // Specific filtering done in Controller
        PROTECTED_RESOURCES.put("/welcome.jsp", allStaffOrSm); // Welcome page after login
        // Add other common pages like /page if needed

        // You can add more mappings for other servlets
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false); // Don't create session if none exists

        String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        String servletPath = httpRequest.getServletPath(); // Often more reliable for matching

        // --- 1. Check if the path is public ---
        boolean isPublic = false;
        if (PUBLIC_PATHS.contains(servletPath)) {
            isPublic = true;
        } else {
            // Check resource paths like /resources/css/style.css
            for (String publicPrefix : PUBLIC_PATHS) {
                if (publicPrefix.endsWith("/") && path.startsWith(publicPrefix)) {
                    isPublic = true;
                    break;
                }
            }
        }

        if (isPublic) {
            // LOGGER.log(Level.FINER, "Public path accessed: {0}", path);
            chain.doFilter(request, response); // Public resource, let it go through
            return;
        }

        // --- 2. Check if user is logged in (Session and UserBean exist) ---
        UserBean currentUser = null;
        if (session != null) {
            currentUser = (UserBean) session.getAttribute("userInfo");
        }

        if (currentUser == null) {
            LOGGER.log(Level.WARNING, "Authentication required for path: {0}. Redirecting to login.", path);
            // Save the requested URL to redirect back after login (optional)
            // session = httpRequest.getSession(); // Create session to store redirect URL
            // session.setAttribute("redirectAfterLogin", path);
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login.jsp?error=AuthenticationRequired");
            return;
        }

        // --- 3. Check Role-Based Authorization ---
        String userRole = currentUser.getRole();
        if (userRole == null || userRole.trim().isEmpty()) {
            LOGGER.log(Level.SEVERE, "User {0} has missing role in session. Denying access to {1}",
                    new Object[] { currentUser.getUsername(), path });
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login.jsp?error=UserRoleMissing");
            return;
        }

        boolean authorized = false;
        // Find the matching rule for the servlet path
        Set<String> allowedRoles = PROTECTED_RESOURCES.get(servletPath);

        if (allowedRoles != null) {
            if (allowedRoles.contains(userRole)) {
                authorized = true;
            }
        } else {
            // If no specific rule is defined for this path, default to deny or allow?
            // For safety, let's default to deny unless explicitly allowed.
            // However, some paths might be generally accessible after login (e.g., welcome
            // page)
            // Check if the role is one of the known roles (basic check)
            if (ROLE_SENIOR_MANAGEMENT.equals(userRole) || ROLE_WAREHOUSE_STAFF.equals(userRole)
                    || ROLE_BAKERY_SHOP_STAFF.equals(userRole)) {
                // Assume general logged-in access is allowed if no specific rule denies it
                // Fine-tune this logic based on exact needs.
                // Example: Check for a "general access" role set if needed.
                LOGGER.log(Level.FINER, "No specific rule for {0}, allowing general logged-in access for role {1}",
                        new Object[] { servletPath, userRole });
                authorized = true; // Allow if logged in and no specific rule applies
            }
        }

        if (authorized) {
            // User is logged in and has the basic role permission for this URL pattern
            // LOGGER.log(Level.FINER, "User {0} (Role: {1}) authorized for path: {2}", new
            // Object[]{currentUser.getUsername(), userRole, path});
            chain.doFilter(request, response); // Proceed to the servlet
        } else {
            // User is logged in but does not have the required role
            LOGGER.log(Level.WARNING, "Authorization denied for User {0} (Role: {1}) accessing path: {2}",
                    new Object[] { currentUser.getUsername(), userRole, path });
            // Redirect to an access denied page or back to a safe page
            request.setAttribute("errorMessage", "You do not have permission to access this resource.");
            // Forwarding allows keeping the original request attributes if needed by the
            // error page
            RequestDispatcher rd = request.getRequestDispatcher("/accessDenied.jsp"); // Create this JSP
            if (rd != null) {
                rd.forward(request, response);
            } else {
                // Fallback if forward fails or JSP doesn't exist
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
            }
        }
    }

    @Override
    public void destroy() {
        LOGGER.info("AuthFilter destroyed.");
    }
}