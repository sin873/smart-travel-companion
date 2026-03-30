package com.yizhaoqi.smartpai.service.travel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yizhaoqi.smartpai.entity.travel.TravelPlanTask;
import com.yizhaoqi.smartpai.model.travel.*;
import com.yizhaoqi.smartpai.repository.travel.TravelPlanTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * TravelAgentService - 旅游规划 Agent 核心服务（Mock 版本）
 *
 * 当前为 Mock 实现，后续可替换为：
 * - AmapMcpTravelAgentService（高德 MCP 实现）
 * - LlmTravelAgentService（大模型编排实现）
 * - HybridTravelAgentService（混合实现）
 */
@Service
public class TravelAgentService {

    private static final Logger logger = LoggerFactory.getLogger(TravelAgentService.class);

    @Autowired
    private TravelPlanTaskRepository travelPlanTaskRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 执行行程规划（同步 Mock 版本）
     * 从 QUEUED -> PROCESSING -> COMPLETED/FAILED
     */
    @Transactional
    public void executePlan(String taskId, PlanItineraryRequest request, String userId) {
        logger.info("Starting plan execution - taskId: {}, destination: {}", taskId, request.getDestination());

        TravelPlanTask task = travelPlanTaskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        try {
            // 1. 更新状态为 QUEUED
            updateTaskStatus(task, "QUEUED", 0, "任务已创建，正在排队...");
            simulateDelay(100);

            // 2. 更新状态为 PROCESSING - 分析用户需求
            updateTaskStatus(task, "PROCESSING", 20, "正在分析用户需求...");
            simulateDelay(200);

            // 3. 验证城市是否支持
            String destination = request.getDestination();
            if (!MockAttractionDataProvider.isCitySupported(destination)) {
                throw new RuntimeException("暂不支持目的地：" + destination);
            }

            // 4. 更新状态 - 检索景点信息
            updateTaskStatus(task, "PROCESSING", 50, "正在检索景点信息...");
            simulateDelay(200);

            // 5. 筛选景点
            List<MockAttractionDataProvider.MockAttraction> filteredAttractions =
                    filterAttractions(destination, request.getPreferences());

            // 6. 更新状态 - 规划行程路线
            updateTaskStatus(task, "PROCESSING", 80, "正在规划行程路线...");
            simulateDelay(200);

            // 7. 生成完整 itinerary
            ItineraryDTO itinerary = generateItinerary(request, filteredAttractions);

            // 8. 序列化并保存结果
            String resultJson = objectMapper.writeValueAsString(itinerary);
            task.setResultJson(resultJson);

            // 9. 更新状态为 COMPLETED
            updateTaskStatus(task, "COMPLETED", 100, "行程规划完成！");

            logger.info("Plan completed successfully - taskId: {}, itineraryId: {}",
                    taskId, itinerary.getItineraryId());

        } catch (Exception e) {
            logger.error("Plan execution failed - taskId: {}", taskId, e);
            // 更新状态为 FAILED
            task.setStatus("FAILED");
            task.setProgress(0);
            task.setErrorMessage("规划失败：" + e.getMessage());
            travelPlanTaskRepository.save(task);
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 根据兴趣筛选景点
     */
    private List<MockAttractionDataProvider.MockAttraction> filterAttractions(
            String destination, PreferencesDTO preferences) {

        List<MockAttractionDataProvider.MockAttraction> allAttractions =
                MockAttractionDataProvider.getAttractionsByCity(destination);

        if (preferences == null || preferences.getInterests() == null || preferences.getInterests().isEmpty()) {
            // 没有偏好，返回前6个热门景点
            return allAttractions.stream().limit(6).collect(Collectors.toList());
        }

        List<String> interests = preferences.getInterests();

        // 根据兴趣筛选
        return allAttractions.stream()
                .filter(attraction -> {
                    for (String interest : interests) {
                        if (attraction.hasCategory(interest)) {
                            return true;
                        }
                    }
                    return false;
                })
                .limit(8) // 最多8个景点
                .collect(Collectors.toList());
    }

    /**
     * 生成完整 itinerary
     */
    private ItineraryDTO generateItinerary(
            PlanItineraryRequest request,
            List<MockAttractionDataProvider.MockAttraction> attractions) {

        ItineraryDTO itinerary = new ItineraryDTO();

        // 生成 itineraryId
        String itineraryId = "itin_" + UUID.randomUUID().toString().substring(0, 8);
        itinerary.setItineraryId(itineraryId);

        // 基本信息
        itinerary.setTitle(request.getDestination() + " " + calculateDayCount(request) + "日游");
        itinerary.setDestination(request.getDestination());
        itinerary.setStartDate(LocalDate.parse(request.getStartDate()));
        itinerary.setEndDate(LocalDate.parse(request.getEndDate()));
        itinerary.setTravelerCount(request.getTravelerCount() != null ? request.getTravelerCount() : 2);
        itinerary.setBudget(request.getBudget() != null ? java.math.BigDecimal.valueOf(request.getBudget()) : null);
        itinerary.setStatus("PLANNED");

        // 生成每日行程
        List<ItineraryDayDTO> days = generateDays(request, attractions);
        itinerary.setDays(days);

        return itinerary;
    }

    /**
     * 生成每日行程
     */
    private List<ItineraryDayDTO> generateDays(
            PlanItineraryRequest request,
            List<MockAttractionDataProvider.MockAttraction> attractions) {

        List<ItineraryDayDTO> days = new ArrayList<>();
        int dayCount = calculateDayCount(request);
        LocalDate currentDate = LocalDate.parse(request.getStartDate());

        // 每个景点分配索引
        int attractionIndex = 0;

        for (int i = 1; i <= dayCount; i++) {
            ItineraryDayDTO day = new ItineraryDayDTO();
            day.setDayId("day_" + UUID.randomUUID().toString().substring(0, 6));
            day.setDayNumber(i);
            day.setTravelDate(currentDate);

            // 当天标题
            day.setTitle(getDayTitle(request.getDestination(), i));
            day.setNotes(getDayNotes(request.getDestination(), i));

            // 分配2-3个景点到当天
            List<ItineraryItemDTO> dayItems = new ArrayList<>();
            int itemsPerDay = 2 + (i % 2); // 交替2-3个

            LocalTime currentTime = LocalTime.of(9, 0); // 从9:00开始
            int orderIndex = 0;

            for (int j = 0; j < itemsPerDay && attractionIndex < attractions.size(); j++) {
                MockAttractionDataProvider.MockAttraction attraction = attractions.get(attractionIndex);
                ItineraryItemDTO item = convertToItemDTO(attraction, currentTime, orderIndex++);
                dayItems.add(item);

                // 更新下一个时间（加上当前时长 + 1小时缓冲）
                currentTime = currentTime.plusMinutes(attraction.getDurationMinutes() + 60);
                attractionIndex++;
            }

            day.setItems(dayItems);
            days.add(day);

            currentDate = currentDate.plusDays(1);
        }

        return days;
    }

    /**
     * 转换为 ItineraryItemDTO
     */
    private ItineraryItemDTO convertToItemDTO(
            MockAttractionDataProvider.MockAttraction attraction,
            LocalTime startTime,
            int orderIndex) {

        ItineraryItemDTO dto = new ItineraryItemDTO();
        dto.setItemId("item_" + UUID.randomUUID().toString().substring(0, 6));
        dto.setAttractionId(attraction.getAttractionId());
        dto.setAttractionName(attraction.getAttractionName());
        dto.setAddress(attraction.getAddress());
        dto.setLatitude(java.math.BigDecimal.valueOf(attraction.getLatitude()));
        dto.setLongitude(java.math.BigDecimal.valueOf(attraction.getLongitude()));
        dto.setDurationMinutes(attraction.getDurationMinutes());
        dto.setOrderIndex(orderIndex);
        dto.setNotes("建议游览" + (attraction.getDurationMinutes() / 60) + "小时");

        // 设置时间
        dto.setStartTime(startTime);

        LocalTime endTime = startTime.plusMinutes(attraction.getDurationMinutes());
        dto.setEndTime(endTime);

        return dto;
    }

    /**
     * 计算天数
     */
    private int calculateDayCount(PlanItineraryRequest request) {
        LocalDate startDate = LocalDate.parse(request.getStartDate());
        LocalDate endDate = LocalDate.parse(request.getEndDate());
        return (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    /**
     * 获取每日标题
     */
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

    /**
     * 获取每日备注
     */
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

    /**
     * 更新任务状态
     */
    private void updateTaskStatus(TravelPlanTask task, String status, int progress, String message) {
        task.setStatus(status);
        task.setProgress(progress);
        travelPlanTaskRepository.save(task);
    }

    /**
     * 模拟延迟（为了演示效果）
     */
    private void simulateDelay(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
