package com.yizhaoqi.smartpai.service.travel;

import com.yizhaoqi.smartpai.config.TravelAgentProperties;
import com.yizhaoqi.smartpai.entity.SearchResult;
import com.yizhaoqi.smartpai.model.travel.PoiDTO;
import com.yizhaoqi.smartpai.service.HybridSearchService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TravelPlanningTools {

    private final AmapService amapService;
    private final HybridSearchService hybridSearchService;
    private final TravelAgentProperties travelAgentProperties;

    public TravelPlanningTools(AmapService amapService,
                               HybridSearchService hybridSearchService,
                               TravelAgentProperties travelAgentProperties) {
        this.amapService = amapService;
        this.hybridSearchService = hybridSearchService;
        this.travelAgentProperties = travelAgentProperties;
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
        int topK = travelAgentProperties.getRagTopK() != null ? travelAgentProperties.getRagTopK() : 4;
        List<SearchResult> results;
        if (userId == null || userId.isBlank()) {
            results = hybridSearchService.search(query, topK);
        } else {
            results = hybridSearchService.searchWithPermission(query, userId, topK);
        }

        if (results == null || results.isEmpty()) {
            return "知识库中没有检索到明显相关的结果。";
        }

        return results.stream()
                .limit(topK)
                .map(result -> String.format("[%s] %s",
                        result.getFileName() != null ? result.getFileName() : result.getFileMd5(),
                        compact(result.getTextContent())))
                .collect(Collectors.joining("\n"));
    }

    @Tool("Plan a driving route between two place names in the same city.")
    public String planRoute(String city, String origin, String destination) {
        return amapService.planDrivingRoute(city, origin, destination);
    }

    private String compact(String text) {
        if (text == null) {
            return "";
        }
        String compacted = text.replaceAll("\\s+", " ").trim();
        return compacted.length() > 180 ? compacted.substring(0, 180) + "..." : compacted;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "未知" : value;
    }
}
