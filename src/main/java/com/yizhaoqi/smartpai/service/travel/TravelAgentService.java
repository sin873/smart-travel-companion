package com.yizhaoqi.smartpai.service.travel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yizhaoqi.smartpai.entity.travel.TravelPlanTask;
import com.yizhaoqi.smartpai.model.travel.ItineraryDTO;
import com.yizhaoqi.smartpai.model.travel.ItineraryDayDTO;
import com.yizhaoqi.smartpai.model.travel.ItineraryDraft;
import com.yizhaoqi.smartpai.model.travel.ItineraryItemDTO;
import com.yizhaoqi.smartpai.model.travel.PlanItineraryRequest;
import com.yizhaoqi.smartpai.model.travel.PlanningContext;
import com.yizhaoqi.smartpai.model.travel.PoiCandidate;
import com.yizhaoqi.smartpai.model.travel.PoiDTO;
import com.yizhaoqi.smartpai.model.travel.UserIntent;
import com.yizhaoqi.smartpai.repository.travel.TravelPlanTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * TravelAgentService
 *
 * Lightweight Agent workflow for itinerary generation:
 * 1. Perception
 * 2. Planning
 * 3. Decision
 * 4. Action
 */
@Service
public class TravelAgentService {

    private static final Logger logger = LoggerFactory.getLogger(TravelAgentService.class);

    private static final Map<String, String> INTEREST_ALIASES = new HashMap<>();

    static {
        INTEREST_ALIASES.put("natural", "nature");
        INTEREST_ALIASES.put("nature", "nature");
        INTEREST_ALIASES.put("historical", "historical");
        INTEREST_ALIASES.put("cultural", "cultural");
        INTEREST_ALIASES.put("shopping", "shopping");
        INTEREST_ALIASES.put("food", "food");
    }

    @Autowired
    private TravelPlanTaskRepository travelPlanTaskRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AmapService amapService;

    @Transactional
    public void executePlan(String taskId, PlanItineraryRequest request, String userId) {
        logger.info("Starting plan execution - taskId: {}, destination: {}, userId: {}", taskId, request.getDestination(), userId);

        TravelPlanTask task = travelPlanTaskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        try {
            updateTaskStatus(task, "QUEUED", 0, "任务已创建，正在排队...");
            simulateDelay(100);

            updateTaskStatus(task, "PROCESSING", 20, "正在解析用户需求...");
            UserIntent intent = perceive(request);

            updateTaskStatus(task, "PROCESSING", 50, "正在组织候选景点...");
            PlanningContext context = plan(intent);

            updateTaskStatus(task, "PROCESSING", 80, "正在生成行程草案...");
            ItineraryDraft draft = decide(intent, context);

            ItineraryDTO result = act(task, draft);
            updateTaskStatus(task, "COMPLETED", 100, "行程规划完成!");

            logger.info("Plan completed successfully - taskId: {}, itineraryId: {}", taskId, result.getItineraryId());
        } catch (Exception e) {
            logger.error("Plan execution failed - taskId: {}", taskId, e);
            task.setStatus("FAILED");
            task.setProgress(0);
            task.setErrorMessage("规划失败:" + e.getMessage());
            travelPlanTaskRepository.save(task);
        }
    }

    /**
     * Phase 1: Perception
     * Parse and normalize user input into structured intent.
     */
    public UserIntent perceive(PlanItineraryRequest request) {
        UserIntent intent = new UserIntent();
        intent.setDestination(request.getDestination());
        intent.setStartDate(LocalDate.parse(request.getStartDate()));
        intent.setEndDate(LocalDate.parse(request.getEndDate()));
        intent.setTravelerCount(request.getTravelerCount() != null ? request.getTravelerCount() : 2);
        intent.setBudget(request.getBudget());
        intent.setPace(request.getPreferences() != null ? request.getPreferences().getPace() : null);
        intent.setInterests(normalizeInterests(request));
        intent.setDays(calculateDayCount(intent.getStartDate(), intent.getEndDate()));

        logger.info("【Perception】解析用户输入成功 destination={}, days={}, travelerCount={}, interests={}",
                intent.getDestination(), intent.getDays(), intent.getTravelerCount(), intent.getInterests());
        return intent;
    }

