package com.yizhaoqi.smartpai.service.travel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yizhaoqi.smartpai.model.travel.PoiDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AmapService {

    private static final Logger logger = LoggerFactory.getLogger(AmapService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String poiUrl;
    private final String directionUrl;

    public AmapService(RestTemplate restTemplate,
                       ObjectMapper objectMapper,
                       @Value("${travel.amap.api-key:b4a4f48b3c7e0f7401316f33997ad1c2}") String apiKey,
                       @Value("${travel.amap.poi-url:https://restapi.amap.com/v3/place/text}") String poiUrl,
                       @Value("${travel.amap.direction-url:https://restapi.amap.com/v3/direction/driving}") String directionUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.poiUrl = poiUrl;
        this.directionUrl = directionUrl;
    }

    public List<PoiDTO> searchPoi(String city) {
        return searchPoi(city, "景点", "风景名胜", 10);
    }

    public List<PoiDTO> searchPoiByKeyword(String city, String keyword) {
        return searchPoi(city, keyword, null, 5);
    }

    public String planDrivingRoute(String city, String originKeyword, String destinationKeyword) {
        List<PoiDTO> originCandidates = searchPoiByKeyword(city, originKeyword);
        List<PoiDTO> destinationCandidates = searchPoiByKeyword(city, destinationKeyword);
        if (originCandidates.isEmpty() || destinationCandidates.isEmpty()) {
            return String.format("未能在%s找到可用于规划的路线点：%s -> %s", city, originKeyword, destinationKeyword);
        }

        PoiDTO origin = originCandidates.get(0);
        PoiDTO destination = destinationCandidates.get(0);
        if (origin.getLongitude() == null || origin.getLatitude() == null
                || destination.getLongitude() == null || destination.getLatitude() == null) {
            return String.format("路线规划缺少坐标信息：%s -> %s", originKeyword, destinationKeyword);
        }

        try {
            String url = buildDrivingUrl(origin, destination);
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode route = root.path("route");
            JsonNode path = route.path("paths").isArray() && route.path("paths").size() > 0
                    ? route.path("paths").get(0)
                    : null;
            if (path == null || path.isMissingNode()) {
                return String.format("高德未返回可用路线：%s -> %s", origin.getName(), destination.getName());
            }

            long distanceMeters = path.path("distance").asLong(0L);
            long durationSeconds = path.path("duration").asLong(0L);
            String distanceText = formatDistance(distanceMeters);
            String durationText = formatDuration(durationSeconds);
            return String.format("%s -> %s：驾车约%s，距离约%s",
                    origin.getName(), destination.getName(), durationText, distanceText);
        } catch (Exception e) {
            logger.warn("Failed to plan route via Amap, city={}, origin={}, destination={}",
                    city, originKeyword, destinationKeyword, e);
            return String.format("高德路线规划失败：%s -> %s", originKeyword, destinationKeyword);
        }
    }

    private List<PoiDTO> searchPoi(String city, String keyword, String types, int offset) {
        logger.info("Searching Amap POI, city={}, keyword={}", city, keyword);
        List<PoiDTO> result = new ArrayList<>();

        try {
            String url = buildPoiUrl(city, keyword, types, offset);
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode poisNode = root.path("pois");
            if (poisNode.isArray()) {
                for (JsonNode poiNode : poisNode) {
                    PoiDTO poi = parsePoi(poiNode);
                    if (poi != null) {
                        result.add(poi);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to search POI from Amap, city={}, keyword={}", city, keyword, e);
        }

        return result;
    }

    private String buildPoiUrl(String city, String keyword, String types, int offset) {
        StringBuilder builder = new StringBuilder(poiUrl);
        builder.append("?key=").append(encode(apiKey));
        builder.append("&keywords=").append(encode(keyword));
        builder.append("&city=").append(encode(city));
        if (types != null && !types.isBlank()) {
            builder.append("&types=").append(encode(types));
        }
        builder.append("&offset=").append(offset);
        return builder.toString();
    }

    private String buildDrivingUrl(PoiDTO origin, PoiDTO destination) {
        return directionUrl
                + "?key=" + encode(apiKey)
                + "&origin=" + encode(origin.getLongitude() + "," + origin.getLatitude())
                + "&destination=" + encode(destination.getLongitude() + "," + destination.getLatitude());
    }

    private PoiDTO parsePoi(JsonNode poiNode) {
        try {
            String name = poiNode.path("name").asText("");
            String address = poiNode.path("address").asText("");
            String location = poiNode.path("location").asText("");

            BigDecimal longitude = null;
            BigDecimal latitude = null;
            if (location.contains(",")) {
                String[] parts = location.split(",");
                if (parts.length == 2) {
                    longitude = new BigDecimal(parts[0]);
                    latitude = new BigDecimal(parts[1]);
                }
            }

            PoiDTO poi = new PoiDTO();
            poi.setName(name);
            poi.setAddress(address);
            poi.setLongitude(longitude);
            poi.setLatitude(latitude);
            return poi;
        } catch (Exception e) {
            logger.warn("Failed to parse Amap POI node", e);
            return null;
        }
    }

    private String formatDistance(long distanceMeters) {
        if (distanceMeters <= 0) {
            return "未知";
        }
        if (distanceMeters < 1000) {
            return distanceMeters + "米";
        }
        return String.format(Locale.ROOT, "%.1f公里", distanceMeters / 1000.0);
    }

    private String formatDuration(long durationSeconds) {
        if (durationSeconds <= 0) {
            return "未知";
        }
        long minutes = Math.max(1L, durationSeconds / 60L);
        long hours = minutes / 60L;
        long remainMinutes = minutes % 60L;
        if (hours == 0L) {
            return minutes + "分钟";
        }
        if (remainMinutes == 0L) {
            return hours + "小时";
        }
        return hours + "小时" + remainMinutes + "分钟";
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
