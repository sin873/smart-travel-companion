package com.yizhaoqi.smartpai.model.travel;

import java.util.ArrayList;
import java.util.List;

public class AmapPoiSearchResult {

    private boolean success;
    private String message;
    private String city;
    private String keyword;
    private List<PoiDTO> pois = new ArrayList<>();

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public List<PoiDTO> getPois() {
        return pois;
    }

    public void setPois(List<PoiDTO> pois) {
        this.pois = pois;
    }
}