    /**
     * Phase 2: Planning
     * Gather candidate POIs and external knowledge.
     */
    public PlanningContext plan(UserIntent intent) {
        PlanningContext context = new PlanningContext();
        context.setDestination(intent.getDestination());
        context.setUserIntent(intent);

        List<PoiCandidate> poiCandidates = retrievePoiCandidates(intent);
        List<String> retrievedKnowledge = retrieveKnowledge(intent);

        context.setPoiCandidates(poiCandidates);
        context.setRetrievedKnowledge(retrievedKnowledge);
        context.setConstraintSummary(buildConstraintSummary(intent));
        context.setUsedAmapData(poiCandidates.stream().anyMatch(candidate -> "amap".equals(candidate.getSource())));

        logger.info("【Planning】高德POI候选数量：{}", poiCandidates.size());
        logger.info("【Planning】知识检索结果数量：{}", retrievedKnowledge.size());
        return context;
    }

    /**
     * Phase 3: Decision
     * Select and allocate POIs into an itinerary draft.
     */
    public ItineraryDraft decide(UserIntent intent, PlanningContext context) {
        List<PoiCandidate> selectedCandidates = selectCandidates(intent, context);

        ItineraryDraft draft = new ItineraryDraft();
        draft.setItineraryId("itin_" + UUID.randomUUID().toString().substring(0, 8));
        draft.setTitle(intent.getDestination() + " " + intent.getDays() + "日游");
        draft.setDestination(intent.getDestination());
        draft.setStartDate(intent.getStartDate());
        draft.setEndDate(intent.getEndDate());
        draft.setTravelerCount(intent.getTravelerCount());
        draft.setBudget(intent.getBudget());
        draft.setStatus("PLANNED");
        draft.setSummary(buildSummary(intent, context, selectedCandidates));
        draft.setDays(generateDays(intent, selectedCandidates));

        logger.info("【Decision】生成 itinerary 草案成功 itineraryId={}, selectedPoiCount={}, dayCount={}",
                draft.getItineraryId(), selectedCandidates.size(), draft.getDays().size());
        return draft;
    }

    /**
     * Phase 4: Action
     * Materialize draft into final DTO and persist task result.
     */
    public ItineraryDTO act(TravelPlanTask task, ItineraryDraft draft) throws Exception {
        ItineraryDTO result = toItineraryDTO(draft);
        String resultJson = objectMapper.writeValueAsString(result);
        task.setResultJson(resultJson);
        task.setErrorMessage(null);
        travelPlanTaskRepository.save(task);

        logger.info("【Action】结果持久化完成 taskId={}, itineraryId={}", task.getTaskId(), result.getItineraryId());
        return result;
    }

