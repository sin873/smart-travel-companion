package com.yizhaoqi.smartpai.model.travel;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PlanItineraryRequest {

    private String destination;

    private String startDate;

    private String endDate;

    private Integer travelerCount;

    private BigDecimal budget;

    private UserPreferences preferences;

    @Data
    public static class UserPreferences {
        private String pace;
        private List<String> interests;
        private Boolean avoidCrowds;
        private Boolean includeMeals;
    }
}
