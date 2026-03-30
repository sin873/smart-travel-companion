package com.yizhaoqi.smartpai.model.miniprogram;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class ItineraryDTO {
    private String itineraryId;
    private String title;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer travelerCount;
    private BigDecimal budget;
    private String status;
    private List<ItineraryDayDTO> days;
}

@Data
class ItineraryDayDTO {
    private String dayId;
    private Integer dayNumber;
    private LocalDate date;
    private String title;
    private String notes;
    private List<ItineraryAttractionDTO> attractions;
}

@Data
class ItineraryAttractionDTO {
    private String itemId;
    private String attractionId;
    private String attractionName;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer durationMinutes;
    private String notes;
}
