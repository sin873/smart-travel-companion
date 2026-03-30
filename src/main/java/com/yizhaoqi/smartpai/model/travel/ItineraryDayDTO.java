package com.yizhaoqi.smartpai.model.travel;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ItineraryDayDTO {

    private String dayId;

    private Integer dayNumber;

    private LocalDate travelDate;

    private String title;

    private String notes;

    private List<ItineraryItemDTO> items;
}