    private List<String> normalizeInterests(PlanItineraryRequest request) {
        if (request.getPreferences() == null || request.getPreferences().getInterests() == null) {
            return new ArrayList<>();
        }

        return request.getPreferences().getInterests().stream()
                .map(this::normalizeInterest)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<PoiCandidate> retrievePoiCandidates(UserIntent intent) {
        List<PoiCandidate> candidates = new ArrayList<>();

        try {
            List<PoiDTO> pois = amapService.searchPoi(intent.getDestination());
            if (pois != null && !pois.isEmpty()) {
                candidates.addAll(pois.stream()
                        .map(this::toAmapCandidate)
                        .collect(Collectors.toList()));
            }
        } catch (Exception e) {
            logger.warn("【Planning】高德POI调用失败，回退到本地候选数据", e);
        }

        if (candidates.isEmpty()) {
            candidates.addAll(MockAttractionDataProvider.getAttractionsByCity(intent.getDestination()).stream()
                    .map(this::toMockCandidate)
                    .collect(Collectors.toList()));
        }

        return candidates;
    }

    private List<String> retrieveKnowledge(UserIntent intent) {
        // Reserved for future RAG integration in planning phase.
        // Current implementation keeps the structure explicit for defense/demo.
        logger.debug("【Planning】知识检索预留入口 destination={}", intent.getDestination());
        return new ArrayList<>();
    }

    private String buildConstraintSummary(UserIntent intent) {
        return String.format("destination=%s, days=%d, travelers=%d, budget=%s, interests=%s",
                intent.getDestination(),
                intent.getDays(),
                intent.getTravelerCount(),
                intent.getBudget(),
                intent.getInterests());
    }

    private List<PoiCandidate> selectCandidates(UserIntent intent, PlanningContext context) {
        List<PoiCandidate> candidates = context.getPoiCandidates() == null
                ? new ArrayList<>()
                : new ArrayList<>(context.getPoiCandidates());

        if (intent.getInterests() == null || intent.getInterests().isEmpty()) {
            return candidates.stream().limit(6).collect(Collectors.toList());
        }

        List<PoiCandidate> filtered = candidates.stream()
                .filter(candidate -> candidateMatchesInterest(candidate, intent.getInterests()))
                .limit(8)
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            logger.warn("【Decision】偏好筛选无结果，回退到热门候选 interests={}", intent.getInterests());
            return candidates.stream().limit(6).collect(Collectors.toList());
        }

        return filtered;
    }

    private boolean candidateMatchesInterest(PoiCandidate candidate, List<String> interests) {
        if (candidate.getCategories() == null || candidate.getCategories().isEmpty()) {
            return false;
        }

        for (String interest : interests) {
            if (candidate.getCategories().contains(interest)) {
                return true;
            }
        }

        return false;
    }

    private List<ItineraryDayDTO> generateDays(UserIntent intent, List<PoiCandidate> candidates) {
        List<ItineraryDayDTO> days = new ArrayList<>();
        LocalDate currentDate = intent.getStartDate();
        int candidateIndex = 0;

        for (int dayNumber = 1; dayNumber <= intent.getDays(); dayNumber++) {
            ItineraryDayDTO day = new ItineraryDayDTO();
            day.setDayId("day_" + UUID.randomUUID().toString().substring(0, 6));
            day.setDayNumber(dayNumber);
            day.setTravelDate(currentDate);
            day.setTitle(getDayTitle(intent.getDestination(), dayNumber));
            day.setNotes(getDayNotes(intent.getDestination(), dayNumber));

            List<ItineraryItemDTO> items = new ArrayList<>();
            int itemsPerDay = 2 + (dayNumber % 2);
            LocalTime currentTime = LocalTime.of(9, 0);

            for (int index = 0; index < itemsPerDay && candidateIndex < candidates.size(); index++) {
                PoiCandidate candidate = candidates.get(candidateIndex);
                ItineraryItemDTO item = toItineraryItem(candidate, currentTime, index);
                items.add(item);
                currentTime = currentTime.plusMinutes(candidate.getRecommendedDurationMinutes() + 60L);
                candidateIndex++;
            }

            day.setItems(items);
            days.add(day);
            currentDate = currentDate.plusDays(1);
        }

        return days;
    }

    private ItineraryItemDTO toItineraryItem(PoiCandidate candidate, LocalTime startTime, int orderIndex) {
        ItineraryItemDTO dto = new ItineraryItemDTO();
        dto.setItemId("item_" + UUID.randomUUID().toString().substring(0, 6));
        dto.setAttractionId(candidate.getCandidateId());
        dto.setAttractionName(candidate.getName());
        dto.setAddress(candidate.getAddress());
        dto.setLatitude(candidate.getLatitude());
        dto.setLongitude(candidate.getLongitude());
        dto.setDurationMinutes(candidate.getRecommendedDurationMinutes());
        dto.setOrderIndex(orderIndex);
        dto.setNotes("建议游览" + Math.max(1, candidate.getRecommendedDurationMinutes() / 60) + "小时");
        dto.setStartTime(startTime);
        dto.setEndTime(startTime.plusMinutes(candidate.getRecommendedDurationMinutes()));
        return dto;
    }

