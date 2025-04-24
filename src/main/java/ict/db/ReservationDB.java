package ict.db;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import ict.bean.AggregatedNeedBean;
import ict.bean.ConsumptionDataBean;
import ict.bean.ForecastBean;
import ict.bean.ReservationBean;

public class ReservationDB {

    private static final Logger LOGGER = Logger.getLogger(ReservationDB.class.getName());
    private String dburl, username, password;
    private FruitDB fruitDb;

    public ReservationDB(String dburl, String dbUser, String dbPassword) {
        this.dburl = dburl;
        this.username = dbUser;
        this.password = dbPassword;

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

    public List<ReservationBean> getAllReservations() {
        List<ReservationBean> reservations = new ArrayList<>();

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
                bean.setFruitName(rs.getString("fruit_name"));
                bean.setShopName(rs.getString("shop_name"));
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

    public List<ReservationBean> getReservationsForShop(int shopId) {
        List<ReservationBean> reservations = new ArrayList<>();

        String sql = "SELECT r.*, f.fruit_name "
                + "FROM reservations r "
                + "JOIN fruits f ON r.fruit_id = f.fruit_id "
                + "WHERE r.shop_id = ? "
                + "ORDER BY r.reservation_date DESC, r.reservation_id DESC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, shopId);
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

                reservations.add(bean);
            }
            LOGGER.log(Level.INFO, "Fetched {0} reservations for ShopID={1}",
                    new Object[] { reservations.size(), shopId });
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching reservations for shop " + shopId, e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return reservations;
    }

    public List<AggregatedNeedBean> getAggregatedNeedsByCountry(String sourceCountry) {
        List<AggregatedNeedBean> needs = new ArrayList<>();

        String sql = "SELECT f.source_country, r.fruit_id, f.fruit_name, SUM(r.quantity) AS total_needed_quantity "
                + "FROM reservations r "
                + "JOIN fruits f ON r.fruit_id = f.fruit_id "
                + "WHERE r.status = 'Pending' AND f.source_country = ? "
                +
                "GROUP BY f.source_country, r.fruit_id, f.fruit_name "
                + "HAVING SUM(r.quantity) > 0 "
                +
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
            LOGGER.log(Level.INFO, "Fetched {0} aggregated needs for country: {1}",
                    new Object[] { needs.size(), sourceCountry });

        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching aggregated needs for country " + sourceCountry, e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return needs;
    }

    public boolean approveReservationsForFruit(int fruitId, String sourceCountry, String newStatus) {
        Connection conn = null;
        PreparedStatement psUpdate = null;
        boolean success = false;
        int rowsAffected = 0;

        String sql = "UPDATE reservations r "
                + "JOIN fruits f ON r.fruit_id = f.fruit_id "
                + "SET r.status = ? "
                + "WHERE r.fruit_id = ? AND f.source_country = ? AND r.status = 'Pending'";

        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            psUpdate = conn.prepareStatement(sql);
            psUpdate.setString(1, newStatus);
            psUpdate.setInt(2, fruitId);
            psUpdate.setString(3, sourceCountry);

            rowsAffected = psUpdate.executeUpdate();

            if (rowsAffected > 0) {
                conn.commit();
                success = true;
                LOGGER.log(Level.INFO,
                        "Successfully approved {0} reservations for FruitID={1}, Country={2}. Status set to {3}",
                        new Object[] { rowsAffected, fruitId, sourceCountry, newStatus });
            } else {

                conn.rollback();
                success = false;
                LOGGER.log(Level.WARNING, "No pending reservations found to approve for FruitID={0}, Country={1}.",
                        new Object[] { fruitId, sourceCountry });
            }

        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE,
                    "Error approving reservations for FruitID=" + fruitId + ", Country=" + sourceCountry, e);
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

        public void setFruitId(int fruitId) {
            this.fruitId = fruitId;
        }

        public String getFruitName() {
            return fruitName;
        }

        public void setFruitName(String fruitName) {
            this.fruitName = fruitName;
        }

        public String getTargetCountry() {
            return targetCountry;
        }

        public void setTargetCountry(String targetCountry) {
            this.targetCountry = targetCountry;
        }

        public int getTotalApprovedQuantity() {
            return totalApprovedQuantity;
        }

        public void setTotalApprovedQuantity(int totalApprovedQuantity) {
            this.totalApprovedQuantity = totalApprovedQuantity;
        }
    }

