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
import java.util.UUID;

@Service
public class TravelPlanService {

    private static final Logger logger = LoggerFactory.getLogger(TravelPlanService.class);

    @Autowired
    private TravelPlanTaskRepository travelPlanTaskRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 创建行程规划任务
     * 本步骤只创建任务记录，不真正生成行程（占位实现）
     */
    @Transactional
    public PlanItineraryResponse createPlanTask(PlanItineraryRequest request, String userId) {
        logger.info("Creating plan task - userId: {}, destination: {}", userId, request.getDestination());

        // 1. 校验必要字段
        validatePlanRequest(request);

        // 2. 生成 taskId
        String taskId = "task_" + UUID.randomUUID().toString().substring(0, 8);

        // 3. 创建任务记录
        TravelPlanTask task = new TravelPlanTask();
        task.setTaskId(taskId);
        task.setUserId(userId);
        task.setDestination(request.getDestination());
        task.setStartDate(LocalDate.parse(request.getStartDate()));
        task.setEndDate(LocalDate.parse(request.getEndDate()));
        task.setTravelerCount(request.getTravelerCount() != null ? request.getTravelerCount() : 2);
        task.setBudget(request.getBudget());
        
        // 保存偏好设置为JSON
        try {
            if (request.getPreferences() != null) {
                task.setPreferencesJson(objectMapper.writeValueAsString(request.getPreferences()));
            }
        } catch (Exception e) {
            logger.warn("Failed to serialize preferences", e);
        }

        // 初始状态设为 PROCESSING
        task.setStatus("PROCESSING");
        task.setProgress(0);
        task.setErrorMessage(null);
        task.setResultJson(null);

        travelPlanTaskRepository.save(task);
        logger.info("Plan task created - taskId: {}", taskId);

        // TODO: 下一步接入 Agent 核心逻辑时，这里触发异步任务
        // asyncPlanItinerary(taskId, request, userId);

        // 4. 返回响应
        PlanItineraryResponse response = new PlanItineraryResponse();
        response.setTaskId(taskId);
        response.setStatus("PROCESSING");
        response.setEstimatedTime(15); // 预计15秒（占位值）

        return response;
    }

    /**
     * 查询规划任务状态
     */
    public PlanTaskStatusResponse getTaskStatus(String taskId) {
        logger.info("Querying task status - taskId: {}", taskId);

        TravelPlanTask task = travelPlanTaskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        PlanTaskStatusResponse response = new PlanTaskStatusResponse();
        response.setTaskId(task.getTaskId());
        response.setStatus(task.getStatus());
        response.setProgress(task.getProgress() != null ? task.getProgress() : 0);
        response.setMessage(getStatusMessage(task));

        // 如果有结果数据，解析并放入 itinerary
        if ("COMPLETED".equals(task.getStatus()) && task.getResultJson() != null) {
            try {
                ItineraryDTO itinerary = objectMapper.readValue(task.getResultJson(), ItineraryDTO.class);
                response.setItinerary(itinerary);
            } catch (Exception e) {
                logger.error("Failed to parse result JSON", e);
                response.setItinerary(null);
            }
        } else {
            response.setItinerary(null);
        }

        return response;
    }

    // ==================== 私有方法 ====================

    /**
     * 校验规划请求的必要字段
     */
    private void validatePlanRequest(PlanItineraryRequest request) {
        if (request.getDestination() == null || request.getDestination().isBlank()) {
            throw new IllegalArgumentException("目的地不能为空");
        }
        if (request.getStartDate() == null || request.getStartDate().isBlank()) {
            throw new IllegalArgumentException("开始日期不能为空");
        }
        if (request.getEndDate() == null || request.getEndDate().isBlank()) {
            throw new IllegalArgumentException("结束日期不能为空");
        }
        if (request.getTravelerCount() == null || request.getTravelerCount() <= 0) {
            throw new IllegalArgumentException("出行人数必须大于0");
        }

        // 校验日期格式
        try {
            LocalDate startDate = LocalDate.parse(request.getStartDate());
            LocalDate endDate = LocalDate.parse(request.getEndDate());
            if (endDate.isBefore(startDate)) {
                throw new IllegalArgumentException("结束日期不能早于开始日期");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("日期格式错误，请使用 YYYY-MM-DD 格式");
        }
    }

    /**
     * 根据任务状态返回友好的提示消息
     */
    private String getStatusMessage(TravelPlanTask task) {
        String status = task.getStatus();
        if (task.getErrorMessage() != null && !task.getErrorMessage().isBlank()) {
            return task.getErrorMessage();
        }
        
        switch (status) {
            case "QUEUED":
                return "任务已创建，正在排队...";
            case "PROCESSING":
                Integer progress = task.getProgress();
                if (progress == null || progress < 30) {
                    return "正在分析用户需求...";
                } else if (progress < 60) {
                    return "正在检索景点信息...";
                } else if (progress < 90) {
                    return "正在规划行程路线...";
                } else {
                    return "正在生成完整行程...";
                }
            case "COMPLETED":
                return "行程规划完成！";
            case "FAILED":
                return "规划失败";
            default:
                return "未知状态";
        }
    }
}
