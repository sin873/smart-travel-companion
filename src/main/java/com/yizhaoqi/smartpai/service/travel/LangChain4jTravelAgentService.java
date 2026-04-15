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
        if (!travelAgentProperties.useLangChain4j()) {
            logger.info("LangChain4j provider disabled, fallback to legacy travel agent, taskId={}", taskId);
            legacyTravelAgentService.executePlan(taskId, request, userId);
            return;
        }

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
            logger.warn("LangChain4j travel agent failed, fallback to legacy flow, taskId={}", taskId, e);
            legacyTravelAgentService.executePlan(taskId, request, userId);
        }
    }

    private String generateAgentAdvice(PlanItineraryRequest request, UserIntent intent, String userId) {
        TravelAgentProperties.Langchain4j config = travelAgentProperties.getLangchain4j();
        if (config == null) {
            throw new IllegalStateException("LangChain4j config is missing");
        }
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new IllegalStateException("LangChain4j agent apiKey is blank");
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

        return assistant.plan(buildAgentPrompt(request, intent, userId));
    }

    private String buildAgentPrompt(PlanItineraryRequest request, UserIntent intent, String userId) {
        String interests = request.getPreferences() != null && request.getPreferences().getInterests() != null
                ? String.join(", ", request.getPreferences().getInterests())
                : "none";
        String pace = request.getPreferences() != null && request.getPreferences().getPace() != null
                ? request.getPreferences().getPace()
                : "normal";
        String meals = request.getPreferences() != null && Boolean.TRUE.equals(request.getPreferences().getIncludeMeals())
                ? "need meal suggestions"
                : "meal suggestions optional";
        String avoidCrowds = request.getPreferences() != null && Boolean.TRUE.equals(request.getPreferences().getAvoidCrowds())
                ? "prefer to avoid peak crowd hours"
                : "no crowd avoidance requirement";

        return """
                You are the travel planning agent of Smart Travel Companion.
                Use tools first before answering. Prefer concrete POIs, route hints, and local knowledge.

                userId: %s
                destination: %s
                travel dates: %s to %s, %s day(s)
                traveler count: %s
                budget: %s
                pace: %s
                interests: %s
                extra preferences: %s, %s

                Requirements:
                - Call the POI tool at least once.
                - Call the route tool at least once.
                - If useful, call the local knowledge search tool once.
                - Respond in concise Simplified Chinese.
                - Structure the answer with recommended places, route advice, and notes.
                """.formatted(
                userId,
                request.getDestination(),
                request.getStartDate(),
                request.getEndDate(),
                intent.getDays(),
                Objects.toString(request.getTravelerCount(), "2"),
                Objects.toString(request.getBudget(), "unspecified"),
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
            summary.append("智能伴旅 Agent 建议:\n").append(agentAdvice.trim());
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
