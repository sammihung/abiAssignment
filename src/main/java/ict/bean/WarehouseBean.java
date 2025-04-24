package ict.bean;

public class WarehouseBean {
    private String warehouse_id, warehouse_name, city, country, is_source;

    public WarehouseBean() {
    }

    public WarehouseBean(String warehouse_id, String warehouse_name, String city, String country, String is_source) {
        this.warehouse_id = warehouse_id;
        this.warehouse_name = warehouse_name;
        this.city = city;
        this.country = country;
        this.is_source = is_source;
    }

    public String getWarehouse_id() {
        return warehouse_id;
    }

    public void setWarehouse_id(String warehouse_id) {
        this.warehouse_id = warehouse_id;
    }

    public String getWarehouse_name() {
        return warehouse_name;
    }

    public void setWarehouse_name(String warehouse_name) {
        this.warehouse_name = warehouse_name;
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

    public String getIs_source() {
        return is_source;
    }

    public void setIs_source(String is_source) {
        this.is_source = is_source;
    }

    public void setId(String int1) {
        this.warehouse_id = int1;
    }

    public String getName() {
        return warehouse_name;
    }

    public void setName(String name) {
        this.warehouse_name = name;
    }
}