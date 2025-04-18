package ict.db;

import ict.bean.AggregatedNeedBean;
import ict.bean.FruitBean; // Assuming FruitDB is available
import ict.bean.ReservationBean;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles database operations related to reservations, including checking
 * source inventory.
 */
public class ReservationDB {

    private static final Logger LOGGER = Logger.getLogger(ReservationDB.class.getName());
    private String dburl, username, password;
    private FruitDB fruitDb; // Dependency to get fruit details

    // Constructor
    public ReservationDB(String dburl, String dbUser, String dbPassword) {
        this.dburl = dburl;
        this.username = dbUser;
        this.password = dbPassword;
        // Initialize dependencies - ensure FruitDB is also initialized elsewhere or pass it in
        this.fruitDb = new FruitDB(dburl, dbUser, dbPassword);
    }

    /**
     * Establishes a database connection.
     */
    public Connection getConnection() throws SQLException, IOException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "MySQL JDBC Driver not found.", e);
            throw new IOException("Database driver not found.", e);
        }
        return DriverManager.getConnection(dburl, username, password);
    }

    /**
     * Finds the warehouse ID marked as the source for a given fruit's source
     * country.
     *
     * @param fruitId The ID of the fruit.
     * @param conn An existing database connection.
     * @return The source warehouse ID, or -1 if not found.
     * @throws SQLException if a database access error occurs.
     */
    private int getSourceWarehouseId(int fruitId, Connection conn) throws SQLException {
        int sourceWarehouseId = -1;
        // First, get the source country of the fruit
        FruitBean fruit = fruitDb.getFruitById(fruitId); // Assumes FruitDB has getFruitById
        if (fruit == null || fruit.getSourceCountry() == null || fruit.getSourceCountry().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Fruit not found or source country missing for fruit ID: {0}", fruitId);
            return -1; // Cannot proceed without source country
        }
        String sourceCountry = fruit.getSourceCountry();
        LOGGER.log(Level.INFO, "Finding source warehouse for fruit ID {0} from country: {1}", new Object[]{fruitId, sourceCountry});

        // Now find the warehouse in that country marked as source
        // Consider city as well if needed: AND city = ?
        String sql = "SELECT warehouse_id FROM warehouses WHERE country = ? AND is_source = 1 LIMIT 1";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setString(1, sourceCountry);
            rs = ps.executeQuery();
            if (rs.next()) {
                sourceWarehouseId = rs.getInt("warehouse_id");
                LOGGER.log(Level.INFO, "Source warehouse found: ID={0}", sourceWarehouseId);
            } else {
                LOGGER.log(Level.WARNING, "No source warehouse found for country: {0}", sourceCountry);
            }
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            // Do not close the connection here, it's managed by the calling method
        }
        return sourceWarehouseId;
    }

    /**
     * Gets the current inventory quantity for a specific fruit in a specific
     * warehouse.
     *
     * @param fruitId The ID of the fruit.
     * @param warehouseId The ID of the warehouse.
     * @param conn An existing database connection.
     * @return The current quantity, or -1 if inventory record not found.
     * @throws SQLException if a database access error occurs.
     */
    private int getInventoryQuantity(int fruitId, int warehouseId, Connection conn) throws SQLException {
        int quantity = -1;
        String sql = "SELECT quantity FROM inventory WHERE fruit_id = ? AND warehouse_id = ?";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, fruitId);
            ps.setInt(2, warehouseId);
            rs = ps.executeQuery();
            if (rs.next()) {
                quantity = rs.getInt("quantity");
                LOGGER.log(Level.INFO, "Inventory check: FruitID={0}, WarehouseID={1}, Quantity={2}", new Object[]{fruitId, warehouseId, quantity});
            } else {
                LOGGER.log(Level.WARNING, "No inventory record found for FruitID={0}, WarehouseID={1}", new Object[]{fruitId, warehouseId});
                // Treat missing inventory as 0 available quantity
                quantity = 0;
            }
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            // Do not close the connection here
        }
        return quantity;
    }

    /**
     * Adds a reservation record to the database.
     *
     * @param fruitId The ID of the fruit.
     * @param shopId The ID of the reserving shop.
     * @param quantity The quantity being reserved.
     * @param conn An existing database connection (part of a transaction).
     * @return true if insertion was successful, false otherwise.
     * @throws SQLException if a database access error occurs.
     */
    private boolean addReservationRecord(int fruitId, int shopId, int quantity, Connection conn) throws SQLException {
        String sql = "INSERT INTO reservations (fruit_id, shop_id, quantity, reservation_date, status) VALUES (?, ?, ?, CURDATE(), ?)";
        PreparedStatement ps = null;
        String initialStatus = "Pending"; // Or "Confirmed" depending on logic
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, fruitId);
            ps.setInt(2, shopId);
            ps.setInt(3, quantity);
            ps.setString(4, initialStatus);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected >= 1;
        } finally {
            closeQuietly(ps);
            // Do not close the connection here
        }
    }

    /**
     * Updates the inventory quantity for a specific fruit in a specific
     * warehouse. Assumes the inventory record exists.
     *
     * @param fruitId The ID of the fruit.
     * @param warehouseId The ID of the warehouse.
     * @param quantityChange The amount to change the quantity by (e.g.,
     * negative for reservation).
     * @param conn An existing database connection (part of a transaction).
     * @return true if update was successful, false otherwise.
     * @throws SQLException if a database access error occurs.
     */
    private boolean updateInventoryRecord(int fruitId, int warehouseId, int quantityChange, Connection conn) throws SQLException {
        // Ensure we don't go below zero with the update
        String sql = "UPDATE inventory SET quantity = quantity + ? WHERE fruit_id = ? AND warehouse_id = ? AND quantity >= ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, quantityChange); // quantityChange will be negative for reservation
            ps.setInt(2, fruitId);
            ps.setInt(3, warehouseId);
            ps.setInt(4, -quantityChange); // Ensure current quantity is >= amount to be deducted
            int rowsAffected = ps.executeUpdate();
            return rowsAffected >= 1;
        } finally {
            closeQuietly(ps);
            // Do not close the connection here
        }
    }

    /**
     * Creates a reservation for a fruit from its source city/warehouse,
     * checking inventory and performing operations within a transaction.
     *
     * @param fruitId The ID of the fruit to reserve.
     * @param shopId The ID of the shop making the reservation.
     * @param quantity The quantity to reserve.
     * @return A status message indicating success or the reason for failure.
     */
    public String createReservationFromSource(int fruitId, int shopId, int quantity) {
        Connection conn = null;
        String statusMessage = "Reservation failed: Unknown error.";

        if (quantity <= 0) {
            return "Reservation failed: Quantity must be positive.";
        }

        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Start transaction

            // 1. Find the source warehouse for the fruit
            int sourceWarehouseId = getSourceWarehouseId(fruitId, conn);
            if (sourceWarehouseId == -1) {
                conn.rollback(); // Rollback transaction
                return "Reservation failed: Could not find a source warehouse for this fruit.";
            }

            // 2. Check current inventory at the source warehouse
            int currentQuantity = getInventoryQuantity(fruitId, sourceWarehouseId, conn);
            if (currentQuantity < quantity) {
                conn.rollback(); // Rollback transaction
                return "Reservation failed: Insufficient stock available at the source warehouse. Available: " + currentQuantity;
            }

            // 3. Add the reservation record
            boolean reservationAdded = addReservationRecord(fruitId, shopId, quantity, conn);
            if (!reservationAdded) {
                conn.rollback();
                return "Reservation failed: Could not create reservation record.";
            }
            LOGGER.log(Level.INFO, "Reservation record added for FruitID={0}, ShopID={1}, Qty={2}", new Object[]{fruitId, shopId, quantity});

            // 4. Update (decrease) the inventory at the source warehouse
            boolean inventoryUpdated = updateInventoryRecord(fruitId, sourceWarehouseId, -quantity, conn); // Decrease quantity
            if (!inventoryUpdated) {
                // This could happen if stock changed between check and update, or if record vanished.
                conn.rollback();
                return "Reservation failed: Could not update inventory. Stock might have changed.";
            }
            LOGGER.log(Level.INFO, "Inventory updated for FruitID={0}, WarehouseID={1}, Change={2}", new Object[]{fruitId, sourceWarehouseId, -quantity});

            // If all steps succeeded, commit the transaction
            conn.commit();
            statusMessage = "Reservation created successfully!";
            LOGGER.log(Level.INFO, "Transaction committed for reservation.");

        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error during reservation transaction for fruit ID " + fruitId, e);
            statusMessage = "Reservation failed: Database error occurred.";
            if (conn != null) {
                try {
                    LOGGER.log(Level.WARNING, "Rolling back transaction due to error.");
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Failed to rollback transaction", ex);
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Restore default commit behavior
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Failed to close connection", e);
                }
            }
        }
        return statusMessage;
    }

    // --- Helper Method for closing resources ---
    private void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                // Log or ignore
                LOGGER.log(Level.WARNING, "Failed to close resource: " + resource.getClass().getSimpleName(), e);
            }
        }
    }

    // TODO: Add methods to list reservations if needed, potentially joining with fruits/shops
    public List<ReservationBean> getAllReservations() {
        List<ReservationBean> reservations = new ArrayList<>();
        // Example SQL joining tables
        String sql = "SELECT r.*, f.fruit_name, s.shop_name "
                + "FROM reservations r "
                + "JOIN fruits f ON r.fruit_id = f.fruit_id "
                + "JOIN shops s ON r.shop_id = s.shop_id "
                + "ORDER BY r.reservation_date DESC, r.reservation_id DESC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                ReservationBean bean = new ReservationBean();
                bean.setReservationId(rs.getInt("reservation_id"));
                bean.setFruitId(rs.getInt("fruit_id"));
                bean.setShopId(rs.getInt("shop_id"));
                bean.setQuantity(rs.getInt("quantity"));
                bean.setReservationDate(rs.getDate("reservation_date"));
                bean.setStatus(rs.getString("status"));
                bean.setFruitName(rs.getString("fruit_name")); // From join
                bean.setShopName(rs.getString("shop_name"));   // From join
                reservations.add(bean);
            }
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching all reservations", e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return reservations;
    }
    // Add this method inside your existing ReservationDB class

    /**
     * Retrieves all reservations made by a specific shop.
     *
     * @param shopId The ID of the shop whose reservations are to be fetched.
     * @return A List of ReservationBean objects, potentially including joined
     * data.
     */
    public List<ReservationBean> getReservationsForShop(int shopId) {
        List<ReservationBean> reservations = new ArrayList<>();
        // SQL to fetch reservations for a specific shop, joining with fruits for the name
        String sql = "SELECT r.*, f.fruit_name "
                + "FROM reservations r "
                + "JOIN fruits f ON r.fruit_id = f.fruit_id "
                + "WHERE r.shop_id = ? "
                + "ORDER BY r.reservation_date DESC, r.reservation_id DESC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection(); // Use existing method
            ps = conn.prepareStatement(sql);
            ps.setInt(1, shopId);
            rs = ps.executeQuery();
            while (rs.next()) {
                ReservationBean bean = new ReservationBean();
                bean.setReservationId(rs.getInt("reservation_id"));
                bean.setFruitId(rs.getInt("fruit_id"));
                bean.setShopId(rs.getInt("shop_id")); // Keep shopId even if filtering by it
                bean.setQuantity(rs.getInt("quantity"));
                bean.setReservationDate(rs.getDate("reservation_date"));
                bean.setStatus(rs.getString("status"));
                bean.setFruitName(rs.getString("fruit_name")); // From join
                // We don't need shop name here as we are filtering by shopId
                reservations.add(bean);
            }
            LOGGER.log(Level.INFO, "Fetched {0} reservations for ShopID={1}", new Object[]{reservations.size(), shopId});
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching reservations for shop " + shopId, e);
        } finally {
            closeQuietly(rs); // Use existing helper
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return reservations;
    }
    // Add these methods inside your existing ReservationDB class

    /**
     * Calculates the total pending reservation quantity for each fruit,
     * filtered by the fruit's source country.
     *
     * @param sourceCountry The source country to filter fruits by.
     * @return A list of AggregatedNeedBean objects.
     */
    public List<AggregatedNeedBean> getAggregatedNeedsByCountry(String sourceCountry) {
        List<AggregatedNeedBean> needs = new ArrayList<>();
        // SQL to sum pending reservations, grouped by fruit, filtered by source country
        String sql = "SELECT f.source_country, r.fruit_id, f.fruit_name, SUM(r.quantity) AS total_needed_quantity "
                + "FROM reservations r "
                + "JOIN fruits f ON r.fruit_id = f.fruit_id "
                + "WHERE r.status = 'Pending' AND f.source_country = ? "
                + // Filter by status and country
                "GROUP BY f.source_country, r.fruit_id, f.fruit_name "
                + "HAVING SUM(r.quantity) > 0 "
                + // Only show if there's a need
                "ORDER BY f.fruit_name";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, sourceCountry);
            rs = ps.executeQuery();

            while (rs.next()) {
                AggregatedNeedBean need = new AggregatedNeedBean();
                need.setSourceCountry(rs.getString("source_country"));
                need.setFruitId(rs.getInt("fruit_id"));
                need.setFruitName(rs.getString("fruit_name"));
                need.setTotalNeededQuantity(rs.getInt("total_needed_quantity"));
                needs.add(need);
            }
            LOGGER.log(Level.INFO, "Fetched {0} aggregated needs for country: {1}", new Object[]{needs.size(), sourceCountry});

        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching aggregated needs for country " + sourceCountry, e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return needs;
    }

    /**
     * Updates the status of all 'Pending' reservations for a specific fruit
     * originating from a specific source country. Runs within a transaction.
     *
     * @param fruitId The ID of the fruit whose reservations are to be approved.
     * @param sourceCountry The source country of the fruit (for verification).
     * @param newStatus The new status to set (e.g., "Approved").
     * @return true if the update was successful (at least one record updated),
     * false otherwise.
     */
    public boolean approveReservationsForFruit(int fruitId, String sourceCountry, String newStatus) {
        Connection conn = null;
        PreparedStatement psUpdate = null;
        boolean success = false;
        int rowsAffected = 0;

        // SQL to update status based on fruit_id and current status, joining with fruits to verify source country
        String sql = "UPDATE reservations r "
                + "JOIN fruits f ON r.fruit_id = f.fruit_id "
                + "SET r.status = ? "
                + "WHERE r.fruit_id = ? AND f.source_country = ? AND r.status = 'Pending'";

        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Start transaction

            psUpdate = conn.prepareStatement(sql);
            psUpdate.setString(1, newStatus);
            psUpdate.setInt(2, fruitId);
            psUpdate.setString(3, sourceCountry);

            rowsAffected = psUpdate.executeUpdate();

            if (rowsAffected > 0) {
                conn.commit();
                success = true;
                LOGGER.log(Level.INFO, "Successfully approved {0} reservations for FruitID={1}, Country={2}. Status set to {3}",
                        new Object[]{rowsAffected, fruitId, sourceCountry, newStatus});
            } else {
                // No records matched (maybe already approved, or wrong fruit/country combo)
                conn.rollback(); // Rollback, although nothing changed
                success = false; // Indicate nothing was approved
                LOGGER.log(Level.WARNING, "No pending reservations found to approve for FruitID={0}, Country={1}.",
                        new Object[]{fruitId, sourceCountry});
            }

        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error approving reservations for FruitID=" + fruitId + ", Country=" + sourceCountry, e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Rollback failed", ex);
                }
            }
            success = false;
        } finally {
            closeQuietly(psUpdate);
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Failed to close connection", e);
                }
            }
        }
        return success; // Returns true only if commit happened
    }

}
