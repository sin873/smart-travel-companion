package com.yizhaoqi.smartpai.model.travel;

public class AmapRoutePlanResult {

    private boolean success;
    private String message;
    private String city;
    private String routeType;
    private PoiDTO origin;
    private PoiDTO destination;
    private Long distanceMeters;
    private Long durationSeconds;
    private String distanceText;
    private String durationText;
    private String strategy;
    private String polyline;

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

    public String getRouteType() {
        return routeType;
    }

    public void setRouteType(String routeType) {
        this.routeType = routeType;
    }

    public PoiDTO getOrigin() {
        return origin;
    }

    public void setOrigin(PoiDTO origin) {
        this.origin = origin;
    }

    public PoiDTO getDestination() {
        return destination;
    }

    public void setDestination(PoiDTO destination) {
        this.destination = destination;
    }

    public Long getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(Long distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public Long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getDistanceText() {
        return distanceText;
    }

    public void setDistanceText(String distanceText) {
        this.distanceText = distanceText;
    }

    public String getDurationText() {
        return durationText;
    }

    public void setDurationText(String durationText) {
        this.durationText = durationText;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getPolyline() {
        return polyline;
    }

    public void setPolyline(String polyline) {
        this.polyline = polyline;
    }
}
