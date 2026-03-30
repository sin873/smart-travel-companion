package com.yizhaoqi.smartpai.model.travel;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class SaveItineraryRequest {

    private String itineraryId;

    private String title;

    private String destination;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer travelerCount;

    private BigDecimal budget;

    private String summary;

    private List<ItineraryDayDTO> days;
}
