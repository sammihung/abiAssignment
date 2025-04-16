package ict.bean;

import java.io.Serializable;

/**
 * Represents a fruit entity based on the 'fruits' table.
 */
public class FruitBean implements Serializable {

    private int fruitId;        // Corresponds to fruit_id (INT)
    private String fruitName;     // Corresponds to fruit_name (VARCHAR)
    private String sourceCountry; // Corresponds to source_country (VARCHAR)

    // Default constructor
    public FruitBean() {
    }

    // Constructor with all fields
    public FruitBean(int fruitId, String fruitName, String sourceCountry) {
        this.fruitId = fruitId;
        this.fruitName = fruitName;
        this.sourceCountry = sourceCountry;
    }

    // Getters and Setters
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

    public String getSourceCountry() {
        return sourceCountry;
    }

    public void setSourceCountry(String sourceCountry) {
        this.sourceCountry = sourceCountry;
    }

    @Override
    public String toString() {
        return "FruitBean{" +
                "fruitId=" + fruitId +
                ", fruitName='" + fruitName + '\'' +
                ", sourceCountry='" + sourceCountry + '\'' +
                '}';
    }
}
