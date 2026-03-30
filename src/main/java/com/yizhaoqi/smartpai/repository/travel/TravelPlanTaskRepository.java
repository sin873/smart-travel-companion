package com.yizhaoqi.smartpai.repository.travel;

import com.yizhaoqi.smartpai.entity.travel.TravelPlanTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TravelPlanTaskRepository extends JpaRepository<TravelPlanTask, Long> {

    Optional<TravelPlanTask> findByTaskId(String taskId);

    List<TravelPlanTask> findByUserIdOrderByCreatedAtDesc(String userId);
}
