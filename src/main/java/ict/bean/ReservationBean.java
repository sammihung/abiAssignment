package ict.bean;

import java.io.Serializable;
import java.sql.Date;

public class ReservationBean implements Serializable {

    private int reservationId;
    private int fruitId;
    private int shopId;
    private int quantity;
    private Date reservationDate;
    private String status;

    private String fruitName;
    private String shopName;

    public ReservationBean() {
    }

    public ReservationBean(int reservationId, int fruitId, int shopId, int quantity, Date reservationDate,
            String status) {
        this.reservationId = reservationId;
        this.fruitId = fruitId;
        this.shopId = shopId;
        this.quantity = quantity;
        this.reservationDate = reservationDate;
        this.status = status;
    }

    public int getReservationId() {
        return reservationId;
    }

    public void setReservationId(int reservationId) {
        this.reservationId = reservationId;
    }

    public int getFruitId() {
        return fruitId;
    }

    public void setFruitId(int fruitId) {
        this.fruitId = fruitId;
    }

    public int getShopId() {
        return shopId;
    }

    public void setShopId(int shopId) {
        this.shopId = shopId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Date getReservationDate() {
        return reservationDate;
    }

    public void setReservationDate(Date reservationDate) {
        this.reservationDate = reservationDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFruitName() {
        return fruitName;
    }

    public void setFruitName(String fruitName) {
        this.fruitName = fruitName;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    @Override
    public String toString() {
        return "ReservationBean{" +
                "reservationId=" + reservationId +
                ", fruitId=" + fruitId +
                ", shopId=" + shopId +
                ", quantity=" + quantity +
                ", reservationDate=" + reservationDate +
                ", status='" + status + '\'' +
                '}';
    }
}