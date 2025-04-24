package ict.db;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager; // Make sure this bean exists if getAggregatedNeedsByCountry is used here
import java.sql.PreparedStatement; // Using BakeryShopBean
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List; // Need this for WarehouseDB dependency
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import ict.bean.BakeryShopBean;
import ict.bean.BorrowableFruitInfoBean;
import ict.bean.BorrowingBean;
import ict.bean.ConsumptionDataBean;
import ict.bean.ForecastBean;
import ict.bean.FruitBean;
import ict.bean.InventoryBean;
import ict.bean.InventorySummaryBean;
import ict.bean.OrderableFruitBean;
import ict.bean.ReservationBean;
import ict.bean.WarehouseBean;

/**
 * Handles database operations related to borrowing fruits between shops,
 * inventory updates, and potentially reservation/delivery logic consolidation.
 */
public class BorrowingDB { // Renamed from ReservationDB if this is the primary class now

    private static final Logger LOGGER = Logger.getLogger(BorrowingDB.class.getName());
    private String dburl, username, password;
    private BakeryShopDB bakeryShopDb; // For shop details
    private WarehouseDB warehouseDb; // ADDED: For warehouse details
    private FruitDB fruitDb; // ADDED: For fruit details (needed by some methods)

    // Constructor
    public BorrowingDB(String dburl, String dbUser, String dbPassword) {
        this.dburl = dburl;
        this.username = dbUser;
        this.password = dbPassword;
        // Initialize dependencies
        this.bakeryShopDb = new BakeryShopDB(dburl, dbUser, dbPassword);
        this.warehouseDb = new WarehouseDB(dburl, dbUser, dbPassword); // ADDED: Initialize WarehouseDB
        this.fruitDb = new FruitDB(dburl, dbUser, dbPassword); // ADDED: Initialize FruitDB
    }

