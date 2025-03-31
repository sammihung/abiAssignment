package ict.bean;

public class BakeryShopBean {
    private String shop_id, shop_name, city, country;

    public BakeryShopBean() {
    }

    public BakeryShopBean(String shop_id, String shop_name, String city, String country) {
        this.shop_id = shop_id;
        this.shop_name = shop_name;
        this.city = city;
        this.country = country;
    }

    public String getShop_id() {
        return shop_id;
    }

    public void setShop_id(String shop_id) {
        this.shop_id = shop_id;
    }

    public String getShop_name() {
        return shop_name;
    }

    public void setShop_name(String shop_name) {
        this.shop_name = shop_name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;

    }
}