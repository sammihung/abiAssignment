package ict.db;

import java.io.IOException; // Make sure this bean exists if getAggregatedNeedsByCountry is used here
import java.io.Serializable; // Using BakeryShopBean
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException; // Need this for WarehouseDB dependency
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import ict.bean.InventoryBean;
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
    }

} // End of BorrowingDB class
