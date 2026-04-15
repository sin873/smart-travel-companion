package com.yizhaoqi.smartpai.model.travel;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PoiCandidate {

    private String candidateId;

    private String name;

    private String address;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private Integer recommendedDurationMinutes;

    private List<String> categories;

    private String source;
}
