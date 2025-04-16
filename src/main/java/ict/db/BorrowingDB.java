package ict.db;

import ict.bean.BorrowingBean;
// Updated import to use BakeryShopBean
import ict.bean.BakeryShopBean;
import ict.bean.InventoryBean; // Optional, but useful

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles database operations related to borrowing fruits between shops,
 * including finding lenders and managing inventory updates within transactions.
 */
public class BorrowingDB {

    private static final Logger LOGGER = Logger.getLogger(BorrowingDB.class.getName());
    private String dburl, username, password;
    // Updated to use BakeryShopDB
    // Assumes BakeryShopDB will have a getShopById(int shopId) method added.
    private BakeryShopDB bakeryShopDb; // Dependency needed to get shop city

    // Constructor
    public BorrowingDB(String dburl, String dbUser, String dbPassword) {
        this.dburl = dburl;
        this.username = dbUser;
        this.password = dbPassword;
        // Initialize dependencies - Updated to use BakeryShopDB
        this.bakeryShopDb = new BakeryShopDB(dburl, dbUser, dbPassword);
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

    /**
     * Finds potential shops in the same city that have enough inventory of a specific fruit.
     *
     * @param fruitId          The ID of the fruit needed.
     * @param requiredQuantity The minimum quantity required.
     * @param city             The city where the borrowing and lending shops must be located.
     * @param requestingShopId The ID of the shop requesting the borrowing (to exclude itself).
     * @return A List of Maps, where each map contains "shopId", "shopName", and "availableQuantity".
     */
    public List<Map<String, Object>> findPotentialLenders(int fruitId, int requiredQuantity, String city, int requestingShopId) {
        List<Map<String, Object>> lenders = new ArrayList<>();
        // SQL to find shops in the same city (excluding requester) with enough inventory
        // Assumes the table name is 'shops' as per the schema provided earlier
        String sql = "SELECT s.shop_id, s.shop_name, i.quantity " +
                     "FROM shops s " +
                     "JOIN inventory i ON s.shop_id = i.shop_id " +
                     "WHERE s.city = ? " +
                     "AND s.shop_id != ? " + // Exclude the requesting shop
                     "AND i.fruit_id = ? " +
                     "AND i.quantity >= ?";

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
             LOGGER.log(Level.INFO, "Found {0} potential lenders for fruit ID {1} in city {2}", new Object[]{lenders.size(), fruitId, city});

        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error finding potential lenders", e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return lenders;
    }

    /**
     * Creates a borrowing record and updates inventory for both shops within a transaction.
     *
     * @param fruitId         The ID of the fruit being borrowed.
     * @param lendingShopId   The ID of the shop lending the fruit.
     * @param borrowingShopId The ID of the shop receiving the fruit.
     * @param quantity        The quantity being borrowed.
     * @return A status message indicating success or failure reason.
     */
    public String createBorrowing(int fruitId, int lendingShopId, int borrowingShopId, int quantity) {
        Connection conn = null;
        String statusMessage = "Borrowing failed: Unknown error.";

        if (quantity <= 0) {
            return "Borrowing failed: Quantity must be positive.";
        }
        if (lendingShopId == borrowingShopId) {
             return "Borrowing failed: Cannot borrow from yourself.";
        }

        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Start transaction

            // 1. Re-check inventory of the lending shop within the transaction
            int currentQuantity = getInventoryQuantityForShop(fruitId, lendingShopId, conn);
            if (currentQuantity < quantity) {
                conn.rollback();
                return "Borrowing failed: Lending shop stock changed. Available: " + currentQuantity;
            }
             LOGGER.log(Level.INFO, "[TX] Inventory check passed for lender ShopID={0}, FruitID={1}, Need={2}, Have={3}",
                       new Object[]{lendingShopId, fruitId, quantity, currentQuantity});


            // 2. Add the borrowing record
            boolean borrowingAdded = addBorrowingRecord(fruitId, lendingShopId, borrowingShopId, quantity, conn);
            if (!borrowingAdded) {
                conn.rollback();
                return "Borrowing failed: Could not create borrowing record.";
            }
             LOGGER.log(Level.INFO, "[TX] Borrowing record added.");


            // 3. Decrease inventory for the lending shop
            boolean lenderInventoryUpdated = updateShopInventory(fruitId, lendingShopId, -quantity, conn);
            if (!lenderInventoryUpdated) {
                conn.rollback();
                // Provide a more specific error if possible
                return "Borrowing failed: Could not update lending shop's inventory (possible stock issue or missing record).";
            }
             LOGGER.log(Level.INFO, "[TX] Lender inventory updated (decreased).");


            // 4. Increase inventory for the borrowing shop
            // This handles inserting a new inventory record if one doesn't exist
            boolean borrowerInventoryUpdated = addOrUpdateShopInventory(fruitId, borrowingShopId, quantity, conn);
            if (!borrowerInventoryUpdated) {
                conn.rollback();
                return "Borrowing failed: Could not update borrowing shop's inventory.";
            }
            LOGGER.log(Level.INFO, "[TX] Borrower inventory updated (increased).");


            // If all steps succeeded, commit the transaction
            conn.commit();
            statusMessage = "Borrowing request created successfully!";
             LOGGER.log(Level.INFO, "[TX] Transaction committed successfully.");


        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error during borrowing transaction", e);
            statusMessage = "Borrowing failed: Database error occurred (" + e.getMessage() + ")";
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


    // --- Transaction Helper Methods ---

    private int getInventoryQuantityForShop(int fruitId, int shopId, Connection conn) throws SQLException {
        int quantity = 0; // Default to 0 if no record found
        String sql = "SELECT quantity FROM inventory WHERE fruit_id = ? AND shop_id = ?";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, fruitId);
            ps.setInt(2, shopId);
            rs = ps.executeQuery();
            if (rs.next()) {
                quantity = rs.getInt("quantity");
            }
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
        return quantity;
    }

    private boolean addBorrowingRecord(int fruitId, int lendingShopId, int borrowingShopId, int quantity, Connection conn) throws SQLException {
        String sql = "INSERT INTO borrowings (fruit_id, borrowing_shop_id, receiving_shop_id, quantity, borrowing_date, status) VALUES (?, ?, ?, ?, CURDATE(), ?)";
        PreparedStatement ps = null;
        String initialStatus = "Borrowed"; // Or "Pending Approval" etc.
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, fruitId);
            ps.setInt(2, lendingShopId);
            ps.setInt(3, borrowingShopId);
            ps.setInt(4, quantity);
            ps.setString(5, initialStatus);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected >= 1;
        } finally {
            closeQuietly(ps);
        }
    }

    // Simple update, checks if decreasing quantity would go below zero
    private boolean updateShopInventory(int fruitId, int shopId, int quantityChange, Connection conn) throws SQLException {
        String sql = "UPDATE inventory SET quantity = quantity + ? WHERE fruit_id = ? AND shop_id = ?";
        // Add check to prevent negative inventory ONLY if decreasing
        if (quantityChange < 0) {
            sql += " AND quantity >= ?";
        }
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, quantityChange);
            ps.setInt(2, fruitId);
            ps.setInt(3, shopId);
            if (quantityChange < 0) {
                ps.setInt(4, -quantityChange); // Ensure current quantity >= amount to decrease
            }

            int rowsAffected = ps.executeUpdate();
             // If decreasing and no rows affected, it means either record missing or not enough stock
             if (rowsAffected == 0 && quantityChange < 0) {
                 LOGGER.log(Level.WARNING, "[TX] Update failed for decreasing inventory - possibly record missing or insufficient quantity. FruitID={0}, ShopID={1}", new Object[]{fruitId, shopId});
                 return false;
             }
            // If increasing and no rows affected, it means the record doesn't exist yet (handled by addOrUpdate)
            // If update succeeds (rowsAffected >= 1), return true
            return rowsAffected >= 1;
        } finally {
            closeQuietly(ps);
        }
    }

     // Tries to update, if no rows affected, tries to insert (handles adding inventory for borrower)
     private boolean addOrUpdateShopInventory(int fruitId, int shopId, int quantityChange, Connection conn) throws SQLException {
         // First, try to update
         boolean updated = updateShopInventory(fruitId, shopId, quantityChange, conn);

         if (updated) {
             return true; // Update successful
         } else {
             // If update affected 0 rows and we are INCREASING quantity, try inserting
             if (quantityChange > 0) {
                 LOGGER.log(Level.INFO, "[TX] No existing inventory for FruitID={0}, ShopID={1}. Inserting new record.", new Object[]{fruitId, shopId});
                 String insertSql = "INSERT INTO inventory (fruit_id, shop_id, quantity) VALUES (?, ?, ?)";
                 PreparedStatement psInsert = null;
                 try {
                     psInsert = conn.prepareStatement(insertSql);
                     psInsert.setInt(1, fruitId);
                     psInsert.setInt(2, shopId);
                     psInsert.setInt(3, quantityChange); // Initial quantity
                     int rowsAffected = psInsert.executeUpdate();
                     return rowsAffected >= 1; // Return true if insert succeeded
                 } finally {
                     closeQuietly(psInsert);
                 }
             } else {
                  // If update failed and we were trying to decrease, it means record didn't exist or had insufficient quantity (already logged in updateShopInventory)
                  LOGGER.log(Level.WARNING, "[TX] addOrUpdateShopInventory failed to decrease inventory for FruitID={0}, ShopID={1}", new Object[]{fruitId, shopId});
                  return false;
             }
         }
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

     // Method to list borrowings (unchanged, but uses BakeryShopBean implicitly via joins if needed)
     public List<BorrowingBean> getAllBorrowingsForShop(int shopId) {
         List<BorrowingBean> borrowings = new ArrayList<>();
         String sql = "SELECT b.*, f.fruit_name, bs.shop_name as borrowing_shop_name, rs.shop_name as receiving_shop_name " +
                      "FROM borrowings b " +
                      "JOIN fruits f ON b.fruit_id = f.fruit_id " +
                      "JOIN shops bs ON b.borrowing_shop_id = bs.shop_id " + // Shop lending
                      "JOIN shops rs ON b.receiving_shop_id = rs.shop_id " + // Shop receiving
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
         } catch (SQLException | IOException e) {
             LOGGER.log(Level.SEVERE, "Error fetching borrowings for shop " + shopId, e);
         } finally {
             closeQuietly(rs);
             closeQuietly(ps);
             closeQuietly(conn);
         }
         return borrowings;
     }
     
}
