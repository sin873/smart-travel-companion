package com.yizhaoqi.smartpai.service.travel;

import com.yizhaoqi.smartpai.config.TravelAgentProperties;
import com.yizhaoqi.smartpai.entity.travel.TravelPlanTask;
import com.yizhaoqi.smartpai.model.travel.ItineraryDTO;
import com.yizhaoqi.smartpai.model.travel.ItineraryDayDTO;
import com.yizhaoqi.smartpai.model.travel.ItineraryDraft;
import com.yizhaoqi.smartpai.model.travel.ItineraryItemDTO;
import com.yizhaoqi.smartpai.model.travel.PlanItineraryRequest;
import com.yizhaoqi.smartpai.model.travel.PlanningContext;
import com.yizhaoqi.smartpai.model.travel.UserIntent;
import com.yizhaoqi.smartpai.repository.travel.TravelPlanTaskRepository;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Service
public class LangChain4jTravelAgentService {

    private static final Logger logger = LoggerFactory.getLogger(LangChain4jTravelAgentService.class);

    private final TravelPlanTaskRepository travelPlanTaskRepository;
    private final TravelAgentService legacyTravelAgentService;
    private final TravelPlanningTools travelPlanningTools;
    private final TravelAgentProperties travelAgentProperties;

    public LangChain4jTravelAgentService(TravelPlanTaskRepository travelPlanTaskRepository,
                                         TravelAgentService legacyTravelAgentService,
                                         TravelPlanningTools travelPlanningTools,
                                         TravelAgentProperties travelAgentProperties) {
        this.travelPlanTaskRepository = travelPlanTaskRepository;
        this.legacyTravelAgentService = legacyTravelAgentService;
        this.travelPlanningTools = travelPlanningTools;
        this.travelAgentProperties = travelAgentProperties;
    }

    @Transactional
    public void executePlan(String taskId, PlanItineraryRequest request, String userId) {
        logger.info("Starting LangChain4j travel agent flow, taskId={}, destination={}, userId={}",
                taskId, request.getDestination(), userId);

        TravelPlanTask task = travelPlanTaskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        try {
            updateTaskStatus(task, "QUEUED", 0);

            UserIntent intent = legacyTravelAgentService.perceive(request);
            updateTaskStatus(task, "PROCESSING", 25);

            String agentAdvice = generateAgentAdvice(request, intent, userId);
            updateTaskStatus(task, "PROCESSING", 55);

            PlanningContext context = legacyTravelAgentService.plan(intent, userId);
            ItineraryDraft draft = legacyTravelAgentService.decide(intent, context);
            enrichDraft(draft, request.getDestination(), agentAdvice);
            updateTaskStatus(task, "PROCESSING", 85);

            ItineraryDTO result = legacyTravelAgentService.act(task, draft);
            updateTaskStatus(task, "COMPLETED", 100);
            logger.info("LangChain4j travel plan completed, taskId={}, itineraryId={}",
                    taskId, result.getItineraryId());
        } catch (Exception e) {
            logger.error("LangChain4j travel plan failed, taskId={}", taskId, e);
            task.setStatus("FAILED");
            task.setProgress(0);
            task.setErrorMessage("规划失败:" + e.getMessage());
            travelPlanTaskRepository.save(task);
        }
    }

    private String generateAgentAdvice(PlanItineraryRequest request, UserIntent intent, String userId) {
        TravelAgentProperties.Langchain4j config = travelAgentProperties.getLangchain4j();
        if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
            return "LangChain4j Agent 未启用，当前行程使用基础规划结果。";
        }
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            return "LangChain4j Agent 缺少 API Key，当前行程使用基础规划结果。";
        }

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .modelName(config.getModel())
                .temperature(config.getTemperature())
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .build();

        TravelPlanningAssistant assistant = AiServices.builder(TravelPlanningAssistant.class)
                .chatModel(chatModel)
                .tools(travelPlanningTools)
                .build();

        String prompt = buildAgentPrompt(request, intent, userId);
        return assistant.plan(prompt);
    }

    private String buildAgentPrompt(PlanItineraryRequest request, UserIntent intent, String userId) {
        String interests = request.getPreferences() != null && request.getPreferences().getInterests() != null
                ? String.join("、", request.getPreferences().getInterests())
                : "无特别偏好";
        String pace = request.getPreferences() != null && request.getPreferences().getPace() != null
                ? request.getPreferences().getPace()
                : "适中";
        String meals = request.getPreferences() != null && Boolean.TRUE.equals(request.getPreferences().getIncludeMeals())
                ? "需要餐饮建议"
                : "餐饮建议可选";
        String avoidCrowds = request.getPreferences() != null && Boolean.TRUE.equals(request.getPreferences().getAvoidCrowds())
                ? "尽量避开拥挤时段"
                : "无需特别避开拥挤";

        return """
                请为以下旅行需求生成规划建议，并主动使用工具完成景点检索、路线判断和本地知识库检索。
                当前 userId: %s
                目的地: %s
                出行日期: %s 至 %s，共 %s 天
                出行人数: %s
                预算: %s
                节奏偏好: %s
                兴趣偏好: %s
                其他要求: %s，%s

                要求:
                - 至少调用一次景点工具
                - 至少调用一次路线工具
                - 如有可能，调用一次知识库工具补充本地资料
                - 输出要能直接附加到结构化行程中，避免空泛
                """.formatted(
                userId,
                request.getDestination(),
                request.getStartDate(),
                request.getEndDate(),
                intent.getDays(),
                Objects.toString(request.getTravelerCount(), "2"),
                Objects.toString(request.getBudget(), "未填写"),
                pace,
                interests,
                meals,
                avoidCrowds
        );
    }

    private void enrichDraft(ItineraryDraft draft, String city, String agentAdvice) {
        StringBuilder summary = new StringBuilder();
        if (draft.getSummary() != null && !draft.getSummary().isBlank()) {
            summary.append(draft.getSummary().trim());
        }
        if (agentAdvice != null && !agentAdvice.isBlank()) {
            if (summary.length() > 0) {
                summary.append("\n\n");
            }
            summary.append("智能伴旅Agent建议:\n").append(agentAdvice.trim());
        }
        draft.setSummary(summary.toString());

        List<ItineraryDayDTO> days = draft.getDays();
        if (days == null) {
            return;
        }

        for (ItineraryDayDTO day : days) {
            if (day == null || day.getItems() == null || day.getItems().size() < 2) {
                continue;
            }
            ItineraryItemDTO first = day.getItems().get(0);
            ItineraryItemDTO second = day.getItems().get(1);
            String routeTip = travelPlanningTools.planRoute(city, first.getAttractionName(), second.getAttractionName());
            String notes = day.getNotes() == null ? "" : day.getNotes().trim();
            if (!notes.isEmpty()) {
                notes += "\n";
            }
            notes += "路线提示: " + routeTip;
            day.setNotes(notes);
        }
    }

    private void updateTaskStatus(TravelPlanTask task, String status, int progress) {
        task.setStatus(status);
        task.setProgress(progress);
        task.setErrorMessage(null);
        travelPlanTaskRepository.save(task);
    }
}
