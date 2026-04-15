package com.yizhaoqi.smartpai.service.travel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yizhaoqi.smartpai.model.travel.AmapPoiSearchResult;
import com.yizhaoqi.smartpai.model.travel.AmapRoutePlanResult;
import com.yizhaoqi.smartpai.model.travel.PoiDTO;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AmapService {

    private static final Logger logger = LoggerFactory.getLogger(AmapService.class);
    private static final String DEFAULT_SCENIC_KEYWORD = "景点";
    private static final String DEFAULT_SCENIC_TYPES = "风景名胜";
    private static final int DEFAULT_POI_LIMIT = 10;
    private static final int DEFAULT_KEYWORD_LIMIT = 5;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String poiUrl;
    private final String directionUrl;
    private final OkHttpClient httpClient;

    @Autowired
    public AmapService(RestTemplate restTemplate,
                       ObjectMapper objectMapper,
                       @Value("${travel.amap.api-key:b4a4f48b3c7e0f7401316f33997ad1c2}") String apiKey,
                       @Value("${travel.amap.poi-url:https://restapi.amap.com/v3/place/text}") String poiUrl,
                       @Value("${travel.amap.direction-url:https://restapi.amap.com/v3/direction/driving}") String directionUrl,
                       @Value("${travel.amap.connect-timeout-seconds:10}") int connectTimeoutSeconds,
                       @Value("${travel.amap.read-timeout-seconds:20}") int readTimeoutSeconds,
                       @Value("${travel.amap.call-timeout-seconds:30}") int callTimeoutSeconds) {
        this(restTemplate, objectMapper, apiKey, poiUrl, directionUrl, new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(Math.max(1, connectTimeoutSeconds)))
                .readTimeout(Duration.ofSeconds(Math.max(1, readTimeoutSeconds)))
                .callTimeout(Duration.ofSeconds(Math.max(1, callTimeoutSeconds)))
                .build());
    }

    AmapService(RestTemplate restTemplate,
                ObjectMapper objectMapper,
                String apiKey,
                String poiUrl,
                String directionUrl,
                OkHttpClient httpClient) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.poiUrl = poiUrl;
        this.directionUrl = directionUrl;
        this.httpClient = httpClient;
    }

    public List<PoiDTO> searchPoi(String city) {
        return searchScenicPois(city).getPois();
    }

    public List<PoiDTO> searchPoiByKeyword(String city, String keyword) {
        return searchPoiResult(city, keyword, null, DEFAULT_KEYWORD_LIMIT).getPois();
    }

    public AmapPoiSearchResult searchScenicPois(String city) {
        return searchPoiResult(city, DEFAULT_SCENIC_KEYWORD, DEFAULT_SCENIC_TYPES, DEFAULT_POI_LIMIT);
    }

    public AmapPoiSearchResult searchPoiResult(String city, String keyword, String types, int limit) {
        logger.info("Searching Amap POI, city={}, keyword={}", city, keyword);
        AmapPoiSearchResult result = new AmapPoiSearchResult();
        result.setCity(city);
        result.setKeyword(keyword);

        if (isBlank(city) || isBlank(keyword)) {
            result.setSuccess(false);
            result.setMessage("缺少 POI 检索所需的城市或关键词");
            return result;
        }

        try {
            String url = buildPoiUrl(city, keyword, types, limit);
            String response = get(url);
            JsonNode root = readRoot(response);
            if (!isAmapSuccess(root)) {
                result.setSuccess(false);
                result.setMessage(extractAmapMessage(root, "高德 POI 检索失败"));
                return result;
            }

            List<PoiDTO> pois = new ArrayList<>();
            JsonNode poisNode = root.path("pois");
            if (poisNode.isArray()) {
                for (JsonNode poiNode : poisNode) {
                    PoiDTO poi = parsePoi(poiNode);
                    if (poi != null) {
                        pois.add(poi);
                    }
                }
            }

            List<PoiDTO> normalizedPois = normalizePois(pois, limit);
            result.setSuccess(true);
            result.setPois(normalizedPois);
            result.setMessage(normalizedPois.isEmpty() ? "未检索到匹配的 POI" : "ok");
            return result;
        } catch (Exception e) {
            logger.warn("Failed to search POI from Amap, city={}, keyword={}", city, keyword, e);
            result.setSuccess(false);
            result.setMessage("高德 POI 检索失败，已降级为空结果");
            return result;
        }
    }

    public String planDrivingRoute(String city, String originKeyword, String destinationKeyword) {
        return summarizeRoute(planDrivingRouteResult(city, originKeyword, destinationKeyword));
    }

    public AmapRoutePlanResult planDrivingRouteResult(String city, String originKeyword, String destinationKeyword) {
        AmapRoutePlanResult result = new AmapRoutePlanResult();
        result.setCity(city);
        result.setRouteType("driving");

        if (isBlank(city) || isBlank(originKeyword) || isBlank(destinationKeyword)) {
            result.setSuccess(false);
            result.setMessage("缺少路线规划所需的城市或起终点");
            return result;
        }

        AmapPoiSearchResult originSearch = searchPoiResult(city, originKeyword, null, 1);
        AmapPoiSearchResult destinationSearch = searchPoiResult(city, destinationKeyword, null, 1);
        if (originSearch.getPois().isEmpty() || destinationSearch.getPois().isEmpty()) {
            result.setSuccess(false);
            result.setMessage(String.format("未能在%s找到可用于路线规划的地点：%s -> %s", city, originKeyword, destinationKeyword));
            return result;
        }

        PoiDTO origin = originSearch.getPois().get(0);
        PoiDTO destination = destinationSearch.getPois().get(0);
        result.setOrigin(origin);
        result.setDestination(destination);

        if (!hasCoordinate(origin) || !hasCoordinate(destination)) {
            result.setSuccess(false);
            result.setMessage(String.format("路线规划缺少坐标信息：%s -> %s", safe(origin.getName()), safe(destination.getName())));
            return result;
        }

        try {
            String url = buildDrivingUrl(origin, destination);
            String response = get(url);
            JsonNode root = readRoot(response);
            if (!isAmapSuccess(root)) {
                result.setSuccess(false);
                result.setMessage(extractAmapMessage(root, "高德路线规划失败"));
                return result;
            }

            JsonNode path = firstPath(root.path("route").path("paths"));
            if (path == null) {
                result.setSuccess(false);
                result.setMessage(String.format("高德未返回可用路线：%s -> %s", safe(origin.getName()), safe(destination.getName())));
                return result;
            }

            long distanceMeters = path.path("distance").asLong(0L);
            long durationSeconds = path.path("duration").asLong(0L);

            result.setSuccess(true);
            result.setDistanceMeters(distanceMeters);
            result.setDurationSeconds(durationSeconds);
            result.setDistanceText(formatDistance(distanceMeters));
            result.setDurationText(formatDuration(durationSeconds));
            result.setStrategy(path.path("strategy").asText(""));
            result.setPolyline(extractPolyline(path));
            result.setMessage("ok");
            return result;
        } catch (Exception e) {
            logger.warn("Failed to plan route via Amap, city={}, origin={}, destination={}",
                    city, originKeyword, destinationKeyword, e);
            result.setSuccess(false);
            result.setMessage(String.format("高德路线规划失败，已降级返回：%s -> %s", originKeyword, destinationKeyword));
            return result;
        }
    }

    public String summarizePoiSearch(AmapPoiSearchResult result) {
        if (result == null) {
            return "POI 检索结果为空";
        }
        if (!result.isSuccess() && (result.getPois() == null || result.getPois().isEmpty())) {
            return result.getMessage();
        }
        if (result.getPois() == null || result.getPois().isEmpty()) {
            return String.format("%s没有检索到与%s相关的 POI", safe(result.getCity()), safe(result.getKeyword()));
        }

        List<String> lines = new ArrayList<>();
        for (PoiDTO poi : result.getPois().stream().limit(8).toList()) {
            lines.add(String.format("%s | %s | 坐标:%s,%s | 区域:%s | 类型:%s",
                    safe(poi.getName()),
                    safe(poi.getAddress()),
                    poi.getLongitude() != null ? poi.getLongitude() : "未知",
                    poi.getLatitude() != null ? poi.getLatitude() : "未知",
                    safe(joinLocation(poi)),
                    safe(poi.getType())));
        }
        return String.join("\n", lines);
    }

    public String summarizeRoute(AmapRoutePlanResult result) {
        if (result == null) {
            return "路线规划结果为空";
        }
        if (!result.isSuccess()) {
            return result.getMessage();
        }
        return String.format("%s -> %s：驾车约%s，距离约%s",
                safe(result.getOrigin() != null ? result.getOrigin().getName() : null),
                safe(result.getDestination() != null ? result.getDestination().getName() : null),
                safe(result.getDurationText()),
                safe(result.getDistanceText()));
    }

    private String buildPoiUrl(String city, String keyword, String types, int offset) {
        StringBuilder builder = new StringBuilder(poiUrl);
        builder.append("?key=").append(encode(apiKey));
        builder.append("&keywords=").append(encode(keyword));
        builder.append("&city=").append(encode(city));
        if (!isBlank(types)) {
            builder.append("&types=").append(encode(types));
        }
        builder.append("&offset=").append(Math.max(1, offset));
        return builder.toString();
    }

    private String buildDrivingUrl(PoiDTO origin, PoiDTO destination) {
        return directionUrl
                + "?key=" + encode(apiKey)
                + "&origin=" + encode(origin.getLongitude() + "," + origin.getLatitude())
                + "&destination=" + encode(destination.getLongitude() + "," + destination.getLatitude());
    }

    private String get(String url) throws Exception {
        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IllegalStateException("HTTP " + response.code());
            }
            return response.body() != null ? response.body().string() : "";
        }
    }

    private JsonNode readRoot(String response) throws Exception {
        if (response == null || response.isBlank()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(response);
    }

    private boolean isAmapSuccess(JsonNode root) {
        return "1".equals(root.path("status").asText())
                && !"0".equals(root.path("infocode").asText())
                && !"false".equalsIgnoreCase(root.path("info").asText());
    }

    private String extractAmapMessage(JsonNode root, String fallback) {
        String info = root.path("info").asText("");
        String infocode = root.path("infocode").asText("");
        if (!isBlank(info) && !isBlank(infocode)) {
            return info + " (" + infocode + ")";
        }
        if (!isBlank(info)) {
            return info;
        }
        return fallback;
    }

    private JsonNode firstPath(JsonNode pathsNode) {
        if (pathsNode.isArray() && !pathsNode.isEmpty()) {
            return pathsNode.get(0);
        }
        return null;
    }

    private String extractPolyline(JsonNode pathNode) {
        JsonNode steps = pathNode.path("steps");
        if (!steps.isArray() || steps.isEmpty()) {
            return "";
        }

        List<String> segments = new ArrayList<>();
        for (JsonNode step : steps) {
            String polyline = step.path("polyline").asText("");
            if (!isBlank(polyline)) {
                segments.add(polyline);
            }
        }
        return String.join(";", segments);
    }

    private List<PoiDTO> normalizePois(List<PoiDTO> pois, int limit) {
        if (pois == null || pois.isEmpty()) {
            return List.of();
        }

        Map<String, PoiDTO> deduplicated = new LinkedHashMap<>();
        for (PoiDTO poi : pois) {
            if (poi == null || isBlank(poi.getName())) {
                continue;
            }
            String key = !isBlank(poi.getPoiId())
                    ? poi.getPoiId()
                    : poi.getName() + "|" + safe(poi.getAddress());
            deduplicated.putIfAbsent(key, poi);
        }

        return deduplicated.values().stream()
                .sorted(Comparator
                        .comparingInt(this::poiScore)
                        .reversed()
                        .thenComparing(PoiDTO::getName, String.CASE_INSENSITIVE_ORDER))
                .limit(Math.max(1, limit))
                .toList();
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
            poi.setCity(poiNode.path("cityname").asText(""));
            poi.setDistrict(poiNode.path("adname").asText(""));
            poi.setType(poiNode.path("type").asText(""));
            poi.setPoiId(poiNode.path("id").asText(""));
            poi.setLongitude(longitude);
            poi.setLatitude(latitude);
            return poi;
        } catch (Exception e) {
            logger.warn("Failed to parse Amap POI node", e);
            return null;
        }
    }

    private int poiScore(PoiDTO poi) {
        int score = 0;
        String type = safe(poi.getType()).toLowerCase(Locale.ROOT);
        String name = safe(poi.getName()).toLowerCase(Locale.ROOT);

        if (type.contains("国家级景点") || type.contains("风景名胜")) {
            score += 40;
        }
        if (type.contains("旅游景点") || type.contains("纪念馆")) {
            score += 20;
        }
        if (type.contains("公园")) {
            score += 10;
        }
        if (name.contains("西湖") || name.contains("景区") || name.contains("景点")) {
            score += 10;
        }
        if (hasCoordinate(poi)) {
            score += 5;
        }
        return score;
    }

    private boolean hasCoordinate(PoiDTO poi) {
        return poi != null && poi.getLongitude() != null && poi.getLatitude() != null;
    }

    private String joinLocation(PoiDTO poi) {
        if (poi == null) {
            return "";
        }
        if (!isBlank(poi.getCity()) && !isBlank(poi.getDistrict())) {
            return poi.getCity() + "-" + poi.getDistrict();
        }
        return !isBlank(poi.getDistrict()) ? poi.getDistrict() : poi.getCity();
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

    private String safe(String value) {
        return isBlank(value) ? "未知" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
