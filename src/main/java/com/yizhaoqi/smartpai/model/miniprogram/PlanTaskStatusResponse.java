package com.yizhaoqi.smartpai.model.miniprogram;

import lombok.Data;

@Data
public class PlanTaskStatusResponse {
    private String taskId;
    private String status;
    private Integer progress;
    private String message;
    private ItineraryDTO itinerary;
}
