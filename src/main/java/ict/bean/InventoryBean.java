package ict.bean;

import java.io.Serializable;

/**
 * Represents an inventory item, potentially in a shop or warehouse.
 */
public class InventoryBean implements Serializable {

    private int inventoryId;
    private int fruitId;
    private Integer shopId; // Use Integer to allow null
    private Integer warehouseId; // Use Integer to allow null
    private int quantity;

    // Additional fields for display purposes
    private String fruitName;
    private String locationName; // e.g., Shop Name or Warehouse Name
    private String sourceCountry; // ADDED: To store fruit's source country

    // Default constructor
    public InventoryBean() {
    }

    // Getters and Setters for all fields...

    public int getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(int inventoryId) {
        this.inventoryId = inventoryId;
    }

    public int getFruitId() {
        return fruitId;
    }

    public void setFruitId(int fruitId) {
        this.fruitId = fruitId;
    }

    public Integer getShopId() {
        return shopId;
    }

    public void setShopId(Integer shopId) {
        this.shopId = shopId;
    }

    public Integer getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Integer warehouseId) {
        this.warehouseId = warehouseId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getFruitName() {
        return fruitName;
    }

    public void setFruitName(String fruitName) {
        this.fruitName = fruitName;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    // ADDED: Getter and Setter for sourceCountry
    public String getSourceCountry() {
        return sourceCountry;
    }

    public void setSourceCountry(String sourceCountry) {
        this.sourceCountry = sourceCountry;
    }

    @Override
    public String toString() {
        return "InventoryBean{" +
                "inventoryId=" + inventoryId +
                ", fruitId=" + fruitId +
                ", shopId=" + shopId +
                ", warehouseId=" + warehouseId +
                ", quantity=" + quantity +
                ", fruitName='" + fruitName + '\'' +
                ", locationName='" + locationName + '\'' +
                ", sourceCountry='" + sourceCountry + '\'' + // Added to toString
                '}';
    }
}
