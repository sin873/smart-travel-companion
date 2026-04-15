package com.yizhaoqi.smartpai.service.travel;

import com.yizhaoqi.smartpai.config.TravelAgentProperties;
import com.yizhaoqi.smartpai.entity.travel.TravelPlanTask;
import com.yizhaoqi.smartpai.model.travel.ItineraryDTO;
import com.yizhaoqi.smartpai.model.travel.ItineraryDraft;
import com.yizhaoqi.smartpai.model.travel.PlanItineraryRequest;
import com.yizhaoqi.smartpai.model.travel.PlanningContext;
import com.yizhaoqi.smartpai.model.travel.UserIntent;
import com.yizhaoqi.smartpai.repository.travel.TravelPlanTaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LangChain4jTravelAgentServiceTest {

    @Mock
    private TravelPlanTaskRepository travelPlanTaskRepository;

    @Mock
    private TravelAgentService legacyTravelAgentService;

    @Mock
    private TravelPlanningTools travelPlanningTools;

    @Mock
    private TravelAgentProperties travelAgentProperties;

    @InjectMocks
    private LangChain4jTravelAgentService langChain4jTravelAgentService;

    @Test
    void executePlan_shouldPassUserIdToPlanningPhase() throws Exception {
        String taskId = "task-001";
        String userId = "user-001";

        PlanItineraryRequest request = new PlanItineraryRequest();
        request.setDestination("杭州");
        request.setStartDate("2026-05-01");
        request.setEndDate("2026-05-03");

        TravelPlanTask task = new TravelPlanTask();
        task.setTaskId(taskId);

        UserIntent intent = new UserIntent();
        PlanningContext context = new PlanningContext();
        ItineraryDraft draft = new ItineraryDraft();
        ItineraryDTO result = new ItineraryDTO();
        result.setItineraryId("itin-001");

        when(travelPlanTaskRepository.findByTaskId(taskId)).thenReturn(Optional.of(task));
        when(legacyTravelAgentService.perceive(request)).thenReturn(intent);
        when(legacyTravelAgentService.plan(intent, userId)).thenReturn(context);
        when(legacyTravelAgentService.decide(intent, context)).thenReturn(draft);
        when(legacyTravelAgentService.act(task, draft)).thenReturn(result);

        langChain4jTravelAgentService.executePlan(taskId, request, userId);

        verify(legacyTravelAgentService).plan(intent, userId);
        verify(legacyTravelAgentService, never()).plan(intent);
    }
}
