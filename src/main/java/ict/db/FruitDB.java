package ict.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import ict.bean.FruitBean;

public class FruitDB {

    private static final Logger LOGGER = Logger.getLogger(FruitDB.class.getName());
    private String dburl, username, password;

    public FruitDB(String dburl, String dbUser, String dbPassword) {
        this.dburl = dburl;
        this.username = dbUser;
        this.password = dbPassword;
    }

    public Connection getConnection() throws SQLException, IOException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            LOGGER.log(Level.FINE, "MySQL JDBC Driver loaded.");
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "MySQL JDBC Driver not found.", e);
            throw new IOException("Database driver not found.", e);
        }
        Connection conn = DriverManager.getConnection(dburl, username, password);
        LOGGER.log(Level.FINE, "Database connection established to {0}", dburl);
        return conn;
    }

    public boolean addFruit(String fruitName, String sourceCountry) {
        Connection c = null;
        PreparedStatement ps = null;
        boolean isSuccess = false;

        String sql = "INSERT INTO fruits (fruit_name, source_country) VALUES (?, ?)";

        if (fruitName == null || fruitName.trim().isEmpty()
                || sourceCountry == null || sourceCountry.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to add fruit with empty name or source country.");
            return false;
        }

        try {
            c = getConnection();
            ps = c.prepareStatement(sql);
            ps.setString(1, fruitName.trim());
            ps.setString(2, sourceCountry.trim());

            LOGGER.log(Level.INFO, "Executing SQL: {0} with Name='{1}', Country='{2}'",
                    new Object[] { sql, fruitName.trim(), sourceCountry.trim() });
            int rowsAffected = ps.executeUpdate();

            if (rowsAffected >= 1) {
                isSuccess = true;
                LOGGER.log(Level.INFO, "Fruit '{0}' added successfully.", fruitName.trim());
            } else {
                LOGGER.log(Level.WARNING, "Fruit add operation affected 0 rows for name: {0}", fruitName.trim());
            }

        } catch (SQLException e) {

            if (e.getSQLState().startsWith("23")) {
                LOGGER.log(Level.WARNING,
                        "Failed to add fruit '{0}' due to constraint violation (likely duplicate name).",
                        fruitName.trim());
            } else {
                LOGGER.log(Level.SEVERE, "Error adding fruit: " + fruitName.trim(), e);
            }
            isSuccess = false;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "IOException during addFruit for: " + fruitName.trim(), e);
            isSuccess = false;
        } finally {
            closeQuietly(ps);
            closeQuietly(c);
        }
        return isSuccess;
    }

    public ArrayList<FruitBean> getAllFruits() {
        ArrayList<FruitBean> fruits = new ArrayList<>();

        String sql = "SELECT fruit_id, fruit_name, source_country FROM fruits ORDER BY fruit_name";
        Connection c = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            c = getConnection();
            ps = c.prepareStatement(sql);
            rs = ps.executeQuery();
            LOGGER.log(Level.INFO, "Fetching all fruits...");

            while (rs.next()) {
                FruitBean fruit = new FruitBean();
                fruit.setFruitId(rs.getInt("fruit_id"));
                fruit.setFruitName(rs.getString("fruit_name"));
                fruit.setSourceCountry(rs.getString("source_country"));
                fruits.add(fruit);
            }
            LOGGER.log(Level.INFO, "Fetched {0} fruits.", fruits.size());

        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching all fruits.", e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(c);
        }
        return fruits;
    }

    public boolean updateFruit(int fruitId, String fruitName, String sourceCountry) {
        Connection c = null;
        PreparedStatement ps = null;
        boolean isSuccess = false;

        if (fruitName == null || fruitName.trim().isEmpty()
                || sourceCountry == null || sourceCountry.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Update failed for fruit ID {0}: Name or country cannot be empty.", fruitId);
            return false;
        }

        String sql = "UPDATE fruits SET fruit_name = ?, source_country = ? WHERE fruit_id = ?";

        try {
            c = getConnection();
            ps = c.prepareStatement(sql);
            ps.setString(1, fruitName.trim());
            ps.setString(2, sourceCountry.trim());
            ps.setInt(3, fruitId);

            LOGGER.log(Level.INFO, "Executing fruit update for ID: {0}", fruitId);
            int row = ps.executeUpdate();
            if (row >= 1) {
                isSuccess = true;
                LOGGER.log(Level.INFO, "Fruit updated successfully: ID={0}", fruitId);
            } else {
                LOGGER.log(Level.WARNING, "Fruit update failed: No rows affected for ID={0}", fruitId);
            }
        } catch (SQLException | IOException e) {

            if (e instanceof SQLException && ((SQLException) e).getSQLState().startsWith("23")) {
                LOGGER.log(Level.WARNING,
                        "Failed to update fruit ID {0} due to constraint violation (likely duplicate name).", fruitId);
            } else {
                LOGGER.log(Level.SEVERE, "Error updating fruit with ID=" + fruitId, e);
            }
        } finally {
            closeQuietly(ps);
            closeQuietly(c);
        }
        return isSuccess;
    }

    public FruitBean getFruitById(int fruitId) {
        Connection c = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        FruitBean fruit = null;
        String sql = "SELECT fruit_id, fruit_name, source_country FROM fruits WHERE fruit_id=?";

        try {
            c = getConnection();
            ps = c.prepareStatement(sql);
            ps.setInt(1, fruitId);
            rs = ps.executeQuery();

            if (rs.next()) {
                fruit = new FruitBean();
                fruit.setFruitId(rs.getInt("fruit_id"));
                fruit.setFruitName(rs.getString("fruit_name"));
                fruit.setSourceCountry(rs.getString("source_country"));
                LOGGER.log(Level.INFO, "Fruit found with ID: {0}", fruitId);
            } else {
                LOGGER.log(Level.WARNING, "No fruit found with ID: {0}", fruitId);
            }
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching fruit with ID: " + fruitId, e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(c);
        }
        return fruit;
    }

    public boolean deleteFruit(int fruitId) {
        Connection c = null;
        PreparedStatement ps = null;
        boolean isSuccess = false;
        String sql = "DELETE FROM fruits WHERE fruit_id=?";

        try {
            c = getConnection();
            ps = c.prepareStatement(sql);
            ps.setInt(1, fruitId);
            LOGGER.log(Level.INFO, "Attempting to delete fruit with ID: {0}", fruitId);
            int row = ps.executeUpdate();
            if (row >= 1) {
                isSuccess = true;
                LOGGER.log(Level.INFO, "Fruit deleted successfully with ID: {0}", fruitId);
            } else {
                LOGGER.log(Level.WARNING, "Fruit deletion failed. No fruit found with ID: {0}", fruitId);
            }
        } catch (SQLException | IOException e) {

            if (e instanceof SQLException && ((SQLException) e).getSQLState().startsWith("23")) {
                LOGGER.log(Level.WARNING, "Failed to delete fruit ID {0} due to foreign key constraint.", fruitId);
            } else {
                LOGGER.log(Level.SEVERE, "Error deleting fruit with ID: " + fruitId, e);
            }
        } finally {
            closeQuietly(ps);
            closeQuietly(c);
        }
        return isSuccess;
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
}