
package com.yizhaoqi.smartpai.model.travel;

import java.math.BigDecimal;

public class PoiDTO {
    private String name;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;

    public PoiDTO() {
    }

    public PoiDTO(String name, String address, BigDecimal longitude, BigDecimal latitude) {
        this.name = name;
        this.address = address;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    @Override
    public String toString() {
        return "PoiDTO{" +
                "name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", longitude=" + longitude +
                ", latitude=" + latitude +
                '}';
    }
}

