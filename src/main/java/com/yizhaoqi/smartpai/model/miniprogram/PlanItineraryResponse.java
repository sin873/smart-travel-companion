package com.yizhaoqi.smartpai.model.miniprogram;

import lombok.Data;
import java.util.List;

@Data
public class PlanItineraryResponse {
    private String taskId;
    private String status;
    private Integer estimatedTime;
}