    // --- Connection Method ---
    public Connection getConnection() throws SQLException, IOException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "MySQL JDBC Driver not found.", e);
            throw new IOException("Database driver not found.", e);
        }
        return DriverManager.getConnection(dburl, username, password);
    }

    // --- Resource Closing Helper ---
    private void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to close resource: " + resource.getClass().getSimpleName(), e);
            }
        }
    }

    // ========================================================================
    // Borrowing Related Methods (from previous steps)
    // ========================================================================

    private boolean addBorrowingRecord(int fruitId, int lendingShopId, int borrowingShopId, int quantity, String status,
            Connection conn) throws SQLException {
        String sql = "INSERT INTO borrowings (fruit_id, borrowing_shop_id, receiving_shop_id, quantity, borrowing_date, status) VALUES (?, ?, ?, ?, CURDATE(), ?)";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, fruitId);
            ps.setInt(2, lendingShopId); // Lender
            ps.setInt(3, borrowingShopId); // Receiver (Borrower)
            ps.setInt(4, quantity);
            ps.setString(5, status); // Use passed status ('Pending' initially)
            return ps.executeUpdate() >= 1;
        } finally {
            closeQuietly(ps);
        }
    }

    // --- Borrowing Transaction Helper Methods ---
    public List<BorrowingBean> getPendingBorrowRequests(int lendingShopId) {
        List<BorrowingBean> requests = new ArrayList<>();
        String sql = "SELECT b.*, f.fruit_name, rs.shop_name as receiving_shop_name " +
                "FROM borrowings b " +
                "JOIN fruits f ON b.fruit_id = f.fruit_id " +
                "JOIN shops rs ON b.receiving_shop_id = rs.shop_id " + // Requesting/Receiving shop
                "WHERE b.borrowing_shop_id = ? AND b.status = 'Pending' " + // Filter by LENDER and status
                "ORDER BY b.borrowing_date ASC, b.borrowing_id ASC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, lendingShopId);
            rs = ps.executeQuery();
            while (rs.next()) {
                BorrowingBean bean = new BorrowingBean();
                bean.setBorrowingId(rs.getInt("borrowing_id"));
                bean.setFruitId(rs.getInt("fruit_id"));
                bean.setBorrowingShopId(rs.getInt("borrowing_shop_id")); // This shop (lender)
                bean.setReceivingShopId(rs.getInt("receiving_shop_id")); // The shop asking
                bean.setQuantity(rs.getInt("quantity"));
                bean.setBorrowingDate(rs.getDate("borrowing_date"));
                bean.setStatus(rs.getString("status"));
                bean.setFruitName(rs.getString("fruit_name"));
                // Don't need borrowingShopName (it's this shop)
                bean.setReceivingShopName(rs.getString("receiving_shop_name"));
                requests.add(bean);
            }
            LOGGER.log(Level.INFO, "Fetched {0} pending borrow requests for lending ShopID={1}",
                    new Object[] { requests.size(), lendingShopId });
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching pending borrow requests for shop " + lendingShopId, e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return requests;
    }

    public String approveBorrowRequest(int borrowingId, int lendingShopId) {
        Connection conn = null;
        String statusMessage = "Approval failed: Unknown error.";
        BorrowingBean requestDetails = null;

        try {
            // Fetch details first to check status and get IDs/quantity
            requestDetails = getBorrowingById(borrowingId); // Need this helper method

            if (requestDetails == null) {
                return "Approval failed: Borrow request ID " + borrowingId + " not found.";
            }
            if (requestDetails.getBorrowingShopId() != lendingShopId) {
                return "Approval failed: You are not authorized to approve this request.";
            }
            if (!"Pending".equalsIgnoreCase(requestDetails.getStatus())) {
                return "Approval failed: Request is not in 'Pending' status (Current: " + requestDetails.getStatus()
                        + ").";
            }

            int fruitId = requestDetails.getFruitId();
            int receivingShopId = requestDetails.getReceivingShopId();
            int quantity = requestDetails.getQuantity();

            if (quantity <= 0) {
                return "Approval failed: Invalid quantity in request.";
            }

            // Start Transaction
            conn = getConnection();
            conn.setAutoCommit(false);

            // 1. Check inventory of the lending shop AGAIN
            int currentLenderStock = getInventoryQuantityForShop(fruitId, lendingShopId, conn);
            if (currentLenderStock < quantity) {
                conn.rollback();
                // Optionally update status to 'Rejected - No Stock'?
                // updateBorrowingStatus(borrowingId, "Rejected - No Stock", conn);
                // conn.commit(); // Separate transaction?
                return "Approval failed: Insufficient stock (" + currentLenderStock + ") to fulfill request.";
            }
            LOGGER.log(Level.INFO,
                    "[TX-ApproveBorrow] Lender inventory check passed for ShopID={0}, FruitID={1}. Have: {2}, Need: {3}",
                    new Object[] { lendingShopId, fruitId, currentLenderStock, quantity });

            // 2. Update borrowing status to 'Approved'
            boolean statusUpdated = updateBorrowingStatus(borrowingId, "Approved", conn); // Need this helper
            if (!statusUpdated) {
                conn.rollback();
                return "Approval failed: Could not update borrowing status.";
            }
            LOGGER.log(Level.INFO, "[TX-ApproveBorrow] Borrowing status updated to Approved.");

            // 3. Decrease inventory for the lending shop
            boolean lenderInvUpdated = updateShopInventory(fruitId, lendingShopId, -quantity, conn);
            if (!lenderInvUpdated) {
                conn.rollback();
                return "Approval failed: Could not decrease lending shop inventory.";
            }
            LOGGER.log(Level.INFO, "[TX-ApproveBorrow] Lender inventory decreased.");

            // 4. Increase inventory for the receiving (borrowing) shop
            boolean borrowerInvUpdated = addOrUpdateShopInventory(fruitId, receivingShopId, quantity, conn);
            if (!borrowerInvUpdated) {
                conn.rollback();
                // Consider compensation logic if needed
                return "Approval failed: Could not increase receiving shop inventory.";
            }
            LOGGER.log(Level.INFO, "[TX-ApproveBorrow] Borrower inventory increased.");

            // All steps successful
            conn.commit();
            statusMessage = "Borrow request ID " + borrowingId + " approved successfully!";
            LOGGER.log(Level.INFO, "[TX-ApproveBorrow] Transaction committed successfully.");

        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error during borrowing approval transaction for ID " + borrowingId, e);
            statusMessage = "Approval failed: Database error occurred (" + e.getMessage() + ")";
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Rollback failed", ex);
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Failed to close connection", e);
                }
            }
        }
        return statusMessage;
    }

    public String rejectBorrowRequest(int borrowingId, int lendingShopId) {
        Connection conn = null;
        String statusMessage = "Rejection failed: Unknown error.";
        BorrowingBean requestDetails = null;

        try {
            // Fetch details first to check status and ownership
            requestDetails = getBorrowingById(borrowingId); // Need this helper method

            if (requestDetails == null) {
                return "Rejection failed: Borrow request ID " + borrowingId + " not found.";
            }
            if (requestDetails.getBorrowingShopId() != lendingShopId) {
                return "Rejection failed: You are not authorized to reject this request.";
            }
            if (!"Pending".equalsIgnoreCase(requestDetails.getStatus())) {
                return "Rejection failed: Request is not in 'Pending' status (Current: " + requestDetails.getStatus()
                        + ").";
            }

            // Start Transaction (optional for simple status update, but good practice)
            conn = getConnection();
            conn.setAutoCommit(false);

            boolean statusUpdated = updateBorrowingStatus(borrowingId, "Rejected", conn); // Need this helper

            if (statusUpdated) {
                conn.commit();
                statusMessage = "Borrow request ID " + borrowingId + " rejected.";
                LOGGER.log(Level.INFO, "[TX-RejectBorrow] Borrowing status updated to Rejected. Tx committed.");
            } else {
                conn.rollback();
                statusMessage = "Rejection failed: Could not update borrowing status.";
                LOGGER.log(Level.WARNING, "[TX-RejectBorrow] Failed to update status. Tx rolled back.");
            }

        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error during borrowing rejection for ID " + borrowingId, e);
            statusMessage = "Rejection failed: Database error occurred (" + e.getMessage() + ")";
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Rollback failed", ex);
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Failed to close connection", e);
                }
            }
        }
        return statusMessage;
    }

    public BorrowingBean getBorrowingById(int borrowingId) {
        BorrowingBean bean = null;
        String sql = "SELECT * FROM borrowings WHERE borrowing_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, borrowingId);
            rs = ps.executeQuery();
            if (rs.next()) {
                bean = new BorrowingBean();
                bean.setBorrowingId(rs.getInt("borrowing_id"));
                bean.setFruitId(rs.getInt("fruit_id"));
                bean.setBorrowingShopId(rs.getInt("borrowing_shop_id"));
                bean.setReceivingShopId(rs.getInt("receiving_shop_id"));
                bean.setQuantity(rs.getInt("quantity"));
                bean.setBorrowingDate(rs.getDate("borrowing_date"));
                bean.setStatus(rs.getString("status"));
            }
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching borrowing by ID " + borrowingId, e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return bean;
    }

    private boolean updateBorrowingStatus(int borrowingId, String newStatus, Connection conn) throws SQLException {
        String sql = "UPDATE borrowings SET status = ? WHERE borrowing_id = ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setString(1, newStatus);
            ps.setInt(2, borrowingId);
            return ps.executeUpdate() == 1;
        } finally {
            closeQuietly(ps);
        }
    }

    private int getInventoryQuantityForShop(int fruitId, int shopId, Connection conn) throws SQLException {
        int quantity = 0;
        String sql = "SELECT quantity FROM inventory WHERE fruit_id = ? AND shop_id = ?";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, fruitId);
            ps.setInt(2, shopId);
            rs = ps.executeQuery();
            if (rs.next())
                quantity = rs.getInt("quantity");
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
        return quantity;
    }

    private boolean updateShopInventory(int fruitId, int shopId, int quantityChange, Connection conn)
            throws SQLException {
        String sql = "UPDATE inventory SET quantity = quantity + ? WHERE fruit_id = ? AND shop_id = ?";
        if (quantityChange < 0)
            sql += " AND quantity >= ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, quantityChange);
            ps.setInt(2, fruitId);
            ps.setInt(3, shopId);
            if (quantityChange < 0)
                ps.setInt(4, -quantityChange);
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0 && quantityChange < 0)
                return false;
            return rowsAffected >= 1;
        } finally {
            closeQuietly(ps);
        }
    }

    private boolean addOrUpdateShopInventory(int fruitId, int shopId, int quantityChange, Connection conn)
            throws SQLException {
        boolean updated = updateShopInventory(fruitId, shopId, quantityChange, conn);
        if (updated)
            return true;
        else if (quantityChange > 0) {
            String insertSql = "INSERT INTO inventory (fruit_id, shop_id, quantity, warehouse_id) VALUES (?, ?, ?, NULL)"; // Assuming
                                                                                                                           // warehouse_id
                                                                                                                           // is
                                                                                                                           // NULL
                                                                                                                           // for
                                                                                                                           // shop
            PreparedStatement psInsert = null;
            try {
                psInsert = conn.prepareStatement(insertSql);
                psInsert.setInt(1, fruitId);
                psInsert.setInt(2, shopId);
                psInsert.setInt(3, quantityChange);
                return psInsert.executeUpdate() >= 1;
            } finally {
                closeQuietly(psInsert);
            }
        } else
            return false;
    }

    // ========================================================================
    // Shop Inventory Update Methods (from previous steps)
    // ========================================================================

    public List<InventoryBean> getInventoryForShop(int shopId) {
        List<InventoryBean> inventoryList = new ArrayList<>();
        String sql = "SELECT i.inventory_id, i.fruit_id, i.shop_id, i.warehouse_id, i.quantity, f.fruit_name " +
                "FROM inventory i JOIN fruits f ON i.fruit_id = f.fruit_id " +
                "WHERE i.shop_id = ? ORDER BY f.fruit_name";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, shopId);
            rs = ps.executeQuery();
            while (rs.next()) {
                InventoryBean item = new InventoryBean();
                item.setInventoryId(rs.getInt("inventory_id"));
                item.setFruitId(rs.getInt("fruit_id"));
                item.setShopId(rs.getInt("shop_id"));
                item.setWarehouseId(rs.getObject("warehouse_id") != null ? rs.getInt("warehouse_id") : null);
                item.setQuantity(rs.getInt("quantity"));
                item.setFruitName(rs.getString("fruit_name"));
                inventoryList.add(item);
            }
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching inventory for shop " + shopId, e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return inventoryList;
    }

    public boolean setShopInventoryQuantity(int fruitId, int shopId, int newQuantity) {
        if (newQuantity < 0)
            return false;
        Connection conn = null;
        PreparedStatement psCheck = null;
        PreparedStatement psUpdate = null;
        PreparedStatement psInsert = null;
        ResultSet rsCheck = null;
        boolean success = false;
        String checkSql = "SELECT inventory_id FROM inventory WHERE fruit_id = ? AND shop_id = ?";
        String updateSql = "UPDATE inventory SET quantity = ? WHERE inventory_id = ?";
        String insertSql = "INSERT INTO inventory (fruit_id, shop_id, quantity, warehouse_id) VALUES (?, ?, ?, NULL)";
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            psCheck = conn.prepareStatement(checkSql);
            psCheck.setInt(1, fruitId);
            psCheck.setInt(2, shopId);
            rsCheck = psCheck.executeQuery();
            int inventoryId = rsCheck.next() ? rsCheck.getInt("inventory_id") : -1;
            closeQuietly(rsCheck);
            closeQuietly(psCheck);

            if (inventoryId != -1) { // Update
                psUpdate = conn.prepareStatement(updateSql);
                psUpdate.setInt(1, newQuantity);
                psUpdate.setInt(2, inventoryId);
                success = psUpdate.executeUpdate() > 0;
                closeQuietly(psUpdate);
            } else { // Insert
                psInsert = conn.prepareStatement(insertSql);
                psInsert.setInt(1, fruitId);
                psInsert.setInt(2, shopId);
                psInsert.setInt(3, newQuantity);
                success = psInsert.executeUpdate() > 0;
                closeQuietly(psInsert);
            }
            if (success)
                conn.commit();
            else
                conn.rollback();
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error setting shop inventory", e);
            if (conn != null)
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                }
            success = false;
        } finally {
            if (conn != null)
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                }
        }
        return success;
    }

    // ========================================================================
    // Warehouse Inventory Update Methods (from previous steps)
    // ========================================================================

    public List<InventoryBean> getInventoryForWarehouse(int warehouseId) {
        List<InventoryBean> inventoryList = new ArrayList<>();
        String sql = "SELECT i.inventory_id, i.fruit_id, i.shop_id, i.warehouse_id, i.quantity, f.fruit_name " +
                "FROM inventory i JOIN fruits f ON i.fruit_id = f.fruit_id " +
                "WHERE i.warehouse_id = ? AND i.shop_id IS NULL ORDER BY f.fruit_name";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, warehouseId);
            rs = ps.executeQuery();
            while (rs.next()) {
                InventoryBean item = new InventoryBean();
                item.setInventoryId(rs.getInt("inventory_id"));
                item.setFruitId(rs.getInt("fruit_id"));
                item.setShopId(rs.getObject("shop_id") != null ? rs.getInt("shop_id") : null);
                item.setWarehouseId(rs.getInt("warehouse_id"));
                item.setQuantity(rs.getInt("quantity"));
                item.setFruitName(rs.getString("fruit_name"));
                inventoryList.add(item);
            }
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching warehouse inventory " + warehouseId, e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return inventoryList;
    }

    public boolean setWarehouseInventoryQuantity(int fruitId, int warehouseId, int newQuantity) {
        if (newQuantity < 0)
            return false;
        Connection conn = null;
        PreparedStatement psCheck = null;
        PreparedStatement psUpdate = null;
        PreparedStatement psInsert = null;
        ResultSet rsCheck = null;
        boolean success = false;
        String checkSql = "SELECT inventory_id FROM inventory WHERE fruit_id = ? AND warehouse_id = ? AND shop_id IS NULL";
        String updateSql = "UPDATE inventory SET quantity = ? WHERE inventory_id = ?";
        String insertSql = "INSERT INTO inventory (fruit_id, warehouse_id, quantity, shop_id) VALUES (?, ?, ?, NULL)";
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            psCheck = conn.prepareStatement(checkSql);
            psCheck.setInt(1, fruitId);
            psCheck.setInt(2, warehouseId);
            rsCheck = psCheck.executeQuery();
            int inventoryId = rsCheck.next() ? rsCheck.getInt("inventory_id") : -1;
            closeQuietly(rsCheck);
            closeQuietly(psCheck);

            if (inventoryId != -1) { // Update
                psUpdate = conn.prepareStatement(updateSql);
                psUpdate.setInt(1, newQuantity);
                psUpdate.setInt(2, inventoryId);
                success = psUpdate.executeUpdate() > 0;
                closeQuietly(psUpdate);
            } else { // Insert
                psInsert = conn.prepareStatement(insertSql);
                psInsert.setInt(1, fruitId);
                psInsert.setInt(2, warehouseId);
                psInsert.setInt(3, newQuantity);
                success = psInsert.executeUpdate() > 0;
                closeQuietly(psInsert);
            }
            if (success)
                conn.commit();
            else
                conn.rollback();
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error setting warehouse inventory", e);
            if (conn != null)
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                }
            success = false;
        } finally {
            if (conn != null)
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                }
        }
        return success;
    }

    // ========================================================================
    // Reservation / Needs / Delivery Methods (from previous steps)
    // ========================================================================
    // Inner static class for Delivery Needs
    public static class DeliveryNeedBean implements Serializable {
        private int fruitId;
        private String fruitName;
        private String targetCountry;
        private int totalApprovedQuantity;

        // Getters & Setters...
        public int getFruitId() {
            return fruitId;
        }

        public void setFruitId(int f) {
            this.fruitId = f;
        }

        public String getFruitName() {
            return fruitName;
        }

        public void setFruitName(String n) {
            this.fruitName = n;
        }

        public String getTargetCountry() {
            return targetCountry;
        }

        public void setTargetCountry(String c) {
            this.targetCountry = c;
        }

        public int getTotalApprovedQuantity() {
            return totalApprovedQuantity;
        }

        public void setTotalApprovedQuantity(int q) {
            this.totalApprovedQuantity = q;
        }
    }

    // Method to get fulfillable reservations (needed by CheckoutToShopController)
    public List<ReservationBean> getFulfillableReservationsForWarehouse(int centralWarehouseId) {
        List<ReservationBean> reservations = new ArrayList<>();
        // Ensure warehouseDb is initialized
        if (this.warehouseDb == null) {
            LOGGER.log(Level.SEVERE, "WarehouseDB dependency is null in BorrowingDB.");
            return reservations;
        }
        WarehouseBean centralWarehouse = warehouseDb.getWarehouseById(centralWarehouseId); // Uses WarehouseDB
        if (centralWarehouse == null || centralWarehouse.getCountry() == null) {
            LOGGER.log(Level.SEVERE, "Cannot determine country for central warehouse ID: {0}", centralWarehouseId);
            return reservations;
        }
        String warehouseCountry = centralWarehouse.getCountry();
        String sql = "SELECT r.*, f.fruit_name, s.shop_name, s.city " +
                "FROM reservations r JOIN fruits f ON r.fruit_id = f.fruit_id JOIN shops s ON r.shop_id = s.shop_id " +
                "WHERE s.country = ? AND r.status IN ('Approved', 'Shipped') ORDER BY s.city, s.shop_name, f.fruit_name";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, warehouseCountry);
            rs = ps.executeQuery();
            while (rs.next()) {
                ReservationBean bean = new ReservationBean();
                bean.setReservationId(rs.getInt("reservation_id"));
                bean.setFruitId(rs.getInt("fruit_id"));
                bean.setShopId(rs.getInt("shop_id"));
                bean.setQuantity(rs.getInt("quantity"));
                bean.setReservationDate(rs.getDate("reservation_date"));
                bean.setStatus(rs.getString("status"));
                bean.setFruitName(rs.getString("fruit_name"));
                bean.setShopName(rs.getString("shop_name") + " (" + rs.getString("city") + ")");
                reservations.add(bean);
            }
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching fulfillable reservations for country " + warehouseCountry, e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return reservations;
    }

    // Method to handle checkout transaction (needed by CheckoutToShopController)
    public String checkoutDeliveryToShop(int reservationId, int centralWarehouseId) {
        Connection conn = null;
        String statusMessage = "Checkout failed: Unknown error.";
        ReservationBean reservation = null;
        try {
            reservation = getReservationById(reservationId); // Uses helper below
            if (reservation == null)
                return "Checkout failed: Reservation ID " + reservationId + " not found.";
            if (!"Approved".equalsIgnoreCase(reservation.getStatus())
                    && !"Shipped".equalsIgnoreCase(reservation.getStatus()))
                return "Checkout failed: Reservation status is '" + reservation.getStatus()
                        + "', not 'Approved' or 'Shipped'.";

            int fruitId = reservation.getFruitId();
            int shopId = reservation.getShopId();
            int quantity = reservation.getQuantity();
            if (quantity <= 0)
                return "Checkout failed: Reservation quantity is zero or negative.";

            conn = getConnection();
            conn.setAutoCommit(false);

            int currentWarehouseStock = getInventoryQuantityForWarehouse(fruitId, centralWarehouseId, conn); // Uses
                                                                                                             // helper
                                                                                                             // below
            if (currentWarehouseStock < quantity) {
                conn.rollback();
                return "Checkout failed: Insufficient stock (" + currentWarehouseStock + ") at central warehouse "
                        + centralWarehouseId + ".";
            }
            boolean warehouseInvUpdated = updateWarehouseInventory(fruitId, centralWarehouseId, -quantity, conn); // Uses
                                                                                                                  // helper
                                                                                                                  // below
            if (!warehouseInvUpdated) {
                conn.rollback();
                return "Checkout failed: Could not decrease central warehouse inventory.";
            }
            boolean shopInvUpdated = addOrUpdateShopInventory(fruitId, shopId, quantity, conn); // Uses helper above
            if (!shopInvUpdated) {
                conn.rollback();
                return "Checkout failed: Could not increase shop inventory (ShopID: " + shopId + ").";
            }
            boolean statusUpdated = updateReservationStatus(reservationId, "Fulfilled", conn); // Uses helper below
            if (!statusUpdated) {
                conn.rollback();
                return "Checkout failed: Could not update reservation status.";
            }

            conn.commit();
            statusMessage = "Checkout successful for Reservation ID " + reservationId + ".";
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error during checkout transaction for Reservation ID " + reservationId, e);
            statusMessage = "Checkout failed: Database error occurred (" + e.getMessage() + ")";
            if (conn != null)
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Rollback failed", ex);
                }
        } finally {
            if (conn != null)
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Failed to close connection", e);
                }
        }
        return statusMessage;
    }

    // --- Helper methods needed for checkoutDeliveryToShop ---

    // ADDED: Definition for getInventoryQuantityForWarehouse
    private int getInventoryQuantityForWarehouse(int fruitId, int warehouseId, Connection conn) throws SQLException {
        int quantity = 0;
        String sql = "SELECT quantity FROM inventory WHERE fruit_id = ? AND warehouse_id = ? AND shop_id IS NULL";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, fruitId);
            ps.setInt(2, warehouseId);
            rs = ps.executeQuery();
            if (rs.next())
                quantity = rs.getInt("quantity");
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
        return quantity;
    }

    // ADDED: Definition for updateWarehouseInventory
    private boolean updateWarehouseInventory(int fruitId, int warehouseId, int quantityChange, Connection conn)
            throws SQLException {
        String sql = "UPDATE inventory SET quantity = quantity + ? WHERE fruit_id = ? AND warehouse_id = ? AND shop_id IS NULL";
        if (quantityChange < 0)
            sql += " AND quantity >= ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, quantityChange);
            ps.setInt(2, fruitId);
            ps.setInt(3, warehouseId);
            if (quantityChange < 0)
                ps.setInt(4, -quantityChange);
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0 && quantityChange < 0)
                return false;
            return rowsAffected > 0;
        } finally {
            closeQuietly(ps);
        }
    }

    // ADDED: Definition for getReservationById
    private ReservationBean getReservationById(int reservationId) {
        ReservationBean bean = null;
        String sql = "SELECT * FROM reservations WHERE reservation_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, reservationId);
            rs = ps.executeQuery();
            if (rs.next()) {
                bean = new ReservationBean();
                bean.setReservationId(rs.getInt("reservation_id"));
                bean.setFruitId(rs.getInt("fruit_id"));
                bean.setShopId(rs.getInt("shop_id"));
                bean.setQuantity(rs.getInt("quantity"));
                bean.setReservationDate(rs.getDate("reservation_date"));
                bean.setStatus(rs.getString("status"));
            }
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching reservation by ID " + reservationId, e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return bean;
    }

    // ADDED: Definition for updateReservationStatus
    private boolean updateReservationStatus(int reservationId, String newStatus, Connection conn) throws SQLException {
        String sql = "UPDATE reservations SET status = ? WHERE reservation_id = ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setString(1, newStatus);
            ps.setInt(2, reservationId);
            return ps.executeUpdate() == 1;
        } finally {
            closeQuietly(ps);
        }
    } // Add this method inside your existing ReservationDB class

    /**
     * Calculates total fulfilled reservation quantity per fruit within a date
     * range.
     *
     * @param startDate The start date of the period (inclusive).
     * @param endDate   The end date of the period (inclusive).
     * @return A list of ConsumptionDataBean objects (itemName = fruitName).
     */
    public List<ConsumptionDataBean> getConsumptionSummaryByFruit(Date startDate, Date endDate) {
        List<ConsumptionDataBean> reportData = new ArrayList<>();
        // SQL to sum fulfilled reservations by fruit within the date range
        String sql = "SELECT f.fruit_name, SUM(r.quantity) as total_consumed " +
                "FROM reservations r " +
                "JOIN fruits f ON r.fruit_id = f.fruit_id " +
                "WHERE r.status = 'Fulfilled' AND r.reservation_date BETWEEN ? AND ? " +
                "GROUP BY f.fruit_name " +
                "ORDER BY total_consumed DESC";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        // Default date range if null (e.g., last 30 days) - requires more logic
        // For simplicity, assume valid dates are passed for now.
        if (startDate == null || endDate == null) {
            LOGGER.log(Level.WARNING, "Start date or end date is null for consumption report.");
            // Handle default dates or return empty list
            // Example: Set default range (more robust date logic needed)
            // Calendar cal = Calendar.getInstance();
            // if (endDate == null) endDate = new Date(cal.getTimeInMillis());
            // cal.add(Calendar.DAY_OF_MONTH, -30);
            // if (startDate == null) startDate = new Date(cal.getTimeInMillis());
            return reportData; // Return empty for now if dates are null
        }

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setDate(1, startDate);
            ps.setDate(2, endDate);
            rs = ps.executeQuery();

            while (rs.next()) {
                String fruitName = rs.getString("fruit_name");
                long totalConsumed = rs.getLong("total_consumed");
                reportData.add(new ConsumptionDataBean(fruitName, totalConsumed));
            }
            LOGGER.log(Level.INFO, "Fetched {0} rows for consumption summary by fruit between {1} and {2}",
                    new Object[] { reportData.size(), startDate, endDate });

        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching consumption summary by fruit", e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return reportData;
    }

    // --- Add other report methods as needed (e.g., getConsumptionByShop) ---

    // Add this method inside your existing BorrowingDB class

    /**
     * Gets the total inventory quantity for each fruit, grouped by the fruit's
     * source country.
     * This sums inventory across ALL locations (shops and warehouses).
     *
     * @return A list of InventorySummaryBean objects.
     */
    public List<InventorySummaryBean> getInventorySummaryBySourceCountry() {
        List<InventorySummaryBean> summaryList = new ArrayList<>();
        // SQL to sum all inventory quantities, grouped by fruit and its source country
        String sql = "SELECT f.source_country, i.fruit_id, f.fruit_name, SUM(i.quantity) AS total_quantity " +
                "FROM inventory i " +
                "JOIN fruits f ON i.fruit_id = f.fruit_id " +
                "GROUP BY f.source_country, i.fruit_id, f.fruit_name " +
                "HAVING SUM(i.quantity) > 0 " + // Optional: Only show fruits with stock
                "ORDER BY f.source_country, f.fruit_name";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                InventorySummaryBean item = new InventorySummaryBean();
                item.setGroupingDimension(rs.getString("source_country")); // Grouping is by country
                item.setFruitId(rs.getInt("fruit_id"));
                item.setFruitName(rs.getString("fruit_name"));
                item.setTotalQuantity(rs.getLong("total_quantity"));
                summaryList.add(item);
            }
            LOGGER.log(Level.INFO, "Fetched {0} rows for inventory summary by source country.", summaryList.size());

        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching inventory summary by source country", e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return summaryList;
    }

    // TODO: Add similar methods for grouping by city (shops/warehouses
    // separately?), etc.
    // Example: getInventorySummaryByShopCity(),
    // getInventorySummaryByWarehouseCity()
    // Add this method inside your existing BorrowingDB class

    /**
     * Retrieves all borrowing records, including fruit and shop names.
     * Intended for roles like Senior Management.
     *
     * @return A List of BorrowingBean objects.
     */
    public List<BorrowingBean> getAllBorrowings() {
        List<BorrowingBean> borrowings = new ArrayList<>();
        // SQL joining borrowings with fruits and shops (twice for lender/receiver)
        String sql = "SELECT b.*, f.fruit_name, " +
                "       bs.shop_name as borrowing_shop_name, " +
                "       rs.shop_name as receiving_shop_name " +
                "FROM borrowings b " +
                "JOIN fruits f ON b.fruit_id = f.fruit_id " +
                "JOIN shops bs ON b.borrowing_shop_id = bs.shop_id " + // Lending shop
                "JOIN shops rs ON b.receiving_shop_id = rs.shop_id " + // Receiving shop
                "ORDER BY b.borrowing_date DESC, b.borrowing_id DESC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                BorrowingBean bean = new BorrowingBean();
                bean.setBorrowingId(rs.getInt("borrowing_id"));
                bean.setFruitId(rs.getInt("fruit_id"));
                bean.setBorrowingShopId(rs.getInt("borrowing_shop_id"));
                bean.setReceivingShopId(rs.getInt("receiving_shop_id"));
                bean.setQuantity(rs.getInt("quantity"));
                bean.setBorrowingDate(rs.getDate("borrowing_date"));
                bean.setStatus(rs.getString("status")); // Assuming status exists based on Approve-Borrow requirement
                bean.setFruitName(rs.getString("fruit_name"));
                bean.setBorrowingShopName(rs.getString("borrowing_shop_name"));
                bean.setReceivingShopName(rs.getString("receiving_shop_name"));
                borrowings.add(bean);
            }
            LOGGER.log(Level.INFO, "Fetched {0} total borrowing records.", borrowings.size());
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching all borrowings", e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return borrowings;
    }
    // Add this method inside your existing BorrowingDB class

    /**
     * Retrieves borrowing records related to a specific shop (either as lender or
     * receiver).
     * Includes fruit name and shop names.
     *
     * @param shopId The ID of the shop.
     * @return A List of BorrowingBean objects.
     */
    public List<BorrowingBean> getAllBorrowingsForShop(int shopId) {
        List<BorrowingBean> borrowings = new ArrayList<>();
        // SQL joining borrowings with fruits and shops (twice for lender/receiver)
        // Filters where the given shopId is either the lender or the receiver
        String sql = "SELECT b.*, f.fruit_name, " +
                "       bs.shop_name as borrowing_shop_name, " +
                "       rs.shop_name as receiving_shop_name " +
                "FROM borrowings b " +
                "JOIN fruits f ON b.fruit_id = f.fruit_id " +
                "JOIN shops bs ON b.borrowing_shop_id = bs.shop_id " + // Lending shop
                "JOIN shops rs ON b.receiving_shop_id = rs.shop_id " + // Receiving shop
                "WHERE b.borrowing_shop_id = ? OR b.receiving_shop_id = ? " + // Filter for the specific shop
                "ORDER BY b.borrowing_date DESC, b.borrowing_id DESC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, shopId); // Set shopId for the first placeholder
            ps.setInt(2, shopId); // Set shopId for the second placeholder
            rs = ps.executeQuery();
            while (rs.next()) {
                BorrowingBean bean = new BorrowingBean();
                bean.setBorrowingId(rs.getInt("borrowing_id"));
                bean.setFruitId(rs.getInt("fruit_id"));
                bean.setBorrowingShopId(rs.getInt("borrowing_shop_id"));
                bean.setReceivingShopId(rs.getInt("receiving_shop_id"));
                bean.setQuantity(rs.getInt("quantity"));
                bean.setBorrowingDate(rs.getDate("borrowing_date"));
                bean.setStatus(rs.getString("status")); // Assuming status exists
                bean.setFruitName(rs.getString("fruit_name"));
                bean.setBorrowingShopName(rs.getString("borrowing_shop_name"));
                bean.setReceivingShopName(rs.getString("receiving_shop_name"));
                borrowings.add(bean);
            }
            LOGGER.log(Level.INFO, "Fetched {0} borrowing records for ShopID={1}",
                    new Object[] { borrowings.size(), shopId });
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching borrowings for shop " + shopId, e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return borrowings;
    } // Add these methods inside your existing BorrowingDB class

    /**
     * Retrieves a list of all fruits with their available quantity at their
     * respective
     * primary source warehouse.
     *
     * @return A List of OrderableFruitBean objects.
     */
    public List<OrderableFruitBean> getOrderableFruitsFromSource() {
        List<OrderableFruitBean> orderableFruits = new ArrayList<>();
        // SQL joins fruits with their source warehouse (is_source=1) and that
        // warehouse's inventory
        String sql = "SELECT f.fruit_id, f.fruit_name, f.source_country, " +
                "       w.warehouse_id AS source_warehouse_id, " +
                "       COALESCE(inv.quantity, 0) AS available_quantity " + // Show 0 if no inventory record
                "FROM fruits f " +
                "JOIN warehouses w ON f.source_country = w.country AND w.is_source = 1 " + // Find the source warehouse
                "LEFT JOIN inventory inv ON f.fruit_id = inv.fruit_id AND inv.warehouse_id = w.warehouse_id AND inv.shop_id IS NULL "
                + // LEFT JOIN inventory
                "ORDER BY f.fruit_name";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                OrderableFruitBean bean = new OrderableFruitBean(
                        rs.getInt("fruit_id"),
                        rs.getString("fruit_name"),
                        rs.getString("source_country"),
                        rs.getInt("available_quantity"),
                        rs.getInt("source_warehouse_id"));
                orderableFruits.add(bean);
            }
            LOGGER.log(Level.INFO, "Fetched {0} orderable fruits from source warehouses.", orderableFruits.size());

        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching orderable fruits from source", e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return orderableFruits;
    }

    /**
     * Creates multiple reservation records from a single order request,
     * checking inventory and performing operations within a single transaction.
     * If any item fails (e.g., insufficient stock), the entire transaction is
     * rolled back.
     *
     * @param shopId     The ID of the shop making the reservation.
     * @param fruitIds   A list of fruit IDs being ordered.
     * @param quantities A corresponding list of quantities for each fruit ID.
     * @return A status message indicating success or the reason for failure.
     */
    public String createMultipleReservations(int shopId, List<Integer> fruitIds, List<Integer> quantities) {
        Connection conn = null;
        String statusMessage = "Order failed: Unknown error.";

        if (fruitIds == null || quantities == null || fruitIds.size() != quantities.size() || fruitIds.isEmpty()) {
            return "Order failed: Invalid order data provided.";
        }

        // Basic validation: check for non-positive quantities
        for (int qty : quantities) {
            if (qty <= 0) {
                return "Order failed: All quantities must be positive.";
            }
        }

        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Start transaction

            // Process each item in the order
            for (int i = 0; i < fruitIds.size(); i++) {
                int fruitId = fruitIds.get(i);
                int quantity = quantities.get(i);

                // 1. Find the source warehouse for this fruit (re-check within transaction)
                // We could potentially fetch this once outside, but re-checking is safer.
                // Using the existing private helper method.
                int sourceWarehouseId = getSourceWarehouseId(fruitId, conn);
                if (sourceWarehouseId == -1) {
                    conn.rollback();
                    return "Order failed: Could not find source warehouse for Fruit ID " + fruitId + ".";
                }

                // 2. Check current inventory at the source warehouse
                // Using the existing private helper method.
                int currentQuantity = getInventoryQuantityForWarehouse(fruitId, sourceWarehouseId, conn);
                if (currentQuantity < quantity) {
                    conn.rollback();
                    // Fetch fruit name for better error message
                    String fruitName = fruitDb.getFruitById(fruitId) != null
                            ? fruitDb.getFruitById(fruitId).getFruitName()
                            : "ID " + fruitId;
                    return "Order failed: Insufficient stock for " + fruitName + ". Available: " + currentQuantity
                            + ", Requested: " + quantity;
                }
                LOGGER.log(Level.INFO,
                        "[TX-MultiOrder] Inventory check passed for FruitID={0}, WarehouseID={1}. Have: {2}, Need: {3}",
                        new Object[] { fruitId, sourceWarehouseId, currentQuantity, quantity });

                // 3. Add the reservation record (Status: Pending)
                // Using the existing private helper method.
                boolean reservationAdded = addReservationRecord(fruitId, shopId, quantity, "Pending", conn);
                if (!reservationAdded) {
                    conn.rollback();
                    return "Order failed: Could not create reservation record for Fruit ID " + fruitId + ".";
                }
                LOGGER.log(Level.INFO, "[TX-MultiOrder] Reservation record added for FruitID={0}, ShopID={1}, Qty={2}",
                        new Object[] { fruitId, shopId, quantity });

                // 4. Update (decrease) the inventory at the source warehouse
                // Using the existing private helper method.
                boolean inventoryUpdated = updateWarehouseInventory(fruitId, sourceWarehouseId, -quantity, conn);
                if (!inventoryUpdated) {
                    conn.rollback();
                    return "Order failed: Could not update source inventory for Fruit ID " + fruitId
                            + ". Stock might have changed.";
                }
                LOGGER.log(Level.INFO, "[TX-MultiOrder] Inventory updated for FruitID={0}, WarehouseID={1}, Change={2}",
                        new Object[] { fruitId, sourceWarehouseId, -quantity });

            } // End loop for each item

            // If all items processed successfully, commit the transaction
            conn.commit();
            statusMessage = "Order submitted successfully! " + fruitIds.size() + " item(s) reserved.";
            LOGGER.log(Level.INFO, "[TX-MultiOrder] Transaction committed for multiple reservations.");

        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error during multiple reservation transaction for Shop ID " + shopId, e);
            statusMessage = "Order failed: Database error occurred.";
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Rollback failed", ex);
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Failed to close connection", e);
                }
            }
        }
        return statusMessage;
    }

    // --- Ensure these required helper methods exist in this class ---
    // private int getSourceWarehouseId(int fruitId, Connection conn) throws
    // SQLException { ... }
    // private int getInventoryQuantityForWarehouse(int fruitId, int warehouseId,
    // Connection conn) throws SQLException { ... }
    // private boolean addReservationRecord(int fruitId, int shopId, int quantity,
    // String status, Connection conn) throws SQLException { ... }
    // private boolean updateWarehouseInventory(int fruitId, int warehouseId, int
    // quantityChange, Connection conn) throws SQLException { ... }
    /**
     * Finds the warehouse ID marked as the source for a given fruit's source
     * country.
     * Requires the fruitDb field to be initialized.
     * To be called within a transaction or with a managed connection.
     *
     * @param fruitId The ID of the fruit.
     * @param conn    An existing database connection (should not be closed here).
     * @return The source warehouse ID, or -1 if not found or if fruit details are
     *         missing.
     * @throws SQLException if a database access error occurs.
     */
    private int getSourceWarehouseId(int fruitId, Connection conn) throws SQLException {
        int sourceWarehouseId = -1;

        // Ensure fruitDb is available
        if (this.fruitDb == null) {
            LOGGER.log(Level.SEVERE, "FruitDB dependency is null. Cannot get source country for FruitID={0}", fruitId);
            return -1;
        }

        // 1. Get the source country of the fruit
        FruitBean fruit = fruitDb.getFruitById(fruitId); // Uses FruitDB instance
        if (fruit == null || fruit.getSourceCountry() == null || fruit.getSourceCountry().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Fruit not found or source country missing for fruit ID: {0}", fruitId);
            return -1; // Cannot proceed without source country
        }
        String sourceCountry = fruit.getSourceCountry();
        LOGGER.log(Level.FINER, "Finding source warehouse for fruit ID {0} from country: {1}",
                new Object[] { fruitId, sourceCountry });

        // 2. Find the warehouse in that country marked as source
        String sql = "SELECT warehouse_id FROM warehouses WHERE country = ? AND is_source = 1 LIMIT 1";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setString(1, sourceCountry);
            rs = ps.executeQuery();
            if (rs.next()) {
                sourceWarehouseId = rs.getInt("warehouse_id");
                LOGGER.log(Level.FINER, "Source warehouse found: ID={0}", sourceWarehouseId);
            } else {
                LOGGER.log(Level.WARNING, "No source warehouse found for country: {0}", sourceCountry);
            }
        } finally {
            // Only close PS and RS, not the connection passed in
            closeQuietly(rs);
            closeQuietly(ps);
        }
        return sourceWarehouseId;
    }

    /**
     * Adds a reservation record to the database with a specified status.
     * To be called within a transaction.
     *
     * @param fruitId  The ID of the fruit.
     * @param shopId   The ID of the reserving shop.
     * @param quantity The quantity being reserved.
     * @param status   The status to set for the reservation (e.g., "Pending").
     * @param conn     An existing database connection (should not be closed here).
     * @return true if insertion was successful (1 row affected), false otherwise.
     * @throws SQLException if a database access error occurs.
     */
    private boolean addReservationRecord(int fruitId, int shopId, int quantity, String status, Connection conn)
            throws SQLException {
        String sql = "INSERT INTO reservations (fruit_id, shop_id, quantity, reservation_date, status) VALUES (?, ?, ?, CURDATE(), ?)";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, fruitId);
            ps.setInt(2, shopId);
            ps.setInt(3, quantity);
            ps.setString(4, status); // Use the provided status
            int rowsAffected = ps.executeUpdate();
            return rowsAffected == 1; // Check if exactly one row was inserted
        } finally {
            // Only close PS, not the connection passed in
            closeQuietly(ps);
        }
    }
    // Add these methods inside your existing BorrowingDB class

    /**
     * Gets the total inventory quantity for each fruit, grouped by Shop.
     *
     * @return A list of InventorySummaryBean objects.
     */
    public List<InventorySummaryBean> getInventorySummaryByShop() {
        List<InventorySummaryBean> summaryList = new ArrayList<>();
        // SQL to sum inventory quantities, grouped by fruit and shop
        String sql = "SELECT s.shop_name, i.fruit_id, f.fruit_name, SUM(i.quantity) AS total_quantity " +
                "FROM inventory i " +
                "JOIN fruits f ON i.fruit_id = f.fruit_id " +
                "JOIN shops s ON i.shop_id = s.shop_id " + // Only includes shop inventory
                "WHERE i.shop_id IS NOT NULL " + // Ensure it's shop inventory
                "GROUP BY s.shop_name, i.fruit_id, f.fruit_name " +
                "HAVING SUM(i.quantity) > 0 " +
                "ORDER BY s.shop_name, f.fruit_name";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                InventorySummaryBean item = new InventorySummaryBean();
                item.setGroupingDimension(rs.getString("shop_name")); // Grouping is by shop name
                item.setFruitId(rs.getInt("fruit_id"));
                item.setFruitName(rs.getString("fruit_name"));
                item.setTotalQuantity(rs.getLong("total_quantity"));
                summaryList.add(item);
            }
            LOGGER.log(Level.INFO, "Fetched {0} rows for inventory summary by shop.", summaryList.size());
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching inventory summary by shop", e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return summaryList;
    }

    /**
     * Gets the total inventory quantity for each fruit, grouped by City.
     * This sums inventory across shops and warehouses within the same city.
     *
     * @return A list of InventorySummaryBean objects.
     */
    public List<InventorySummaryBean> getInventorySummaryByCity() {
        List<InventorySummaryBean> summaryList = new ArrayList<>();
        // SQL to sum inventory, grouping by fruit and city (from either shop or
        // warehouse)
        String sql = "SELECT " +
                "  COALESCE(s.city, w.city) AS location_city, " + // Get city from shop or warehouse
                "  i.fruit_id, " +
                "  f.fruit_name, " +
                "  SUM(i.quantity) AS total_quantity " +
                "FROM inventory i " +
                "JOIN fruits f ON i.fruit_id = f.fruit_id " +
                "LEFT JOIN shops s ON i.shop_id = s.shop_id " +
                "LEFT JOIN warehouses w ON i.warehouse_id = w.warehouse_id " +
                "WHERE COALESCE(s.city, w.city) IS NOT NULL " + // Ensure we have a city
                "GROUP BY location_city, i.fruit_id, f.fruit_name " +
                "HAVING SUM(i.quantity) > 0 " +
                "ORDER BY location_city, f.fruit_name";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                InventorySummaryBean item = new InventorySummaryBean();
                item.setGroupingDimension(rs.getString("location_city")); // Grouping is by city
                item.setFruitId(rs.getInt("fruit_id"));
                item.setFruitName(rs.getString("fruit_name"));
                item.setTotalQuantity(rs.getLong("total_quantity"));
                summaryList.add(item);
            }
            LOGGER.log(Level.INFO, "Fetched {0} rows for inventory summary by city.", summaryList.size());
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching inventory summary by city", e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return summaryList;
    }

    /**
     * Gets the total inventory quantity for each fruit, grouped by Country.
     * This sums inventory across shops and warehouses within the same country.
     * Note: 'Target Country' usually refers to the destination, this groups by the
     * location's country.
     *
     * @return A list of InventorySummaryBean objects.
     */
    public List<InventorySummaryBean> getInventorySummaryByCountry() {
        List<InventorySummaryBean> summaryList = new ArrayList<>();
        // SQL to sum inventory, grouping by fruit and country (from either shop or
        // warehouse)
        String sql = "SELECT " +
                "  COALESCE(s.country, w.country) AS location_country, " + // Get country from shop or warehouse
                "  i.fruit_id, " +
                "  f.fruit_name, " +
                "  SUM(i.quantity) AS total_quantity " +
                "FROM inventory i " +
                "JOIN fruits f ON i.fruit_id = f.fruit_id " +
                "LEFT JOIN shops s ON i.shop_id = s.shop_id " +
                "LEFT JOIN warehouses w ON i.warehouse_id = w.warehouse_id " +
                "WHERE COALESCE(s.country, w.country) IS NOT NULL " + // Ensure we have a country
                "GROUP BY location_country, i.fruit_id, f.fruit_name " +
                "HAVING SUM(i.quantity) > 0 " +
                "ORDER BY location_country, f.fruit_name";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                InventorySummaryBean item = new InventorySummaryBean();
                item.setGroupingDimension(rs.getString("location_country")); // Grouping is by country
                item.setFruitId(rs.getInt("fruit_id"));
                item.setFruitName(rs.getString("fruit_name"));
                item.setTotalQuantity(rs.getLong("total_quantity"));
                summaryList.add(item);
            }
            LOGGER.log(Level.INFO, "Fetched {0} rows for inventory summary by country.", summaryList.size());
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching inventory summary by country", e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return summaryList;
    }
    // Add these methods inside your existing BorrowingDB class
    // Assume access to bakeryShopDb and warehouseDb

    /**
     * Gets inventory for all shops within a specific city, excluding the requesting
     * shop.
     * Useful for shop staff checking potential borrowing sources.
     *
     * @param city             The city to check.
     * @param requestingShopId The ID of the shop making the request (to exclude).
     * @return List of InventoryBean objects with fruit and shop names.
     */
    public List<InventoryBean> getInventoryForOtherShopsInCity(String city, int requestingShopId) {
        List<InventoryBean> inventoryList = new ArrayList<>();
        String sql = "SELECT i.inventory_id, i.fruit_id, i.shop_id, i.quantity, f.fruit_name, s.shop_name " +
                "FROM inventory i " +
                "JOIN fruits f ON i.fruit_id = f.fruit_id " +
                "JOIN shops s ON i.shop_id = s.shop_id " +
                "WHERE i.shop_id IS NOT NULL AND s.city = ? AND i.shop_id != ? " + // Filter by city, exclude self
                "ORDER BY s.shop_name, f.fruit_name";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, city);
            ps.setInt(2, requestingShopId);
            rs = ps.executeQuery();
            while (rs.next()) {
                InventoryBean item = new InventoryBean();
                item.setInventoryId(rs.getInt("inventory_id"));
                item.setFruitId(rs.getInt("fruit_id"));
                item.setShopId(rs.getInt("shop_id"));
                item.setQuantity(rs.getInt("quantity"));
                item.setFruitName(rs.getString("fruit_name"));
                item.setLocationName(rs.getString("shop_name")); // Use shop name as location
                inventoryList.add(item);
            }
            LOGGER.log(Level.INFO, "Fetched {0} inventory items for other shops in city {1}",
                    new Object[] { inventoryList.size(), city });
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching inventory for other shops in city " + city, e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return inventoryList;
    }

    /**
     * Gets inventory for a specific warehouse by its ID.
     * Reuses getInventoryForWarehouse but makes it public if needed directly by
     * servlet,
     * or keep it private and call via a wrapper if preferred.
     * This version is public for demonstration.
     *
     * @param warehouseId The ID of the warehouse.
     * @return List of InventoryBean objects.
     */
    // Ensure getInventoryForWarehouse is public or create a public wrapper if
    // needed
    // public List<InventoryBean> getWarehouseInventoryById(int warehouseId) {
    // return getInventoryForWarehouse(warehouseId); // Assuming
    // getInventoryForWarehouse exists
    // }

    /**
     * Gets inventory for the primary source warehouse(s) relevant to a specific
     * country.
     * (Useful for staff checking source availability).
     *
     * @param country The country to find source warehouses for.
     * @return List of InventoryBean including warehouse name.
     */
    public List<InventoryBean> getInventoryForSourceWarehouses(String country) {
        List<InventoryBean> inventoryList = new ArrayList<>();
        String sql = "SELECT i.inventory_id, i.fruit_id, i.warehouse_id, i.quantity, f.fruit_name, w.warehouse_name " +
                "FROM inventory i " +
                "JOIN fruits f ON i.fruit_id = f.fruit_id " +
                "JOIN warehouses w ON i.warehouse_id = w.warehouse_id " +
                "WHERE i.shop_id IS NULL AND w.country = ? AND w.is_source = 1 " + // Filter by country and source flag
                "ORDER BY w.warehouse_name, f.fruit_name";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, country);
            rs = ps.executeQuery();
            while (rs.next()) {
                InventoryBean item = new InventoryBean();
                item.setInventoryId(rs.getInt("inventory_id"));
                item.setFruitId(rs.getInt("fruit_id"));
                item.setWarehouseId(rs.getInt("warehouse_id"));
                item.setQuantity(rs.getInt("quantity"));
                item.setFruitName(rs.getString("fruit_name"));
                item.setLocationName(rs.getString("warehouse_name") + " (Source)");
                inventoryList.add(item);
            }
            LOGGER.log(Level.INFO, "Fetched {0} inventory items for source warehouses in {1}",
                    new Object[] { inventoryList.size(), country });
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching source warehouse inventory for " + country, e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return inventoryList;
    }

    /**
     * Gets inventory for the central/distribution warehouse(s) in a specific
     * country.
     * (Useful for shop staff checking central stock).
     *
     * @param country The country to find central warehouses for.
     * @return List of InventoryBean including warehouse name.
     */
    public List<InventoryBean> getInventoryForCentralWarehouses(String country) {
        List<InventoryBean> inventoryList = new ArrayList<>();
        String sql = "SELECT i.inventory_id, i.fruit_id, i.warehouse_id, i.quantity, f.fruit_name, w.warehouse_name " +
                "FROM inventory i " +
                "JOIN fruits f ON i.fruit_id = f.fruit_id " +
                "JOIN warehouses w ON i.warehouse_id = w.warehouse_id " +
                "WHERE i.shop_id IS NULL AND w.country = ? AND (w.is_source = 0 OR w.is_source IS NULL) " + // Filter by
                                                                                                            // country
                                                                                                            // and
                                                                                                            // NON-source
                                                                                                            // flag
                "ORDER BY w.warehouse_name, f.fruit_name";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, country);
            rs = ps.executeQuery();
            while (rs.next()) {
                InventoryBean item = new InventoryBean();
                item.setInventoryId(rs.getInt("inventory_id"));
                item.setFruitId(rs.getInt("fruit_id"));
                item.setWarehouseId(rs.getInt("warehouse_id"));
                item.setQuantity(rs.getInt("quantity"));
                item.setFruitName(rs.getString("fruit_name"));
                item.setLocationName(rs.getString("warehouse_name") + " (Central)");
                inventoryList.add(item);
            }
            LOGGER.log(Level.INFO, "Fetched {0} inventory items for central warehouses in {1}",
                    new Object[] { inventoryList.size(), country });
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching central warehouse inventory for " + country, e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return inventoryList;
    }
    // Add this method inside your existing BorrowingDB class

    /**
     * Retrieves all inventory records from all locations (shops and warehouses).
     * Includes fruit name and location name (shop or warehouse).
     * Intended for Senior Management.
     *
     * @return A list of InventoryBean objects with details populated.
     */
    public List<InventoryBean> getAllInventory() {
        List<InventoryBean> inventoryList = new ArrayList<>();
        // SQL joins inventory with fruits, shops (LEFT JOIN), and warehouses (LEFT
        // JOIN)
        // COALESCE is used to get the name from whichever table (shop/warehouse) is
        // relevant
        // CASE statement determines the location type
        String sql = "SELECT " +
                "  i.inventory_id, i.fruit_id, i.shop_id, i.warehouse_id, i.quantity, " +
                "  f.fruit_name, " +
                "  COALESCE(s.shop_name, w.warehouse_name) AS location_name, " +
                "  CASE WHEN i.shop_id IS NOT NULL THEN 'Shop' ELSE 'Warehouse' END AS location_type " +
                "FROM inventory i " +
                "JOIN fruits f ON i.fruit_id = f.fruit_id " +
                "LEFT JOIN shops s ON i.shop_id = s.shop_id " + // LEFT JOIN for warehouse inventory
                "LEFT JOIN warehouses w ON i.warehouse_id = w.warehouse_id " + // LEFT JOIN for shop inventory
                "ORDER BY location_type, location_name, f.fruit_name";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                InventoryBean item = new InventoryBean();
                item.setInventoryId(rs.getInt("inventory_id"));
                item.setFruitId(rs.getInt("fruit_id"));
                // Use getObject to handle potential NULLs safely
                item.setShopId(rs.getObject("shop_id") != null ? rs.getInt("shop_id") : null);
                item.setWarehouseId(rs.getObject("warehouse_id") != null ? rs.getInt("warehouse_id") : null);
                item.setQuantity(rs.getInt("quantity"));
                item.setFruitName(rs.getString("fruit_name"));
                // Combine location name and type for display
                item.setLocationName(rs.getString("location_name") + " (" + rs.getString("location_type") + ")");
                inventoryList.add(item);
            }
            LOGGER.log(Level.INFO, "Fetched {0} total inventory records.", inventoryList.size());
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching all inventory records", e);
        } finally {
            // Use your existing closeQuietly helper method
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return inventoryList;
    }
    // Add this method inside your existing BorrowingDB class
    // Ensure imports for java.sql.Date and java.math.BigDecimal exist

    /**
     * Calculates the average daily consumption ('Fulfilled' reservations)
     * for each fruit, grouped by the destination shop's country, within a date
     * range.
     *
     * @param startDate The start date of the period (inclusive).
     * @param endDate   The end date of the period (inclusive).
     * @return A list of ForecastBean objects. Returns empty list on error or if
     *         dates are invalid.
     */
    public List<ForecastBean> getAverageDailyConsumptionByFruitAndCountry(Date startDate, Date endDate) {
        List<ForecastBean> forecastData = new ArrayList<>();

        // Validate dates: end date must be on or after start date
        if (startDate == null || endDate == null || startDate.after(endDate)) {
            LOGGER.log(Level.WARNING, "Invalid date range provided for forecast report: Start={0}, End={1}",
                    new Object[] { startDate, endDate });
            return forecastData; // Return empty list for invalid range
        }

        // Calculate number of days in the period (inclusive)
        // Adding 1 because DATEDIFF calculates the difference; we need the count of
        // days.
        // Using Java time for robust calculation
        long periodDays = java.time.temporal.ChronoUnit.DAYS.between(startDate.toLocalDate(), endDate.toLocalDate())
                + 1;
        if (periodDays <= 0) {
            LOGGER.log(Level.WARNING, "Date range results in zero or negative days for forecast report.");
            return forecastData; // Avoid division by zero
        }

        // SQL to sum fulfilled quantity and group by target country and fruit
        String sql = "SELECT " +
                "  s.country AS target_country, " +
                "  r.fruit_id, " +
                "  f.fruit_name, " +
                "  SUM(r.quantity) AS total_consumed " +
                "FROM reservations r " +
                "JOIN fruits f ON r.fruit_id = f.fruit_id " +
                "JOIN shops s ON r.shop_id = s.shop_id " +
                "WHERE r.status = 'Fulfilled' " +
                "  AND r.reservation_date BETWEEN ? AND ? " + // Use reservation_date or a completion_date if available
                "GROUP BY s.country, r.fruit_id, f.fruit_name " +
                "ORDER BY s.country, f.fruit_name";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setDate(1, startDate);
            ps.setDate(2, endDate);
            rs = ps.executeQuery();

            while (rs.next()) {
                ForecastBean bean = new ForecastBean();
                bean.setTargetCountry(rs.getString("target_country"));
                bean.setFruitId(rs.getInt("fruit_id"));
                bean.setFruitName(rs.getString("fruit_name"));

                long totalConsumed = rs.getLong("total_consumed");

                // Calculate average using BigDecimal for precision
                // Ensure java.math.BigDecimal and java.math.RoundingMode are imported
                java.math.BigDecimal avgDaily = new java.math.BigDecimal(totalConsumed)
                        .divide(new java.math.BigDecimal(periodDays), 2, java.math.RoundingMode.HALF_UP); // 2 decimal
                                                                                                          // places

                bean.setAverageDailyConsumption(avgDaily);
                forecastData.add(bean);
            }
            LOGGER.log(Level.INFO,
                    "Calculated average daily consumption for {0} fruit/country combinations between {1} and {2}",
                    new Object[] { forecastData.size(), startDate, endDate });

        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error calculating average daily consumption", e);
        } finally {
            closeQuietly(rs); // Use your existing helper
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return forecastData;
    }
    // Add these methods inside your existing BorrowingDB class
    // Ensure access to bakeryShopDb

    /**
     * Retrieves a list of all other shops located in the same city as the
     * requesting shop.
     *
     * @param city             The city of the requesting shop.
     * @param requestingShopId The ID of the shop making the request (to exclude
     *                         itself).
     * @return A List of BakeryShopBean objects representing potential lenders.
     */
    public List<BakeryShopBean> getOtherShopsInCity(String city, int requestingShopId) {
        List<BakeryShopBean> shops = new ArrayList<>();
        // Query shops table, filtering by city and excluding the requesting shop ID
        String sql = "SELECT shop_id, shop_name, city, country FROM shops WHERE city = ? AND shop_id != ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, city);
            ps.setInt(2, requestingShopId);
            rs = ps.executeQuery();

            while (rs.next()) {
                // Use BakeryShopBean constructor or setters
                BakeryShopBean shop = new BakeryShopBean();
                shop.setShop_id(rs.getString("shop_id")); // Assuming String ID in bean
                shop.setShop_name(rs.getString("shop_name"));
                shop.setCity(rs.getString("city"));
                shop.setCountry(rs.getString("country"));
                shops.add(shop);
            }
            LOGGER.log(Level.INFO, "Found {0} other shops in city {1} for potential borrowing.",
                    new Object[] { shops.size(), city });
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching other shops in city " + city, e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return shops;
    }

    /**
     * Creates multiple 'Pending' borrowing records directed at a single lending
     * shop.
     * Runs within a transaction. If any item fails, the whole batch is rolled back.
     * Does NOT check or update inventory at this stage.
     *
     * @param borrowingShopId The ID of the shop requesting the items.
     * @param lendingShopId   The ID of the shop the request is sent to.
     * @param fruitIds        A list of fruit IDs being requested.
     * @param quantities      A corresponding list of quantities.
     * @return A status message indicating success or failure.
     */
    public String createMultipleBorrowRequests(int borrowingShopId, int lendingShopId, List<Integer> fruitIds,
            List<Integer> quantities) {
        Connection conn = null;
        String statusMessage = "Borrow request submission failed: Unknown error.";

        if (fruitIds == null || quantities == null || fruitIds.size() != quantities.size() || fruitIds.isEmpty()) {
            return "Borrow request failed: Invalid request data.";
        }
        if (borrowingShopId == lendingShopId) {
            return "Borrow request failed: Cannot request to borrow from yourself.";
        }
        // Basic validation for quantities
        for (int qty : quantities) {
            if (qty <= 0)
                return "Borrow request failed: All quantities must be positive.";
        }

        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Start transaction

            // Loop through each requested item and create a PENDING borrowing record
            for (int i = 0; i < fruitIds.size(); i++) {
                int fruitId = fruitIds.get(i);
                int quantity = quantities.get(i);

                // Use the helper method to add the record with "Pending" status
                boolean requestAdded = addBorrowingRecord(fruitId, lendingShopId, borrowingShopId, quantity, "Pending",
                        conn);

                if (!requestAdded) {
                    conn.rollback();
                    // Fetch fruit name for better error message if possible
                    String fruitName = fruitDb != null
                            ? (fruitDb.getFruitById(fruitId) != null ? fruitDb.getFruitById(fruitId).getFruitName()
                                    : "ID " + fruitId)
                            : "ID " + fruitId;
                    return "Borrow request failed: Could not create request record for " + fruitName + ".";
                }
                LOGGER.log(Level.INFO,
                        "[TX-MultiBorrow] Pending borrow record added for FruitID={0}, Qty={1}, Lender={2}, Borrower={3}",
                        new Object[] { fruitId, quantity, lendingShopId, borrowingShopId });
            }

            // If all records added successfully, commit
            conn.commit();
            statusMessage = "Borrow request submitted successfully for " + fruitIds.size()
                    + " item(s)! Waiting for approval from the lending shop.";
            LOGGER.log(Level.INFO, "[TX-MultiBorrow] Transaction committed.");

        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error during multiple borrow request transaction. Borrower: " + borrowingShopId
                    + ", Lender: " + lendingShopId, e);
            statusMessage = "Borrow request failed: Database error occurred.";
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Rollback failed", ex);
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Failed to close connection", e);
                }
            }
        }
        return statusMessage;
    }
    // Add this method inside your existing BorrowingDB class
    // Ensure FruitDB (fruitDb) and BakeryShopDB (bakeryShopDb) are initialized in
    // the constructor

    /**
     * Retrieves a list of all fruits, and for each fruit, finds potential lending
     * shops
     * in the specified city (excluding the requester) that have stock.
     *
     * @param city             The city of the requesting shop.
     * @param requestingShopId The ID of the shop making the request (to exclude).
     * @return A List of BorrowableFruitInfoBean objects.
     */
    public List<BorrowableFruitInfoBean> getBorrowableFruitsWithLenderInfo(String city, int requestingShopId) {
        List<BorrowableFruitInfoBean> resultList = new ArrayList<>();
        // Ensure fruitDb is available
        if (this.fruitDb == null) {
            LOGGER.log(Level.SEVERE, "FruitDB dependency is null in BorrowingDB. Cannot get all fruits.");
            return resultList;
        }

        // 1. Get all fruits first
        List<FruitBean> allFruits = fruitDb.getAllFruits(); // Assuming this method exists in FruitDB

        if (allFruits == null || allFruits.isEmpty()) {
            LOGGER.log(Level.WARNING, "No fruits found in the database.");
            return resultList;
        }

        // 2. Prepare SQL to query inventory for a specific fruit in other shops in the
        // city
        String inventorySql = "SELECT i.shop_id, s.shop_name, i.quantity " +
                "FROM inventory i JOIN shops s ON i.shop_id = s.shop_id " +
                "WHERE i.fruit_id = ? AND s.city = ? AND i.shop_id != ? AND i.quantity > 0"; // Only shops with quantity
                                                                                             // > 0

        Connection conn = null;
        PreparedStatement psInv = null;
        ResultSet rsInv = null;

        try {
            conn = getConnection();
            psInv = conn.prepareStatement(inventorySql);

            // 3. For each fruit, find its potential lenders in the specified city
            for (FruitBean fruit : allFruits) {
                BorrowableFruitInfoBean infoBean = new BorrowableFruitInfoBean(fruit); // Create the wrapper bean
                List<Map<String, Object>> lenders = new ArrayList<>();

                // Set parameters for the inventory query
                psInv.setInt(1, fruit.getFruitId());
                psInv.setString(2, city);
                psInv.setInt(3, requestingShopId);

                rsInv = psInv.executeQuery();

                // Collect lender info for this fruit
                while (rsInv.next()) {
                    Map<String, Object> lender = new HashMap<>();
                    lender.put("shopId", rsInv.getInt("shop_id"));
                    lender.put("shopName", rsInv.getString("shop_name"));
                    lender.put("quantity", rsInv.getInt("quantity"));
                    lenders.add(lender);
                }
                closeQuietly(rsInv); // Close inner ResultSet for each fruit

                infoBean.setLenderInfo(lenders); // Attach the lender list to the fruit bean
                resultList.add(infoBean); // Add the enhanced bean to the final list
            }
            LOGGER.log(Level.INFO, "Processed lender info for {0} fruits in city {1}",
                    new Object[] { resultList.size(), city });

        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching borrowable fruits with lender info for city " + city, e);
        } finally {
            // Close outer PreparedStatement and Connection once after the loop
            closeQuietly(psInv);
            closeQuietly(conn);
        }
        return resultList;
    }

    // --- Ensure this helper method exists (takes status as parameter) ---
    // private boolean addBorrowingRecord(int fruitId, int lendingShopId, int
    // borrowingShopId, int quantity, String status, Connection conn) throws
    // SQLException { ... }

} // End of BorrowingDB class
