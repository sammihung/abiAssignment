package ict.bean;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class BorrowableFruitInfoBean extends FruitBean implements Serializable {

    private List<Map<String, Object>> lenderInfo;

    public BorrowableFruitInfoBean(FruitBean fruit) {
        super(fruit.getFruitId(), fruit.getFruitName(), fruit.getSourceCountry());
        this.lenderInfo = new java.util.ArrayList<>();
    }

    public List<Map<String, Object>> getLenderInfo() {
        return lenderInfo;
    }

    public void setLenderInfo(List<Map<String, Object>> lenderInfo) {
        this.lenderInfo = lenderInfo;
    }

    @Override
    public String toString() {
        return "BorrowableFruitInfoBean{" +
                "fruitId=" + getFruitId() +
                ", fruitName='" + getFruitName() + '\'' +
                ", sourceCountry='" + getSourceCountry() + '\'' +
                ", lenderInfo=" + lenderInfo +
                '}';
    }
}