package ict.bean;

import java.io.Serializable;

/**
 * Represents aggregated fruit needs, typically grouped by country and fruit.
 */
public class AggregatedNeedBean implements Serializable {

    private String sourceCountry;
    private int fruitId;
    private String fruitName;
    private int totalNeededQuantity;

    // Default constructor
    public AggregatedNeedBean() {
    }

    // Getters and Setters
    public String getSourceCountry() {
        return sourceCountry;
    }

    public void setSourceCountry(String sourceCountry) {
        this.sourceCountry = sourceCountry;
    }

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

    public int getTotalNeededQuantity() {
        return totalNeededQuantity;
    }

    public void setTotalNeededQuantity(int totalNeededQuantity) {
        this.totalNeededQuantity = totalNeededQuantity;
    }

    @Override
    public String toString() {
        return "AggregatedNeedBean{" +
                "sourceCountry='" + sourceCountry + '\'' +
                ", fruitId=" + fruitId +
                ", fruitName='" + fruitName + '\'' +
                ", totalNeededQuantity=" + totalNeededQuantity +
                '}';
    }
}
