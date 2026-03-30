package com.yizhaoqi.smartpai.repository.miniprogram;

import com.yizhaoqi.smartpai.entity.miniprogram.PlanTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanTaskRepository extends JpaRepository<PlanTask, Long> {
    
    Optional<PlanTask> findByTaskId(String taskId);
    
    List<PlanTask> findByUserIdOrderByCreatedAtDesc(String userId);
}