    public List<DeliveryNeedBean> getApprovedNeedsGroupedByFruitAndCountry(int sourceWarehouseId) {
        List<DeliveryNeedBean> needs = new ArrayList<>();

        String sql = "SELECT "
                + "  r.fruit_id, "
                + "  f.fruit_name, "
                + "  s.country AS target_country, "
                + "  SUM(r.quantity) AS total_approved_quantity "
                + "FROM reservations r "
                + "JOIN fruits f ON r.fruit_id = f.fruit_id "
                + "JOIN shops s ON r.shop_id = s.shop_id "
                + "JOIN warehouses w_source ON f.source_country = w_source.country "
                +
                "WHERE r.status = 'Approved' "
                + "  AND w_source.warehouse_id = ? AND w_source.is_source = 1 "
                +

                "GROUP BY r.fruit_id, f.fruit_name, s.country "
                + "HAVING SUM(r.quantity) > 0 "
                + "ORDER BY s.country, f.fruit_name";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, sourceWarehouseId);
            rs = ps.executeQuery();

            while (rs.next()) {
                DeliveryNeedBean need = new DeliveryNeedBean();
                need.setFruitId(rs.getInt("fruit_id"));
                need.setFruitName(rs.getString("fruit_name"));
                need.setTargetCountry(rs.getString("target_country"));
                need.setTotalApprovedQuantity(rs.getInt("total_approved_quantity"));
                needs.add(need);
            }
            LOGGER.log(Level.INFO, "Fetched {0} grouped approved needs originating from WarehouseID={1}",
                    new Object[] { needs.size(), sourceWarehouseId });

        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching grouped approved needs for WarehouseID=" + sourceWarehouseId, e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return needs;
    }

    private boolean addDeliveryRecord(int fruitId, int fromWarehouseId, int toWarehouseId, int quantity,
            Connection conn) throws SQLException {
        String sql = "INSERT INTO deliveries (fruit_id, from_warehouse_id, to_warehouse_id, quantity, delivery_date, status) "
                + "VALUES (?, ?, ?, ?, CURDATE(), ?)";
        PreparedStatement ps = null;
        String initialStatus = "Scheduled";
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, fruitId);
            ps.setInt(2, fromWarehouseId);
            ps.setInt(3, toWarehouseId);
            ps.setInt(4, quantity);
            ps.setString(5, initialStatus);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected >= 1;
        } finally {
            closeQuietly(ps);
        }
    }

    private int updateReservationStatusForDelivery(int fruitId, String targetCountry, String newStatus, Connection conn)
            throws SQLException {
        String sql = "UPDATE reservations r "
                + "JOIN shops s ON r.shop_id = s.shop_id "
                + "SET r.status = ? "
                + "WHERE r.fruit_id = ? AND s.country = ? AND r.status = 'Approved'";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setString(1, newStatus);
            ps.setInt(2, fruitId);
            ps.setString(3, targetCountry);
            int rowsAffected = ps.executeUpdate();
            LOGGER.log(Level.INFO, "[TX] Updated status to {0} for {1} reservations of FruitID={2} destined for {3}",
                    new Object[] { newStatus, rowsAffected, fruitId, targetCountry });
            return rowsAffected;
        } finally {
            closeQuietly(ps);
        }
    }

    public String arrangeDeliveryTransaction(int fruitId, int fromWarehouseId, String targetCountry) {
        Connection conn = null;
        String statusMessage = "Delivery arrangement failed: Unknown error.";
        int quantityToDeliver = 0;

        WarehouseDB localWarehouseDb = new WarehouseDB(this.dburl, this.username, this.password);

        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            String quantitySql = "SELECT SUM(r.quantity) AS total_approved "
                    + "FROM reservations r JOIN shops s ON r.shop_id = s.shop_id "
                    + "WHERE r.fruit_id = ? AND s.country = ? AND r.status = 'Approved'";
            PreparedStatement psQty = conn.prepareStatement(quantitySql);
            psQty.setInt(1, fruitId);
            psQty.setString(2, targetCountry);
            ResultSet rsQty = psQty.executeQuery();
            if (rsQty.next()) {
                quantityToDeliver = rsQty.getInt("total_approved");
            }
            closeQuietly(rsQty);
            closeQuietly(psQty);

            if (quantityToDeliver <= 0) {
                conn.rollback();
                return "Delivery arrangement failed: No 'Approved' reservations found for this fruit and target country.";
            }
            LOGGER.log(Level.INFO, "[TX] Calculated quantity to deliver for FruitID={0}, TargetCountry={1}: {2}",
                    new Object[] { fruitId, targetCountry, quantityToDeliver });

            int toWarehouseId = localWarehouseDb.findCentralWarehouseInCountry(targetCountry);
            if (toWarehouseId == -1) {
                conn.rollback();
                return "Delivery arrangement failed: Could not find a central warehouse in " + targetCountry + ".";
            }
            LOGGER.log(Level.INFO, "[TX] Found target central WarehouseID={0} for Country={1}",
                    new Object[] { toWarehouseId, targetCountry });

            int currentSourceQuantity = getInventoryQuantityForWarehouse(fruitId, fromWarehouseId, conn);
            if (currentSourceQuantity < quantityToDeliver) {
                conn.rollback();
                return "Delivery arrangement failed: Insufficient stock (" + currentSourceQuantity
                        + ") at source warehouse " + fromWarehouseId + " for required quantity (" + quantityToDeliver
                        + ").";
            }
            LOGGER.log(Level.INFO,
                    "[TX] Source inventory check passed for WarehouseID={0}, FruitID={1}. Have: {2}, Need: {3}",
                    new Object[] { fromWarehouseId, fruitId, currentSourceQuantity, quantityToDeliver });

            boolean deliveryAdded = addDeliveryRecord(fruitId, fromWarehouseId, toWarehouseId, quantityToDeliver, conn);
            if (!deliveryAdded) {
                conn.rollback();
                return "Delivery arrangement failed: Could not create delivery record.";
            }
            LOGGER.log(Level.INFO, "[TX] Delivery record created.");

            boolean inventoryDecreased = updateWarehouseInventory(fruitId, fromWarehouseId, -quantityToDeliver, conn);

            if (!inventoryDecreased) {
                conn.rollback();
                return "Delivery arrangement failed: Could not update source warehouse inventory.";
            }
            LOGGER.log(Level.INFO, "[TX] Source inventory decreased.");

            String newReservationStatus = "Shipped";
            int reservationsUpdated = updateReservationStatusForDelivery(fruitId, targetCountry, newReservationStatus,
                    conn);

            if (reservationsUpdated == 0) {
                LOGGER.log(Level.WARNING,
                        "[TX] No reservations were updated to '{0}' status for FruitID={1}, TargetCountry={2}. This might indicate an issue.",
                        new Object[] { newReservationStatus, fruitId, targetCountry });
            } else {
                LOGGER.log(Level.INFO, "[TX] {0} reservation statuses updated to {1}.",
                        new Object[] { reservationsUpdated, newReservationStatus });
            }

            conn.commit();
            statusMessage = "Delivery arranged successfully for " + quantityToDeliver + " units of Fruit ID " + fruitId
                    + " to " + targetCountry + "!";
            LOGGER.log(Level.INFO, "[TX] Delivery transaction committed successfully.");

        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error during delivery arrangement transaction", e);
            statusMessage = "Delivery arrangement failed: Database error occurred (" + e.getMessage() + ")";
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
            if (rs.next()) {
                quantity = rs.getInt("quantity");
            } else {
                LOGGER.log(Level.WARNING, "[TX] No inventory record found for FruitID={0}, WarehouseID={1}",
                        new Object[] { fruitId, warehouseId });

            }
        } finally {

            closeQuietly(rs);
            closeQuietly(ps);
        }
        return quantity;
    }

    public List<ConsumptionDataBean> getConsumptionSummaryByFruit(Date startDate, Date endDate) {
        List<ConsumptionDataBean> reportData = new ArrayList<>();

        String sql = "SELECT f.fruit_name, SUM(r.quantity) as total_consumed "
                + "FROM reservations r "
                + "JOIN fruits f ON r.fruit_id = f.fruit_id "
                + "WHERE r.status = 'Fulfilled' AND r.reservation_date BETWEEN ? AND ? "
                + "GROUP BY f.fruit_name "
                + "ORDER BY total_consumed DESC";

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

    private boolean updateWarehouseInventory(int fruitId, int warehouseId, int quantityChange, Connection conn)
            throws SQLException {

        String sql = "UPDATE inventory SET quantity = quantity + ? WHERE fruit_id = ? AND warehouse_id = ? AND shop_id IS NULL";

        if (quantityChange < 0) {
            sql += " AND quantity >= ?";
        }
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, quantityChange);
            ps.setInt(2, fruitId);
            ps.setInt(3, warehouseId);
            if (quantityChange < 0) {
                ps.setInt(4, -quantityChange);
            }

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0 && quantityChange < 0) {
                LOGGER.log(Level.WARNING,
                        "[TX] Update failed for decreasing warehouse inventory - possibly record missing or insufficient quantity. FruitID={0}, WarehouseID={1}",
                        new Object[] { fruitId, warehouseId });
                return false;
            }
            if (rowsAffected == 0 && quantityChange > 0) {

                LOGGER.log(Level.WARNING,
                        "[TX] Update failed for increasing warehouse inventory - record missing? FruitID={0}, WarehouseID={1}",
                        new Object[] { fruitId, warehouseId });
                return false;
            }

            return rowsAffected > 0;
        } finally {

            closeQuietly(ps);
        }
    }

    public List<AggregatedNeedBean> getAggregatedNeeds(String filterType, String filterValue, Date startDate,
            Date endDate) {
        List<AggregatedNeedBean> needs = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder(
                "SELECT f.fruit_name, f.fruit_id, SUM(r.quantity) AS total_needed_quantity "
                        + "FROM reservations r "
                        + "JOIN fruits f ON r.fruit_id = f.fruit_id ");
        List<Object> params = new ArrayList<>();

        if ("shop".equals(filterType) || "city".equals(filterType) || "country".equals(filterType)) {
            sqlBuilder.append("JOIN shops s ON r.shop_id = s.shop_id ");
            if ("shop".equals(filterType)) {
                sqlBuilder.append("WHERE r.shop_id = ? ");
                try {
                    params.add(Integer.parseInt(filterValue));
                } catch (NumberFormatException e) {
                    return needs;
                }
            } else if ("city".equals(filterType)) {
                sqlBuilder.append("WHERE s.city = ? ");
                params.add(filterValue);
            } else {
                sqlBuilder.append("WHERE s.country = ? ");
                params.add(filterValue);
            }
        } else {

            sqlBuilder.append("WHERE 1=1 ");
        }

        sqlBuilder.append("AND r.status IN ('Pending', 'Approved') ");
        sqlBuilder.append("AND r.reservation_date BETWEEN ? AND ? ");
        params.add(startDate);
        params.add(endDate);

        sqlBuilder.append("GROUP BY f.fruit_name, f.fruit_id ");
        sqlBuilder.append("ORDER BY f.fruit_name");

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sqlBuilder.toString());

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            rs = ps.executeQuery();
            while (rs.next()) {
                AggregatedNeedBean need = new AggregatedNeedBean();
                need.setFruitId(rs.getInt("fruit_id"));
                need.setFruitName(rs.getString("fruit_name"));
                need.setTotalNeededQuantity(rs.getInt("total_needed_quantity"));

                needs.add(need);
            }
            LOGGER.log(Level.INFO, "Fetched {0} aggregated needs for filter [{1}={2}]",
                    new Object[] { needs.size(), filterType, filterValue });
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching aggregated needs", e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return needs;
    }

    public static class SeasonalConsumptionBean implements Serializable {

        private String season;
        private String fruitName;
        private long totalConsumedQuantity;

        public String getSeason() {
            return season;
        }

        public void setSeason(String s) {
            this.season = s;
        }

        public String getFruitName() {
            return fruitName;
        }

        public void setFruitName(String n) {
            this.fruitName = n;
        }

        public long getTotalConsumedQuantity() {
            return totalConsumedQuantity;
        }

        public void setTotalConsumedQuantity(long q) {
            this.totalConsumedQuantity = q;
        }
    }

    public List<SeasonalConsumptionBean> getSeasonalConsumption(String filterType, String filterValue, Date startDate,
            Date endDate) {
        List<SeasonalConsumptionBean> consumption = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder(
                "SELECT "
                        + "  CASE "

                        + "    WHEN MONTH(r.reservation_date) IN (3, 4, 5) THEN 'Spring' "
                        + "    WHEN MONTH(r.reservation_date) IN (6, 7, 8) THEN 'Summer' "
                        + "    WHEN MONTH(r.reservation_date) IN (9, 10, 11) THEN 'Autumn' "
                        + "    ELSE 'Winter' "

                        + "  END AS season, "
                        + "  f.fruit_name, "
                        + "  SUM(r.quantity) as total_consumed "
                        + "FROM reservations r "
                        + "JOIN fruits f ON r.fruit_id = f.fruit_id ");
        List<Object> params = new ArrayList<>();

        if ("shop".equals(filterType) || "city".equals(filterType) || "country".equals(filterType)) {
            sqlBuilder.append("JOIN shops s ON r.shop_id = s.shop_id ");
            if ("shop".equals(filterType)) {
                sqlBuilder.append("WHERE s.shop_id = ? ");
                try {
                    params.add(Integer.parseInt(filterValue));
                } catch (NumberFormatException e) {
                    return consumption;
                }
            } else if ("city".equals(filterType)) {
                sqlBuilder.append("WHERE s.city = ? ");
                params.add(filterValue);
            } else {
                sqlBuilder.append("WHERE s.country = ? ");
                params.add(filterValue);
            }
        } else {
            sqlBuilder.append("WHERE 1=1 ");
        }

        sqlBuilder.append("AND r.status = 'Fulfilled' ");
        sqlBuilder.append("AND r.reservation_date BETWEEN ? AND ? ");
        params.add(startDate);
        params.add(endDate);

        sqlBuilder.append("GROUP BY season, f.fruit_name ");
        sqlBuilder.append("ORDER BY FIELD(season, 'Spring', 'Summer', 'Autumn', 'Winter'), f.fruit_name");

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sqlBuilder.toString());

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            rs = ps.executeQuery();
            while (rs.next()) {
                SeasonalConsumptionBean item = new SeasonalConsumptionBean();
                item.setSeason(rs.getString("season"));
                item.setFruitName(rs.getString("fruit_name"));
                item.setTotalConsumedQuantity(rs.getLong("total_consumed"));
                consumption.add(item);
            }
            LOGGER.log(Level.INFO, "Fetched {0} seasonal consumption rows for filter [{1}={2}]",
                    new Object[] { consumption.size(), filterType, filterValue });
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching seasonal consumption", e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return consumption;
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

        String sql = "SELECT "
                + "  s.country AS target_country, "
                + "  r.fruit_id, "
                + "  f.fruit_name, "
                + "  SUM(r.quantity) AS total_consumed "
                + "FROM reservations r "
                + "JOIN fruits f ON r.fruit_id = f.fruit_id "
                + "JOIN shops s ON r.shop_id = s.shop_id "
                + "WHERE r.status = 'Fulfilled' "
                + "  AND r.reservation_date BETWEEN ? AND ? "

                + "GROUP BY s.country, r.fruit_id, f.fruit_name "
                + "ORDER BY s.country, f.fruit_name";

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

                BigDecimal avgDaily = new BigDecimal(totalConsumed)
                        .divide(new BigDecimal(periodDays), 2, java.math.RoundingMode.HALF_UP);

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

}