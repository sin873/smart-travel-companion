package com.yizhaoqi.smartpai.model.travel;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
public class ItineraryItemDTO {

    private String itemId;

    private String attractionId;

    private String attractionName;

    private String address;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private LocalTime startTime;

    private LocalTime endTime;

    private Integer durationMinutes;

    private Integer orderIndex;

    private String notes;
}
