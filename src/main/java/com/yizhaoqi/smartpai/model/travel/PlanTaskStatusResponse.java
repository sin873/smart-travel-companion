package com.yizhaoqi.smartpai.model.travel;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class PlanTaskStatusResponse {

    private String taskId;

    private String status;

    private Integer progress;

    private String message;

    private ItineraryDTO itinerary;
}
