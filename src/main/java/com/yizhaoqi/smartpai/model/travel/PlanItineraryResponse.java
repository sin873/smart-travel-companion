package com.yizhaoqi.smartpai.model.travel;

import lombok.Data;

@Data
public class PlanItineraryResponse {

    private String taskId;

    private String status;

    private Integer estimatedTime;
}
