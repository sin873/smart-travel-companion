package com.yizhaoqi.smartpai.model.miniprogram;

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
        private String pace; // relaxed, moderate, intensive
        private List<String> interests; // historical, natural, cultural, shopping, food
        private Boolean avoidCrowds;
        private Boolean includeMeals;
    }
}
