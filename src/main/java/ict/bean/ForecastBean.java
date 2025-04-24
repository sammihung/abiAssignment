package ict.bean;

import java.io.Serializable;
import java.math.BigDecimal;

public class ForecastBean implements Serializable {

    private String targetCountry;
    private int fruitId;
    private String fruitName;
    private BigDecimal averageDailyConsumption;

    public ForecastBean() {
    }

    public String getTargetCountry() {
        return targetCountry;
    }

    public void setTargetCountry(String targetCountry) {
        this.targetCountry = targetCountry;
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

    public BigDecimal getAverageDailyConsumption() {
        return averageDailyConsumption;
    }

    public void setAverageDailyConsumption(BigDecimal averageDailyConsumption) {
        this.averageDailyConsumption = averageDailyConsumption;
    }

    @Override
    public String toString() {
        return "ForecastBean{" +
                "targetCountry='" + targetCountry + '\'' +
                ", fruitId=" + fruitId +
                ", fruitName='" + fruitName + '\'' +
                ", averageDailyConsumption=" + averageDailyConsumption +
                '}';
    }
}