package com.yizhaoqi.smartpai.controller.travel;

import com.yizhaoqi.smartpai.model.travel.*;
import com.yizhaoqi.smartpai.service.travel.TravelPlanService;
import com.yizhaoqi.smartpai.service.travel.ItineraryService;
import com.yizhaoqi.smartpai.utils.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/travel")
public class TravelController {

    private static final Logger logger = LoggerFactory.getLogger(TravelController.class);

    @Autowired
    private TravelPlanService travelPlanService;

    @Autowired
    private ItineraryService itineraryService;

    @Autowired
    private JwtUtils jwtUtils;

    // ==================== 行程规划相关接口 ====================

    /**
     * 1. 生成行程 - POST /api/v1/travel/plan
     * 接收 PlanItineraryRequest，创建任务记录，返回 taskId
     */
    @PostMapping("/plan")
    public ResponseEntity<Map<String, Object>> planItinerary(
            @RequestBody PlanItineraryRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String userId = extractUserId(authHeader);
            logger.info("Plan itinerary request - userId: '{}', destination: {}", userId, request.getDestination());
            
            // 打印请求参数
            logger.info("Request params: startDate={}, endDate={}, travelerCount={}, budget={}", 
                request.getStartDate(), request.getEndDate(), request.getTravelerCount(), request.getBudget());
            
            PlanItineraryResponse response = travelPlanService.createPlanTask(request, userId);

            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", response);
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Plan itinerary failed - destination: {}, error: {}", 
                request != null ? request.getDestination() : "unknown", e.getMessage(), e);

            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", e.getMessage());
            error.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 2. 查询规划状态 - GET /api/v1/travel/plan/{taskId}
     * 根据 taskId 查询任务状态和结果
     */
    @GetMapping("/plan/{taskId}")
    public ResponseEntity<Map<String, Object>> getPlanStatus(
            @PathVariable String taskId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            extractUserId(authHeader); // 验证token
            logger.info("Query plan status - taskId: {}", taskId);
            
            PlanTaskStatusResponse response = travelPlanService.getTaskStatus(taskId);

            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", response);
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Get plan status failed", e);

            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", e.getMessage());
            error.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(500).body(error);
        }
    }

    // ==================== 行程管理相关接口 ====================

    /**
     * 3. 保存行程 - POST /api/v1/travel/itineraries
     * 保存完整行程（主表+day+item）
     */
    @PostMapping("/itineraries")
    public ResponseEntity<Map<String, Object>> saveItinerary(
            @RequestBody ItineraryDTO itinerary,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String userId = extractUserId(authHeader);
            logger.info("========== 【DEBUG Controller】收到保存行程请求 ==========");
            logger.info("【DEBUG Controller】userId: {}", userId);
            logger.info("【DEBUG Controller】接收到的完整 itinerary: {}", itinerary);

            String itineraryId = itineraryService.saveItinerary(itinerary, userId);

            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", Map.of("itineraryId", itineraryId));
            result.put("timestamp", System.currentTimeMillis());

            logger.info("【DEBUG Controller】保存成功，返回 itineraryId: {}", itineraryId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("【DEBUG Controller】保存失败", e);

            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", e.getMessage());
            error.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 4. 查询行程列表 - GET /api/v1/travel/itineraries
     * 获取当前用户的行程列表，按创建时间倒序
     */
    @GetMapping("/itineraries")
    public ResponseEntity<Map<String, Object>> getItineraryList(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String userId = extractUserId(authHeader);
            logger.info("Get itinerary list - userId: {}", userId);
            
            List<ItineraryDTO> itineraries = itineraryService.getUserItineraries(userId);

            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", Map.of(
                "total", itineraries.size(),
                "items", itineraries
            ));
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Get itinerary list failed", e);

            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", e.getMessage());
            error.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 5. 查询行程详情 - GET /api/v1/travel/itineraries/{itineraryId}
     * 获取完整行程详情（包含days和items）
     */
    @GetMapping("/itineraries/{itineraryId}")
    public ResponseEntity<Map<String, Object>> getItineraryDetail(
            @PathVariable String itineraryId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            extractUserId(authHeader); // 验证token
            logger.info("Get itinerary detail - itineraryId: {}", itineraryId);
            
            ItineraryDTO itinerary = itineraryService.getItineraryDetail(itineraryId);

            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", itinerary);
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Get itinerary detail failed", e);

            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", e.getMessage());
            error.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 6. 删除行程 - DELETE /api/v1/travel/itineraries/{itineraryId}
     * 删除行程及其关联的day和item
     */
    @DeleteMapping("/itineraries/{itineraryId}")
    public ResponseEntity<Map<String, Object>> deleteItinerary(
            @PathVariable String itineraryId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String userId = extractUserId(authHeader);
            logger.info("Delete itinerary - userId: {}, itineraryId: {}", userId, itineraryId);
            
            itineraryService.deleteItinerary(itineraryId, userId);

            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", null);
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Delete itinerary failed", e);

            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", e.getMessage());
            error.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(500).body(error);
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 从 Authorization header 中提取 userId
     * 如果没有 token 或解析失败，返回演示用户 demo_user_001
     */
    private String extractUserId(String authHeader) {
        logger.debug("Extracting userId from authHeader: {}", authHeader != null ? "present" : "null");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                logger.debug("Token extracted, length: {}", token.length());
                String userId = jwtUtils.extractUserIdFromToken(token);
                // 如果从 token 中提取 userId 失败，返回演示用户
                if (userId == null || userId.isBlank()) {
                    logger.warn("Failed to extract userId from token (userId={}), using demo user", userId);
                    return "demo_user_001";
                }
                logger.info("Successfully extracted userId from token: {}", userId);
                return userId;
            } catch (Exception e) {
                logger.warn("Failed to extract userId from token, using demo user", e);
            }
        }
        logger.info("No valid auth header, using demo user: demo_user_001");
        // 没有 token 时，使用演示用户
        return "demo_user_001";
    }
}