    private ItineraryDTO toItineraryDTO(ItineraryDraft draft) {
        ItineraryDTO itinerary = new ItineraryDTO();
        itinerary.setItineraryId(draft.getItineraryId());
        itinerary.setTitle(draft.getTitle());
        itinerary.setDestination(draft.getDestination());
        itinerary.setStartDate(draft.getStartDate());
        itinerary.setEndDate(draft.getEndDate());
        itinerary.setTravelerCount(draft.getTravelerCount());
        itinerary.setBudget(draft.getBudget());
        itinerary.setStatus(draft.getStatus());
        itinerary.setSummary(draft.getSummary());
        itinerary.setDays(draft.getDays());
        return itinerary;
    }

    private PoiCandidate toAmapCandidate(PoiDTO poi) {
        PoiCandidate candidate = new PoiCandidate();
        candidate.setCandidateId("poi_" + UUID.randomUUID().toString().substring(0, 8));
        candidate.setName(poi.getName());
        candidate.setAddress(poi.getAddress());
        candidate.setLatitude(poi.getLatitude());
        candidate.setLongitude(poi.getLongitude());
        candidate.setRecommendedDurationMinutes(120);
        candidate.setCategories(List.of());
        candidate.setSource("amap");
        return candidate;
    }

    private PoiCandidate toMockCandidate(MockAttractionDataProvider.MockAttraction attraction) {
        PoiCandidate candidate = new PoiCandidate();
        candidate.setCandidateId(attraction.getAttractionId());
        candidate.setName(attraction.getAttractionName());
        candidate.setAddress(attraction.getAddress());
        candidate.setLatitude(BigDecimal.valueOf(attraction.getLatitude()));
        candidate.setLongitude(BigDecimal.valueOf(attraction.getLongitude()));
        candidate.setRecommendedDurationMinutes(attraction.getDurationMinutes());
        candidate.setCategories(attraction.getCategories());
        candidate.setSource("mock");
        return candidate;
    }

    private String buildSummary(UserIntent intent, PlanningContext context, List<PoiCandidate> candidates) {
        String source = context.isUsedAmapData() ? "高德POI" : "Mock景点库";
        return String.format("%s%d日行程草案，基于%s生成，候选景点%d个。",
                intent.getDestination(), intent.getDays(), source, candidates.size());
    }

    private int calculateDayCount(LocalDate startDate, LocalDate endDate) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    private String normalizeInterest(String interest) {
        if (interest == null) {
            return "";
        }

        return INTEREST_ALIASES.getOrDefault(interest.trim().toLowerCase(), interest.trim().toLowerCase());
    }

    private String getDayTitle(String destination, int dayNumber) {
        switch (dayNumber) {
            case 1:
                return destination + "经典游";
            case 2:
                return destination + "深度游";
            case 3:
                return destination + "休闲游";
            default:
                return destination + "探索游";
        }
    }

    private String getDayNotes(String destination, int dayNumber) {
        switch (dayNumber) {
            case 1:
                return "第一天主要游览" + destination + "的经典景点";
            case 2:
                return "第二天深入体验" + destination + "的文化魅力";
            case 3:
                return "第三天放松休闲，享受慢时光";
            default:
                return "继续探索" + destination + "的更多精彩";
        }
    }

    private void updateTaskStatus(TravelPlanTask task, String status, int progress, String message) {
        task.setStatus(status);
        task.setProgress(progress);
        task.setErrorMessage(null);
        travelPlanTaskRepository.save(task);
    }

    private void simulateDelay(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
