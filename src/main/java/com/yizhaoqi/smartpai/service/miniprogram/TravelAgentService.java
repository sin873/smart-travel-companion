package com.yizhaoqi.smartpai.service.miniprogram;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yizhaoqi.smartpai.entity.miniprogram.*;
import com.yizhaoqi.smartpai.model.miniprogram.*;
import com.yizhaoqi.smartpai.repository.miniprogram.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class TravelAgentService {

    private static final Logger logger = LoggerFactory.getLogger(TravelAgentService.class);

    @Autowired
    private PlanTaskRepository planTaskRepository;

    @Autowired
    private ItineraryRepository itineraryRepository;

    @Autowired
    private ItineraryDayRepository itineraryDayRepository;

    @Autowired
    private ItineraryAttractionRepository itineraryAttractionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // 示例景点数据（实际应从RAG知识库获取）
    private static final Map<String, List<AttractionInfo>> MOCK_ATTRACTIONS = new HashMap<>();

    static {
        // 北京景点
        List<AttractionInfo> beijingAttractions = Arrays.asList(
            new AttractionInfo("gugong", "故宫博物院", "北京市东城区景山前街4号", 
                new BigDecimal("39.9163"), new BigDecimal("116.3972"), 180, "historical"),
            new AttractionInfo("tiananmen", "天安门广场", "北京市东城区", 
                new BigDecimal("39.9087"), new BigDecimal("116.3975"), 60, "historical"),
            new AttractionInfo("summerpalace", "颐和园", "北京市海淀区新建宫门路19号", 
                new BigDecimal("39.9999"), new BigDecimal("116.2766"), 180, "natural"),
            new AttractionInfo("greatwall", "八达岭长城", "北京市延庆区八达岭镇", 
                new BigDecimal("40.3579"), new BigDecimal("116.0213"), 240, "historical"),
            new AttractionInfo("wangfujing", "王府井大街", "北京市东城区", 
                new BigDecimal("39.9147"), new BigDecimal("116.4108"), 120, "shopping"),
            new AttractionInfo("nanluoguxiang", "南锣鼓巷", "北京市东城区", 
                new BigDecimal("39.9368"), new BigDecimal("116.4039"), 90, "cultural"),
            new AttractionInfo("yuanmingyuan", "圆明园遗址公园", "北京市海淀区清华西路28号", 
                new BigDecimal("40.0058"), new BigDecimal("116.3116"), 120, "historical"),
            new AttractionInfo("tempofheaven", "天坛公园", "北京市东城区天坛路甲1号", 
                new BigDecimal("39.8882"), new BigDecimal("116.4171"), 120, "historical")
        );
        MOCK_ATTRACTIONS.put("北京", beijingAttractions);
        MOCK_ATTRACTIONS.put("Beijing", beijingAttractions);
    }

    /**
     * 创建行程规划任务
     */
    @Transactional
    public PlanItineraryResponse createPlanTask(PlanItineraryRequest request, String userId) {
        String taskId = "task_" + UUID.randomUUID().toString().substring(0, 8);
        
        PlanTask task = new PlanTask();
        task.setTaskId(taskId);
        task.setUserId(userId);
        try {
            task.setRequestData(objectMapper.writeValueAsString(request));
        } catch (Exception e) {
            logger.error("Failed to serialize request", e);
        }
        task.setStatus("QUEUED");
        task.setProgress(0);
        task.setMessage("任务已创建，正在排队...");
        planTaskRepository.save(task);

        // 异步执行规划
        asyncPlanItinerary(taskId, request, userId);

        PlanItineraryResponse response = new PlanItineraryResponse();
        response.setTaskId(taskId);
        response.setStatus("QUEUED");
        response.setEstimatedTime(15); // 预计15秒
        return response;
    }

    /**
     * 异步执行行程规划
     */
    @Async
    public CompletableFuture<Void> asyncPlanItinerary(String taskId, PlanItineraryRequest request, String userId) {
        return CompletableFuture.runAsync(() -> {
            try {
                updateTaskStatus(taskId, "PROCESSING", 10, "开始分析用户需求...");
                Thread.sleep(1000);

                updateTaskStatus(taskId, "PROCESSING", 30, "正在检索景点信息...");
                List<AttractionInfo> attractions = searchAttractions(request.getDestination());
                Thread.sleep(1500);

                updateTaskStatus(taskId, "PROCESSING", 50, "正在筛选景点...");
                List<AttractionInfo> filteredAttractions = filterAttractions(attractions, request.getPreferences());
                Thread.sleep(1000);

                updateTaskStatus(taskId, "PROCESSING", 70, "正在规划行程路线...");
                LocalDate startDate = LocalDate.parse(request.getStartDate());
                LocalDate endDate = LocalDate.parse(request.getEndDate());
                int days = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
                List<ItineraryDayDTO> dayPlans = optimizeRoute(filteredAttractions, days, startDate, request.getPreferences());
                Thread.sleep(1500);

                updateTaskStatus(taskId, "PROCESSING", 90, "正在生成完整行程...");
                ItineraryDTO itinerary = buildItinerary(dayPlans, request, userId);
                Thread.sleep(1000);

                updateTaskStatus(taskId, "COMPLETED", 100, "行程规划完成！");
                try {
                    PlanTask task = planTaskRepository.findByTaskId(taskId).orElse(null);
                    if (task != null) {
                        task.setResultData(objectMapper.writeValueAsString(itinerary));
                        planTaskRepository.save(task);
                    }
                } catch (Exception e) {
                    logger.error("Failed to save result", e);
                }

            } catch (Exception e) {
                logger.error("Plan failed", e);
                updateTaskStatus(taskId, "FAILED", 0, "规划失败: " + e.getMessage());
            }
        });
    }

    /**
     * 查询任务状态
     */
    public PlanTaskStatusResponse getTaskStatus(String taskId) {
        PlanTask task = planTaskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        PlanTaskStatusResponse response = new PlanTaskStatusResponse();
        response.setTaskId(task.getTaskId());
        response.setStatus(task.getStatus());
        response.setProgress(task.getProgress());
        response.setMessage(task.getMessage());

        if ("COMPLETED".equals(task.getStatus()) && task.getResultData() != null) {
            try {
                ItineraryDTO itinerary = objectMapper.readValue(task.getResultData(), ItineraryDTO.class);
                response.setItinerary(itinerary);
            } catch (Exception e) {
                logger.error("Failed to parse result", e);
            }
        }

        return response;
    }

    /**
     * 保存行程
     */
    @Transactional
    public String saveItinerary(ItineraryDTO itineraryDTO, String userId) {
        String itineraryId = "itinerary_" + UUID.randomUUID().toString().substring(0, 8);

        // 保存行程主表
        Itinerary itinerary = new Itinerary();
        itinerary.setItineraryId(itineraryId);
        itinerary.setUserId(userId);
        itinerary.setTitle(itineraryDTO.getTitle());
        itinerary.setDestination(itineraryDTO.getDestination());
        itinerary.setStartDate(itineraryDTO.getStartDate());
        itinerary.setEndDate(itineraryDTO.getEndDate());
        itinerary.setTravelerCount(itineraryDTO.getTravelerCount());
        itinerary.setBudget(itineraryDTO.getBudget());
        itinerary.setStatus("PLANNED");
        itineraryRepository.save(itinerary);

        // 保存每日行程和景点
        if (itineraryDTO.getDays() != null) {
            for (ItineraryDayDTO dayDTO : itineraryDTO.getDays()) {
                String dayId = "day_" + UUID.randomUUID().toString().substring(0, 8);

                ItineraryDay day = new ItineraryDay();
                day.setDayId(dayId);
                day.setItineraryId(itineraryId);
                day.setDayNumber(dayDTO.getDayNumber());
                day.setDate(dayDTO.getDate());
                day.setTitle(dayDTO.getTitle());
                day.setNotes(dayDTO.getNotes());
                itineraryDayRepository.save(day);

                if (dayDTO.getAttractions() != null) {
                    int orderIndex = 0;
                    for (ItineraryAttractionDTO attrDTO : dayDTO.getAttractions()) {
                        String itemId = "item_" + UUID.randomUUID().toString().substring(0, 8);

                        ItineraryAttraction attr = new ItineraryAttraction();
                        attr.setItemId(itemId);
                        attr.setDayId(dayId);
                        attr.setAttractionId(attrDTO.getAttractionId());
                        attr.setAttractionName(attrDTO.getAttractionName());
                        attr.setAddress(attrDTO.getAddress());
                        attr.setLatitude(attrDTO.getLatitude());
                        attr.setLongitude(attrDTO.getLongitude());
                        attr.setStartTime(attrDTO.getStartTime());
                        attr.setEndTime(attrDTO.getEndTime());
                        attr.setDurationMinutes(attrDTO.getDurationMinutes());
                        attr.setOrderIndex(orderIndex++);
                        attr.setNotes(attrDTO.getNotes());
                        itineraryAttractionRepository.save(attr);
                    }
                }
            }
        }

        return itineraryId;
    }

    /**
     * 获取用户行程列表
     */
    public List<ItineraryDTO> getUserItineraries(String userId) {
        List<Itinerary> itineraries = itineraryRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<ItineraryDTO> result = new ArrayList<>();

        for (Itinerary itinerary : itineraries) {
            ItineraryDTO dto = new ItineraryDTO();
            dto.setItineraryId(itinerary.getItineraryId());
            dto.setTitle(itinerary.getTitle());
            dto.setDestination(itinerary.getDestination());
            dto.setStartDate(itinerary.getStartDate());
            dto.setEndDate(itinerary.getEndDate());
            dto.setTravelerCount(itinerary.getTravelerCount());
            dto.setBudget(itinerary.getBudget());
            dto.setStatus(itinerary.getStatus());
            result.add(dto);
        }

        return result;
    }

    /**
     * 获取行程详情
     */
    public ItineraryDTO getItineraryDetail(String itineraryId) {
        Itinerary itinerary = itineraryRepository.findByItineraryId(itineraryId)
                .orElseThrow(() -> new RuntimeException("Itinerary not found"));

        ItineraryDTO dto = new ItineraryDTO();
        dto.setItineraryId(itinerary.getItineraryId());
        dto.setTitle(itinerary.getTitle());
        dto.setDestination(itinerary.getDestination());
        dto.setStartDate(itinerary.getStartDate());
        dto.setEndDate(itinerary.getEndDate());
        dto.setTravelerCount(itinerary.getTravelerCount());
        dto.setBudget(itinerary.getBudget());
        dto.setStatus(itinerary.getStatus());

        // 加载每日行程
        List<ItineraryDay> days = itineraryDayRepository.findByItineraryIdOrderByDayNumberAsc(itineraryId);
        List<ItineraryDayDTO> dayDTOs = new ArrayList<>();

        for (ItineraryDay day : days) {
            ItineraryDayDTO dayDTO = new ItineraryDayDTO();
            dayDTO.setDayId(day.getDayId());
            dayDTO.setDayNumber(day.getDayNumber());
            dayDTO.setDate(day.getDate());
            dayDTO.setTitle(day.getTitle());
            dayDTO.setNotes(day.getNotes());

            // 加载景点
            List<ItineraryAttraction> attractions = itineraryAttractionRepository.findByDayIdOrderByOrderIndexAsc(day.getDayId());
            List<ItineraryAttractionDTO> attrDTOs = new ArrayList<>();

            for (ItineraryAttraction attr : attractions) {
                ItineraryAttractionDTO attrDTO = new ItineraryAttractionDTO();
                attrDTO.setItemId(attr.getItemId());
                attrDTO.setAttractionId(attr.getAttractionId());
                attrDTO.setAttractionName(attr.getAttractionName());
                attrDTO.setAddress(attr.getAddress());
                attrDTO.setLatitude(attr.getLatitude());
                attrDTO.setLongitude(attr.getLongitude());
                attrDTO.setStartTime(attr.getStartTime());
                attrDTO.setEndTime(attr.getEndTime());
                attrDTO.setDurationMinutes(attr.getDurationMinutes());
                attrDTO.setNotes(attr.getNotes());
                attrDTOs.add(attrDTO);
            }

            dayDTO.setAttractions(attrDTOs);
            dayDTOs.add(dayDTO);
        }

        dto.setDays(dayDTOs);
        return dto;
    }

    // ==================== 私有方法 ====================

    private void updateTaskStatus(String taskId, String status, int progress, String message) {
        PlanTask task = planTaskRepository.findByTaskId(taskId).orElse(null);
        if (task != null) {
            task.setStatus(status);
            task.setProgress(progress);
            task.setMessage(message);
            planTaskRepository.save(task);
        }
    }

    private List<AttractionInfo> searchAttractions(String destination) {
        return MOCK_ATTRACTIONS.getOrDefault(destination, new ArrayList<>());
    }

    private List<AttractionInfo> filterAttractions(List<AttractionInfo> attractions, PlanItineraryRequest.UserPreferences preferences) {
        if (attractions == null || attractions.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> interests = preferences != null ? preferences.getInterests() : new ArrayList<>();
        if (interests == null || interests.isEmpty()) {
            return new ArrayList<>(attractions);
        }

        List<AttractionInfo> filtered = new ArrayList<>();
        for (AttractionInfo attraction : attractions) {
            if (interests.contains(attraction.category)) {
                filtered.add(attraction);
            }
        }

        // 如果没有匹配的,返回所有
        return filtered.isEmpty() ? new ArrayList<>(attractions) : filtered;
    }

    private List<ItineraryDayDTO> optimizeRoute(List<AttractionInfo> attractions, int days, LocalDate startDate, PlanItineraryRequest.UserPreferences preferences) {
        List<ItineraryDayDTO> result = new ArrayList<>();
        
        if (attractions.isEmpty()) {
            return result;
        }

        // 简单的分配策略: 平均分配景点到每一天
        int attractionsPerDay = Math.max(1, attractions.size() / days);
        String pace = preferences != null ? preferences.getPace() : "moderate";
        
        // 根据节奏调整每天景点数量
        if ("relaxed".equals(pace)) {
            attractionsPerDay = Math.max(1, attractionsPerDay - 1);
        } else if ("intensive".equals(pace)) {
            attractionsPerDay = Math.min(attractions.size(), attractionsPerDay + 1);
        }

        int attrIndex = 0;
        for (int i = 0; i < days && attrIndex < attractions.size(); i++) {
            ItineraryDayDTO day = new ItineraryDayDTO();
            day.setDayId("day_" + (i + 1));
            day.setDayNumber(i + 1);
            day.setDate(startDate.plusDays(i));
            day.setTitle("第" + (i + 1) + "天");
            day.setNotes("精彩的一天!");

            List<ItineraryAttractionDTO> dayAttractions = new ArrayList<>();
            LocalTime currentTime = LocalTime.of(9, 0); // 早上9点开始

            int todayCount = 0;
            while (todayCount < attractionsPerDay && attrIndex < attractions.size()) {
                AttractionInfo info = attractions.get(attrIndex);
                
                ItineraryAttractionDTO attr = new ItineraryAttractionDTO();
                attr.setItemId("attr_" + attrIndex);
                attr.setAttractionId(info.id);
                attr.setAttractionName(info.name);
                attr.setAddress(info.address);
                attr.setLatitude(info.latitude);
                attr.setLongitude(info.longitude);
                attr.setStartTime(currentTime);
                attr.setDurationMinutes(info.durationMinutes);
                attr.setEndTime(currentTime.plusMinutes(info.durationMinutes));
                attr.setNotes("推荐游玩时间约" + info.durationMinutes + "分钟");
                
                dayAttractions.add(attr);
                
                // 更新时间（加上游玩时间和30分钟交通时间）
                currentTime = currentTime.plusMinutes(info.durationMinutes + 30);
                
                attrIndex++;
                todayCount++;
            }

            day.setAttractions(dayAttractions);
            result.add(day);
        }

        return result;
    }

    private ItineraryDTO buildItinerary(List<ItineraryDayDTO> days, PlanItineraryRequest request, String userId) {
        ItineraryDTO itinerary = new ItineraryDTO();
        itinerary.setItineraryId("temp_" + UUID.randomUUID().toString().substring(0, 8));
        itinerary.setTitle(request.getDestination() + " " + request.getStartDate() + " 行程");
        itinerary.setDestination(request.getDestination());
        itinerary.setStartDate(LocalDate.parse(request.getStartDate()));
        itinerary.setEndDate(LocalDate.parse(request.getEndDate()));
        itinerary.setTravelerCount(request.getTravelerCount());
        itinerary.setBudget(request.getBudget());
        itinerary.setStatus("PLANNED");
        itinerary.setDays(days);
        return itinerary;
    }

    // 景点信息内部类
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    private static class AttractionInfo {
        String id;
        String name;
        String address;
        BigDecimal latitude;
        BigDecimal longitude;
        int durationMinutes;
        String category;
    }
}
