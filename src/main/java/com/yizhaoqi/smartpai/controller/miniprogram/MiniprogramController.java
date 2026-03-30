package com.yizhaoqi.smartpai.controller.miniprogram;

import com.yizhaoqi.smartpai.model.miniprogram.*;
import com.yizhaoqi.smartpai.service.miniprogram.MiniprogramAuthService;
import com.yizhaoqi.smartpai.service.miniprogram.TravelAgentService;
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
@RequestMapping("/api/v1/miniprogram")
public class MiniprogramController {

    private static final Logger logger = LoggerFactory.getLogger(MiniprogramController.class);

    @Autowired
    private MiniprogramAuthService authService;

    @Autowired
    private TravelAgentService travelAgentService;

    @Autowired
    private JwtUtils jwtUtils;

    // ==================== 认证相关接口 ====================

    /**
     * 微信小程序登录
     */
    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> wxLogin(@RequestBody WxLoginRequest request) {
        try {
            WxLoginResponse response = authService.wxLogin(request);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", response);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Wx login failed", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", e.getMessage());
            
            return ResponseEntity.status(500).body(error);
        }
    }

    // ==================== 行程规划相关接口 ====================

    /**
     * 创建行程规划任务
     */
    @PostMapping("/itinerary/plan")
    public ResponseEntity<Map<String, Object>> planItinerary(
            @RequestBody PlanItineraryRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String userId = extractUserId(authHeader);
            PlanItineraryResponse response = travelAgentService.createPlanTask(request, userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", response);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Plan itinerary failed", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", e.getMessage());
            
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 查询规划任务状态
     */
    @GetMapping("/itinerary/plan/{taskId}")
    public ResponseEntity<Map<String, Object>> getPlanStatus(
            @PathVariable String taskId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            extractUserId(authHeader); // 验证token
            PlanTaskStatusResponse response = travelAgentService.getTaskStatus(taskId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", response);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Get plan status failed", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", e.getMessage());
            
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 保存行程
     */
    @PostMapping("/itinerary")
    public ResponseEntity<Map<String, Object>> saveItinerary(
            @RequestBody ItineraryDTO itinerary,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String userId = extractUserId(authHeader);
            String itineraryId = travelAgentService.saveItinerary(itinerary, userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", Map.of("itineraryId", itineraryId));
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Save itinerary failed", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", e.getMessage());
            
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 获取用户行程列表
     */
    @GetMapping("/itinerary")
    public ResponseEntity<Map<String, Object>> getItineraryList(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String userId = extractUserId(authHeader);
            List<ItineraryDTO> itineraries = travelAgentService.getUserItineraries(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", Map.of(
                "total", itineraries.size(),
                "items", itineraries
            ));
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Get itinerary list failed", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", e.getMessage());
            
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 获取行程详情
     */
    @GetMapping("/itinerary/{itineraryId}")
    public ResponseEntity<Map<String, Object>> getItineraryDetail(
            @PathVariable String itineraryId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            extractUserId(authHeader); // 验证token
            ItineraryDTO itinerary = travelAgentService.getItineraryDetail(itineraryId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", itinerary);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Get itinerary detail failed", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", e.getMessage());
            
            return ResponseEntity.status(500).body(error);
        }
    }

    // ==================== 私有方法 ====================

    private String extractUserId(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtUtils.extractUserIdFromToken(token);
        }
        // 为了演示,返回一个默认用户ID
        return "demo_user_001";
    }
}
