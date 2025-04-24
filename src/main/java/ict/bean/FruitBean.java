package ict.bean;

import java.io.Serializable;

public class FruitBean implements Serializable {

    private int fruitId;
    private String fruitName;
    private String sourceCountry;

    public FruitBean() {
    }

    public FruitBean(int fruitId, String fruitName, String sourceCountry) {
        this.fruitId = fruitId;
        this.fruitName = fruitName;
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