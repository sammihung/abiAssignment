package ict.db;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection; // Make sure this bean exists if getAggregatedNeedsByCountry is used here
import java.sql.Date; // Using BakeryShopBean
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList; // Need this for WarehouseDB dependency
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import ict.bean.BorrowingBean;
import ict.bean.ConsumptionDataBean;
import ict.bean.InventoryBean;
import ict.bean.InventorySummaryBean;
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

    public List<Map<String, Object>> findPotentialLenders(int fruitId, int requiredQuantity, String city,
            int requestingShopId) {
        List<Map<String, Object>> lenders = new ArrayList<>();
        String sql = "SELECT s.shop_id, s.shop_name, i.quantity " +
                "FROM shops s JOIN inventory i ON s.shop_id = i.shop_id " +
                "WHERE s.city = ? AND s.shop_id != ? AND i.fruit_id = ? AND i.quantity >= ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, city);
            ps.setInt(2, requestingShopId);
            ps.setInt(3, fruitId);
            ps.setInt(4, requiredQuantity);
            rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> lenderInfo = new HashMap<>();
                lenderInfo.put("shopId", rs.getInt("shop_id"));
                lenderInfo.put("shopName", rs.getString("shop_name"));
                lenderInfo.put("availableQuantity", rs.getInt("quantity"));
                lenders.add(lenderInfo);
            }
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error finding potential lenders", e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return lenders;
    }

    public String createBorrowing(int fruitId, int lendingShopId, int borrowingShopId, int quantity) {
        Connection conn = null;
        String statusMessage = "Borrowing failed: Unknown error.";
        if (quantity <= 0)
            return "Borrowing failed: Quantity must be positive.";
        if (lendingShopId == borrowingShopId)
            return "Borrowing failed: Cannot borrow from yourself.";

        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            int currentQuantity = getInventoryQuantityForShop(fruitId, lendingShopId, conn);
            if (currentQuantity < quantity) {
                conn.rollback();
                return "Borrowing failed: Lending shop stock changed. Available: " + currentQuantity;
            }
            boolean borrowingAdded = addBorrowingRecord(fruitId, lendingShopId, borrowingShopId, quantity, conn);
            if (!borrowingAdded) {
                conn.rollback();
                return "Borrowing failed: Could not create borrowing record.";
            }
            boolean lenderInventoryUpdated = updateShopInventory(fruitId, lendingShopId, -quantity, conn);
            if (!lenderInventoryUpdated) {
                conn.rollback();
                return "Borrowing failed: Could not update lending shop's inventory.";
            }
            boolean borrowerInventoryUpdated = addOrUpdateShopInventory(fruitId, borrowingShopId, quantity, conn);
            if (!borrowerInventoryUpdated) {
                conn.rollback();
                return "Borrowing failed: Could not update borrowing shop's inventory.";
            }
            conn.commit();
            statusMessage = "Borrowing request created successfully!";
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error during borrowing transaction", e);
            statusMessage = "Borrowing failed: Database error occurred (" + e.getMessage() + ")";
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

    // --- Borrowing Transaction Helper Methods ---

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

    private boolean addBorrowingRecord(int fruitId, int lendingShopId, int borrowingShopId, int quantity,
            Connection conn) throws SQLException {
        String sql = "INSERT INTO borrowings (fruit_id, borrowing_shop_id, receiving_shop_id, quantity, borrowing_date, status) VALUES (?, ?, ?, ?, CURDATE(), ?)";
        PreparedStatement ps = null;
        String initialStatus = "Borrowed";
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, fruitId);
            ps.setInt(2, lendingShopId);
            ps.setInt(3, borrowingShopId);
            ps.setInt(4, quantity);
            ps.setString(5, initialStatus);
            return ps.executeUpdate() >= 1;
        } finally {
            closeQuietly(ps);
        }
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
    }

} // End of BorrowingDB class
