package com.yizhaoqi.smartpai.entity.miniprogram;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "plan_task")
public class PlanTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", unique = true, nullable = false, length = 64)
    private String taskId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "request_data", columnDefinition = "json")
    private String requestData;

    @Column(name = "status", length = 32)
    private String status = "QUEUED";

    @Column(name = "progress")
    private Integer progress = 0;

    @Column(name = "message", columnDefinition = "text")
    private String message;

    @Column(name = "result_data", columnDefinition = "json")
    private String resultData;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
