package ict.db;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import ict.bean.AggregatedNeedBean;
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
import ict.db.ReservationDB.SeasonalConsumptionBean;

public class BorrowingDB {

    private static final Logger LOGGER = Logger.getLogger(BorrowingDB.class.getName());
    private String dburl, username, password;
    private BakeryShopDB bakeryShopDb;
    private WarehouseDB warehouseDb;
    private FruitDB fruitDb;

    public BorrowingDB(String dburl, String dbUser, String dbPassword) {
        this.dburl = dburl;
        this.username = dbUser;
        this.password = dbPassword;

        this.bakeryShopDb = new BakeryShopDB(dburl, dbUser, dbPassword);
        this.warehouseDb = new WarehouseDB(dburl, dbUser, dbPassword);
        this.fruitDb = new FruitDB(dburl, dbUser, dbPassword);
    }

    public Connection getConnection() throws SQLException, IOException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "MySQL JDBC Driver not found.", e);
            throw new IOException("Database driver not found.", e);
        }
        return DriverManager.getConnection(dburl, username, password);
    }

    private void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to close resource: " + resource.getClass().getSimpleName(), e);
            }
        }
    }

    private boolean addBorrowingRecord(int fruitId, int lendingShopId, int borrowingShopId, int quantity, String status,
            Connection conn) throws SQLException {
        String sql = "INSERT INTO borrowings (fruit_id, borrowing_shop_id, receiving_shop_id, quantity, borrowing_date, status) VALUES (?, ?, ?, ?, CURDATE(), ?)";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, fruitId);
            ps.setInt(2, lendingShopId);
            ps.setInt(3, borrowingShopId);
            ps.setInt(4, quantity);
            ps.setString(5, status);
            return ps.executeUpdate() >= 1;
        } finally {
            closeQuietly(ps);
        }
    }

    public List<BorrowingBean> getPendingBorrowRequests(int lendingShopId) {
        List<BorrowingBean> requests = new ArrayList<>();
        String sql = "SELECT b.*, f.fruit_name, rs.shop_name as receiving_shop_name " +
                "FROM borrowings b " +
                "JOIN fruits f ON b.fruit_id = f.fruit_id " +
                "JOIN shops rs ON b.receiving_shop_id = rs.shop_id " +
                "WHERE b.borrowing_shop_id = ? AND b.status = 'Pending' " +
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
                bean.setBorrowingShopId(rs.getInt("borrowing_shop_id"));
                bean.setReceivingShopId(rs.getInt("receiving_shop_id"));
                bean.setQuantity(rs.getInt("quantity"));
                bean.setBorrowingDate(rs.getDate("borrowing_date"));
                bean.setStatus(rs.getString("status"));
                bean.setFruitName(rs.getString("fruit_name"));

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

            requestDetails = getBorrowingById(borrowingId);

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

            conn = getConnection();
            conn.setAutoCommit(false);

            int currentLenderStock = getInventoryQuantityForShop(fruitId, lendingShopId, conn);
            if (currentLenderStock < quantity) {
                conn.rollback();

                return "Approval failed: Insufficient stock (" + currentLenderStock + ") to fulfill request.";
            }
            LOGGER.log(Level.INFO,
                    "[TX-ApproveBorrow] Lender inventory check passed for ShopID={0}, FruitID={1}. Have: {2}, Need: {3}",
                    new Object[] { lendingShopId, fruitId, currentLenderStock, quantity });

            boolean statusUpdated = updateBorrowingStatus(borrowingId, "Approved", conn);
            if (!statusUpdated) {
                conn.rollback();
                return "Approval failed: Could not update borrowing status.";
            }
            LOGGER.log(Level.INFO, "[TX-ApproveBorrow] Borrowing status updated to Approved.");

            boolean lenderInvUpdated = updateShopInventory(fruitId, lendingShopId, -quantity, conn);
            if (!lenderInvUpdated) {
                conn.rollback();
                return "Approval failed: Could not decrease lending shop inventory.";
            }
            LOGGER.log(Level.INFO, "[TX-ApproveBorrow] Lender inventory decreased.");

            boolean borrowerInvUpdated = addOrUpdateShopInventory(fruitId, receivingShopId, quantity, conn);
            if (!borrowerInvUpdated) {
                conn.rollback();

                return "Approval failed: Could not increase receiving shop inventory.";
            }
            LOGGER.log(Level.INFO, "[TX-ApproveBorrow] Borrower inventory increased.");

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

            requestDetails = getBorrowingById(borrowingId);

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

            conn = getConnection();
            conn.setAutoCommit(false);

            boolean statusUpdated = updateBorrowingStatus(borrowingId, "Rejected", conn);

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
            String insertSql = "INSERT INTO inventory (fruit_id, shop_id, quantity, warehouse_id) VALUES (?, ?, ?, NULL)";
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

            if (inventoryId != -1) {
                psUpdate = conn.prepareStatement(updateSql);
                psUpdate.setInt(1, newQuantity);
                psUpdate.setInt(2, inventoryId);
                success = psUpdate.executeUpdate() > 0;
                closeQuietly(psUpdate);
            } else {
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

    public List<InventoryBean> getInventoryForWarehouse(int warehouseId) {
        List<InventoryBean> inventoryList = new ArrayList<>();

        String sql = "SELECT i.inventory_id, i.fruit_id, i.shop_id, i.warehouse_id, i.quantity, f.fruit_name, f.source_country "
                +
                "FROM inventory i " +
                "JOIN fruits f ON i.fruit_id = f.fruit_id " +
                "WHERE i.warehouse_id = ? AND i.shop_id IS NULL " +
                "ORDER BY f.fruit_name";
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
                item.setSourceCountry(rs.getString("source_country"));

                inventoryList.add(item);
            }
            LOGGER.log(Level.INFO, "Fetched {0} inventory items for WarehouseID={1}",
                    new Object[] { inventoryList.size(), warehouseId });
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching inventory for warehouse " + warehouseId, e);
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

            if (inventoryId != -1) {
                psUpdate = conn.prepareStatement(updateSql);
                psUpdate.setInt(1, newQuantity);
                psUpdate.setInt(2, inventoryId);
                success = psUpdate.executeUpdate() > 0;
                closeQuietly(psUpdate);
            } else {
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

    public static class DeliveryNeedBean implements Serializable {
        private int fruitId;
        private String fruitName;
        private String targetCountry;
        private int totalApprovedQuantity;

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

    public List<ReservationBean> getFulfillableReservationsForWarehouse(int centralWarehouseId) {
        List<ReservationBean> reservations = new ArrayList<>();

        if (this.warehouseDb == null) {
            LOGGER.log(Level.SEVERE, "WarehouseDB dependency is null in BorrowingDB.");
            return reservations;
        }
        WarehouseBean centralWarehouse = warehouseDb.getWarehouseById(centralWarehouseId);
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

    public String checkoutDeliveryToShop(int reservationId, int centralWarehouseId) {
        Connection conn = null;
        String statusMessage = "Checkout failed: Unknown error.";
        ReservationBean reservation = null;
        try {
            reservation = getReservationById(reservationId);
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

            int currentWarehouseStock = getInventoryQuantityForWarehouse(fruitId, centralWarehouseId, conn);
            if (currentWarehouseStock < quantity) {
                conn.rollback();
                return "Checkout failed: Insufficient stock (" + currentWarehouseStock + ") at central warehouse "
                        + centralWarehouseId + ".";
            }
            boolean warehouseInvUpdated = updateWarehouseInventory(fruitId, centralWarehouseId, -quantity, conn);
            if (!warehouseInvUpdated) {
                conn.rollback();
                return "Checkout failed: Could not decrease central warehouse inventory.";
            }
            boolean shopInvUpdated = addOrUpdateShopInventory(fruitId, shopId, quantity, conn);
            if (!shopInvUpdated) {
                conn.rollback();
                return "Checkout failed: Could not increase shop inventory (ShopID: " + shopId + ").";
            }
            boolean statusUpdated = updateReservationStatus(reservationId, "Fulfilled", conn);
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

    public List<ConsumptionDataBean> getConsumptionSummaryByFruit(Date startDate, Date endDate) {
        List<ConsumptionDataBean> reportData = new ArrayList<>();

        String sql = "SELECT f.fruit_name, SUM(r.quantity) as total_consumed " +
                "FROM reservations r " +
                "JOIN fruits f ON r.fruit_id = f.fruit_id " +
                "WHERE r.status = 'Fulfilled' AND r.reservation_date BETWEEN ? AND ? " +
                "GROUP BY f.fruit_name " +
                "ORDER BY total_consumed DESC";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        if (startDate == null || endDate == null) {
            LOGGER.log(Level.WARNING, "Start date or end date is null for consumption report.");

            return reportData;
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

    public List<InventorySummaryBean> getInventorySummaryBySourceCountry() {
        List<InventorySummaryBean> summaryList = new ArrayList<>();

        String sql = "SELECT f.source_country, i.fruit_id, f.fruit_name, SUM(i.quantity) AS total_quantity " +
                "FROM inventory i " +
                "JOIN fruits f ON i.fruit_id = f.fruit_id " +
                "GROUP BY f.source_country, i.fruit_id, f.fruit_name " +
                "HAVING SUM(i.quantity) > 0 " +
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
                item.setGroupingDimension(rs.getString("source_country"));
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

    public List<BorrowingBean> getAllBorrowings() {
        List<BorrowingBean> borrowings = new ArrayList<>();

        String sql = "SELECT b.*, f.fruit_name, " +
                "       bs.shop_name as borrowing_shop_name, " +
                "       rs.shop_name as receiving_shop_name " +
                "FROM borrowings b " +
                "JOIN fruits f ON b.fruit_id = f.fruit_id " +
                "JOIN shops bs ON b.borrowing_shop_id = bs.shop_id " +
                "JOIN shops rs ON b.receiving_shop_id = rs.shop_id " +
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
                bean.setStatus(rs.getString("status"));
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

    public List<BorrowingBean> getAllBorrowingsForShop(int shopId) {
        List<BorrowingBean> borrowings = new ArrayList<>();

        String sql = "SELECT b.*, f.fruit_name, " +
                "       bs.shop_name as borrowing_shop_name, " +
                "       rs.shop_name as receiving_shop_name " +
                "FROM borrowings b " +
                "JOIN fruits f ON b.fruit_id = f.fruit_id " +
                "JOIN shops bs ON b.borrowing_shop_id = bs.shop_id " +
                "JOIN shops rs ON b.receiving_shop_id = rs.shop_id " +
                "WHERE b.borrowing_shop_id = ? OR b.receiving_shop_id = ? " +
                "ORDER BY b.borrowing_date DESC, b.borrowing_id DESC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, shopId);
            ps.setInt(2, shopId);
            rs = ps.executeQuery();
            while (rs.next()) {
                BorrowingBean bean = new BorrowingBean();
                bean.setBorrowingId(rs.getInt("borrowing_id"));
                bean.setFruitId(rs.getInt("fruit_id"));
                bean.setBorrowingShopId(rs.getInt("borrowing_shop_id"));
                bean.setReceivingShopId(rs.getInt("receiving_shop_id"));
                bean.setQuantity(rs.getInt("quantity"));
                bean.setBorrowingDate(rs.getDate("borrowing_date"));
                bean.setStatus(rs.getString("status"));
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

    public List<OrderableFruitBean> getOrderableFruitsFromSource() {
        List<OrderableFruitBean> orderableFruits = new ArrayList<>();

        String sql = "SELECT f.fruit_id, f.fruit_name, f.source_country, " +
                "       w.warehouse_id AS source_warehouse_id, " +
                "       COALESCE(inv.quantity, 0) AS available_quantity " +
                "FROM fruits f " +
                "JOIN warehouses w ON f.source_country = w.country AND w.is_source = 1 " +
                "LEFT JOIN inventory inv ON f.fruit_id = inv.fruit_id AND inv.warehouse_id = w.warehouse_id AND inv.shop_id IS NULL "
                +
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

    public String createMultipleReservations(int shopId, List<Integer> fruitIds, List<Integer> quantities) {
        Connection conn = null;
        String statusMessage = "Order failed: Unknown error.";

        if (fruitIds == null || quantities == null || fruitIds.size() != quantities.size() || fruitIds.isEmpty()) {
            return "Order failed: Invalid order data provided.";
        }

        for (int qty : quantities) {
            if (qty <= 0) {
                return "Order failed: All quantities must be positive.";
            }
        }

        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            for (int i = 0; i < fruitIds.size(); i++) {
                int fruitId = fruitIds.get(i);
                int quantity = quantities.get(i);

                int sourceWarehouseId = getSourceWarehouseId(fruitId, conn);
                if (sourceWarehouseId == -1) {
                    conn.rollback();
                    return "Order failed: Could not find source warehouse for Fruit ID " + fruitId + ".";
                }

                int currentQuantity = getInventoryQuantityForWarehouse(fruitId, sourceWarehouseId, conn);
                if (currentQuantity < quantity) {
                    conn.rollback();

                    String fruitName = fruitDb.getFruitById(fruitId) != null
                            ? fruitDb.getFruitById(fruitId).getFruitName()
                            : "ID " + fruitId;
                    return "Order failed: Insufficient stock for " + fruitName + ". Available: " + currentQuantity
                            + ", Requested: " + quantity;
                }
                LOGGER.log(Level.INFO,
                        "[TX-MultiOrder] Inventory check passed for FruitID={0}, WarehouseID={1}. Have: {2}, Need: {3}",
                        new Object[] { fruitId, sourceWarehouseId, currentQuantity, quantity });

                boolean reservationAdded = addReservationRecord(fruitId, shopId, quantity, "Pending", conn);
                if (!reservationAdded) {
                    conn.rollback();
                    return "Order failed: Could not create reservation record for Fruit ID " + fruitId + ".";
                }
                LOGGER.log(Level.INFO, "[TX-MultiOrder] Reservation record added for FruitID={0}, ShopID={1}, Qty={2}",
                        new Object[] { fruitId, shopId, quantity });

                boolean inventoryUpdated = updateWarehouseInventory(fruitId, sourceWarehouseId, -quantity, conn);
                if (!inventoryUpdated) {
                    conn.rollback();
                    return "Order failed: Could not update source inventory for Fruit ID " + fruitId
                            + ". Stock might have changed.";
                }
                LOGGER.log(Level.INFO, "[TX-MultiOrder] Inventory updated for FruitID={0}, WarehouseID={1}, Change={2}",
                        new Object[] { fruitId, sourceWarehouseId, -quantity });

            }

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

    private int getSourceWarehouseId(int fruitId, Connection conn) throws SQLException {
        int sourceWarehouseId = -1;

        if (this.fruitDb == null) {
            LOGGER.log(Level.SEVERE, "FruitDB dependency is null. Cannot get source country for FruitID={0}", fruitId);
            return -1;
        }

        FruitBean fruit = fruitDb.getFruitById(fruitId);
        if (fruit == null || fruit.getSourceCountry() == null || fruit.getSourceCountry().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Fruit not found or source country missing for fruit ID: {0}", fruitId);
            return -1;
        }
        String sourceCountry = fruit.getSourceCountry();
        LOGGER.log(Level.FINER, "Finding source warehouse for fruit ID {0} from country: {1}",
                new Object[] { fruitId, sourceCountry });

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

            closeQuietly(rs);
            closeQuietly(ps);
        }
        return sourceWarehouseId;
    }

    private boolean addReservationRecord(int fruitId, int shopId, int quantity, String status, Connection conn)
            throws SQLException {
        String sql = "INSERT INTO reservations (fruit_id, shop_id, quantity, reservation_date, status) VALUES (?, ?, ?, CURDATE(), ?)";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, fruitId);
            ps.setInt(2, shopId);
            ps.setInt(3, quantity);
            ps.setString(4, status);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected == 1;
        } finally {

            closeQuietly(ps);
        }
    }

    public List<InventorySummaryBean> getInventorySummaryByShop() {
        List<InventorySummaryBean> summaryList = new ArrayList<>();

        String sql = "SELECT s.shop_name, i.fruit_id, f.fruit_name, SUM(i.quantity) AS total_quantity " +
                "FROM inventory i " +
                "JOIN fruits f ON i.fruit_id = f.fruit_id " +
                "JOIN shops s ON i.shop_id = s.shop_id " +
                "WHERE i.shop_id IS NOT NULL " +
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
                item.setGroupingDimension(rs.getString("shop_name"));
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

    public List<InventorySummaryBean> getInventorySummaryByCity() {
        List<InventorySummaryBean> summaryList = new ArrayList<>();

        String sql = "SELECT " +
                "  COALESCE(s.city, w.city) AS location_city, " +
                "  i.fruit_id, " +
                "  f.fruit_name, " +
                "  SUM(i.quantity) AS total_quantity " +
                "FROM inventory i " +
                "JOIN fruits f ON i.fruit_id = f.fruit_id " +
                "LEFT JOIN shops s ON i.shop_id = s.shop_id " +
                "LEFT JOIN warehouses w ON i.warehouse_id = w.warehouse_id " +
                "WHERE COALESCE(s.city, w.city) IS NOT NULL " +
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
                item.setGroupingDimension(rs.getString("location_city"));
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

    public List<InventorySummaryBean> getInventorySummaryByCountry() {
        List<InventorySummaryBean> summaryList = new ArrayList<>();

        String sql = "SELECT " +
                "  COALESCE(s.country, w.country) AS location_country, " +
                "  i.fruit_id, " +
                "  f.fruit_name, " +
                "  SUM(i.quantity) AS total_quantity " +
                "FROM inventory i " +
                "JOIN fruits f ON i.fruit_id = f.fruit_id " +
                "LEFT JOIN shops s ON i.shop_id = s.shop_id " +
                "LEFT JOIN warehouses w ON i.warehouse_id = w.warehouse_id " +
                "WHERE COALESCE(s.country, w.country) IS NOT NULL " +
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
                item.setGroupingDimension(rs.getString("location_country"));
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

    public List<InventoryBean> getInventoryForOtherShopsInCity(String city, int requestingShopId) {
        List<InventoryBean> inventoryList = new ArrayList<>();
        String sql = "SELECT i.inventory_id, i.fruit_id, i.shop_id, i.quantity, f.fruit_name, s.shop_name " +
                "FROM inventory i " +
                "JOIN fruits f ON i.fruit_id = f.fruit_id " +
                "JOIN shops s ON i.shop_id = s.shop_id " +
                "WHERE i.shop_id IS NOT NULL AND s.city = ? AND i.shop_id != ? " +
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
                item.setLocationName(rs.getString("shop_name"));
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

    public List<InventoryBean> getInventoryForSourceWarehouses(String country) {
        List<InventoryBean> inventoryList = new ArrayList<>();
        String sql = "SELECT i.inventory_id, i.fruit_id, i.warehouse_id, i.quantity, f.fruit_name, w.warehouse_name " +
                "FROM inventory i " +
                "JOIN fruits f ON i.fruit_id = f.fruit_id " +
                "JOIN warehouses w ON i.warehouse_id = w.warehouse_id " +
                "WHERE i.shop_id IS NULL AND w.country = ? AND w.is_source = 1 " +
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

    public List<InventoryBean> getInventoryForCentralWarehouses(String country) {
        List<InventoryBean> inventoryList = new ArrayList<>();
        String sql = "SELECT i.inventory_id, i.fruit_id, i.warehouse_id, i.quantity, f.fruit_name, w.warehouse_name " +
                "FROM inventory i " +
                "JOIN fruits f ON i.fruit_id = f.fruit_id " +
                "JOIN warehouses w ON i.warehouse_id = w.warehouse_id " +
                "WHERE i.shop_id IS NULL AND w.country = ? AND (w.is_source = 0 OR w.is_source IS NULL) " +

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

    public List<InventoryBean> getAllInventory() {
        List<InventoryBean> inventoryList = new ArrayList<>();

        String sql = "SELECT " +
                "  i.inventory_id, i.fruit_id, i.shop_id, i.warehouse_id, i.quantity, " +
                "  f.fruit_name, " +
                "  COALESCE(s.shop_name, w.warehouse_name) AS location_name, " +
                "  CASE WHEN i.shop_id IS NOT NULL THEN 'Shop' ELSE 'Warehouse' END AS location_type " +
                "FROM inventory i " +
                "JOIN fruits f ON i.fruit_id = f.fruit_id " +
                "LEFT JOIN shops s ON i.shop_id = s.shop_id " +
                "LEFT JOIN warehouses w ON i.warehouse_id = w.warehouse_id " +
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

                item.setShopId(rs.getObject("shop_id") != null ? rs.getInt("shop_id") : null);
                item.setWarehouseId(rs.getObject("warehouse_id") != null ? rs.getInt("warehouse_id") : null);
                item.setQuantity(rs.getInt("quantity"));
                item.setFruitName(rs.getString("fruit_name"));

                item.setLocationName(rs.getString("location_name") + " (" + rs.getString("location_type") + ")");
                inventoryList.add(item);
            }
            LOGGER.log(Level.INFO, "Fetched {0} total inventory records.", inventoryList.size());
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching all inventory records", e);
        } finally {

            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return inventoryList;
    }

    public List<ForecastBean> getAverageDailyConsumptionByFruitAndCountry(Date startDate, Date endDate) {
        List<ForecastBean> forecastData = new ArrayList<>();

        if (startDate == null || endDate == null || startDate.after(endDate)) {
            LOGGER.log(Level.WARNING, "Invalid date range provided for forecast report: Start={0}, End={1}",
                    new Object[] { startDate, endDate });
            return forecastData;
        }

        long periodDays = java.time.temporal.ChronoUnit.DAYS.between(startDate.toLocalDate(), endDate.toLocalDate())
                + 1;
        if (periodDays <= 0) {
            LOGGER.log(Level.WARNING, "Date range results in zero or negative days for forecast report.");
            return forecastData;
        }

        String sql = "SELECT " +
                "  s.country AS target_country, " +
                "  r.fruit_id, " +
                "  f.fruit_name, " +
                "  SUM(r.quantity) AS total_consumed " +
                "FROM reservations r " +
                "JOIN fruits f ON r.fruit_id = f.fruit_id " +
                "JOIN shops s ON r.shop_id = s.shop_id " +
                "WHERE r.status = 'Fulfilled' " +
                "  AND r.reservation_date BETWEEN ? AND ? " +
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

                java.math.BigDecimal avgDaily = new java.math.BigDecimal(totalConsumed)
                        .divide(new java.math.BigDecimal(periodDays), 2, java.math.RoundingMode.HALF_UP);

                bean.setAverageDailyConsumption(avgDaily);
                forecastData.add(bean);
            }
            LOGGER.log(Level.INFO,
                    "Calculated average daily consumption for {0} fruit/country combinations between {1} and {2}",
                    new Object[] { forecastData.size(), startDate, endDate });

        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error calculating average daily consumption", e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return forecastData;
    }

    public List<BakeryShopBean> getOtherShopsInCity(String city, int requestingShopId) {
        List<BakeryShopBean> shops = new ArrayList<>();

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

                BakeryShopBean shop = new BakeryShopBean();
                shop.setShop_id(rs.getString("shop_id"));
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

        for (int qty : quantities) {
            if (qty <= 0)
                return "Borrow request failed: All quantities must be positive.";
        }

        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            for (int i = 0; i < fruitIds.size(); i++) {
                int fruitId = fruitIds.get(i);
                int quantity = quantities.get(i);

                boolean requestAdded = addBorrowingRecord(fruitId, lendingShopId, borrowingShopId, quantity, "Pending",
                        conn);

                if (!requestAdded) {
                    conn.rollback();

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

    public List<BorrowableFruitInfoBean> getBorrowableFruitsWithLenderInfo(String city, int requestingShopId) {
        List<BorrowableFruitInfoBean> resultList = new ArrayList<>();

        if (this.fruitDb == null) {
            LOGGER.log(Level.SEVERE, "FruitDB dependency is null in BorrowingDB. Cannot get all fruits.");
            return resultList;
        }

        List<FruitBean> allFruits = fruitDb.getAllFruits();

        if (allFruits == null || allFruits.isEmpty()) {
            LOGGER.log(Level.WARNING, "No fruits found in the database.");
            return resultList;
        }

        String inventorySql = "SELECT i.shop_id, s.shop_name, i.quantity " +
                "FROM inventory i JOIN shops s ON i.shop_id = s.shop_id " +
                "WHERE i.fruit_id = ? AND s.city = ? AND i.shop_id != ? AND i.quantity > 0";

        Connection conn = null;
        PreparedStatement psInv = null;
        ResultSet rsInv = null;

        try {
            conn = getConnection();
            psInv = conn.prepareStatement(inventorySql);

            for (FruitBean fruit : allFruits) {
                BorrowableFruitInfoBean infoBean = new BorrowableFruitInfoBean(fruit);
                List<Map<String, Object>> lenders = new ArrayList<>();

                psInv.setInt(1, fruit.getFruitId());
                psInv.setString(2, city);
                psInv.setInt(3, requestingShopId);

                rsInv = psInv.executeQuery();

                while (rsInv.next()) {
                    Map<String, Object> lender = new HashMap<>();
                    lender.put("shopId", rsInv.getInt("shop_id"));
                    lender.put("shopName", rsInv.getString("shop_name"));
                    lender.put("quantity", rsInv.getInt("quantity"));
                    lenders.add(lender);
                }
                closeQuietly(rsInv);

                infoBean.setLenderInfo(lenders);
                resultList.add(infoBean);
            }
            LOGGER.log(Level.INFO, "Processed lender info for {0} fruits in city {1}",
                    new Object[] { resultList.size(), city });

        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching borrowable fruits with lender info for city " + city, e);
        } finally {

            closeQuietly(psInv);
            closeQuietly(conn);
        }
        return resultList;
    }

    public List<AggregatedNeedBean> getAllAggregatedNeeds() {
        List<AggregatedNeedBean> needs = new ArrayList<>();

        String sql = "SELECT f.fruit_name, f.fruit_id, SUM(r.quantity) AS total_needed_quantity " +
                "FROM reservations r " +
                "JOIN fruits f ON r.fruit_id = f.fruit_id " +
                "WHERE r.status IN ('Pending', 'Approved') " +
                "GROUP BY f.fruit_name, f.fruit_id " +
                "HAVING SUM(r.quantity) > 0 " +
                "ORDER BY f.fruit_name";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {

                AggregatedNeedBean need = new AggregatedNeedBean();
                need.setFruitId(rs.getInt("fruit_id"));
                need.setFruitName(rs.getString("fruit_name"));
                need.setTotalNeededQuantity(rs.getInt("total_needed_quantity"));

                needs.add(need);
            }
            LOGGER.log(Level.INFO, "Fetched {0} total aggregated needs rows.", needs.size());
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching all aggregated needs", e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return needs;
    }

    public List<SeasonalConsumptionBean> getAllSeasonalConsumption() {
        List<SeasonalConsumptionBean> consumption = new ArrayList<>();

        String sql = "SELECT " +
                "  CASE " +
                "    WHEN MONTH(r.reservation_date) IN (3, 4, 5) THEN 'Spring' " +
                "    WHEN MONTH(r.reservation_date) IN (6, 7, 8) THEN 'Summer' " +
                "    WHEN MONTH(r.reservation_date) IN (9, 10, 11) THEN 'Autumn' " +
                "    ELSE 'Winter' " +
                "  END AS season, " +
                "  f.fruit_name, " +
                "  SUM(r.quantity) as total_consumed " +
                "FROM reservations r " +
                "JOIN fruits f ON r.fruit_id = f.fruit_id " +
                "WHERE r.status = 'Fulfilled' " +
                "GROUP BY season, f.fruit_name " +
                "ORDER BY FIELD(season, 'Spring', 'Summer', 'Autumn', 'Winter'), f.fruit_name";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                SeasonalConsumptionBean item = new SeasonalConsumptionBean();
                item.setSeason(rs.getString("season"));
                item.setFruitName(rs.getString("fruit_name"));
                item.setTotalConsumedQuantity(rs.getLong("total_consumed"));
                consumption.add(item);
            }
            LOGGER.log(Level.INFO, "Fetched {0} total seasonal consumption rows.", consumption.size());
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching all seasonal consumption", e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return consumption;
    }

}