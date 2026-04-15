package com.yizhaoqi.smartpai.model.travel;

import lombok.Data;

import java.util.List;

@Data
public class PlanningContext {

    private String destination;

    private UserIntent userIntent;

    private List<PoiCandidate> poiCandidates;

    private List<String> retrievedKnowledge;

    private String constraintSummary;

    private boolean usedAmapData;
}
