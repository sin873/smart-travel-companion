package com.yizhaoqi.smartpai.service.travel;

import com.yizhaoqi.smartpai.model.travel.PoiDTO;
import com.yizhaoqi.smartpai.service.travel.rag.TravelKnowledgeRagService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TravelPlanningTools {

    private final AmapService amapService;
    private final TravelKnowledgeRagService travelKnowledgeRagService;

    public TravelPlanningTools(AmapService amapService,
                               TravelKnowledgeRagService travelKnowledgeRagService) {
        this.amapService = amapService;
        this.travelKnowledgeRagService = travelKnowledgeRagService;
    }

    @Tool("Search attraction candidates in a city and return names, addresses, and coordinates.")
    public String searchCityPois(String city) {
        List<PoiDTO> pois = amapService.searchPoi(city);
        if (pois.isEmpty()) {
            return "未检索到可用景点候选。";
        }

        return pois.stream()
                .limit(8)
                .map(poi -> String.format("%s | %s | 坐标:%s,%s",
                        safe(poi.getName()),
                        safe(poi.getAddress()),
                        poi.getLongitude() != null ? poi.getLongitude() : "未知",
                        poi.getLatitude() != null ? poi.getLatitude() : "未知"))
                .collect(Collectors.joining("\n"));
    }

    @Tool("Search local RAG knowledge for travel planning. Provide userId and a focused Chinese query.")
    public String searchTravelKnowledge(String userId, String query) {
        return travelKnowledgeRagService.searchKnowledgeForAgent(userId, query);
    }

    @Tool("Plan a driving route between two place names in the same city.")
    public String planRoute(String city, String origin, String destination) {
        return amapService.planDrivingRoute(city, origin, destination);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "未知" : value;
    }
}
