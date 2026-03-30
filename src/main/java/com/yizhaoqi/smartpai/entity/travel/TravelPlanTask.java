package com.yizhaoqi.smartpai.entity.travel;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "travel_plan_task")
public class TravelPlanTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", unique = true, nullable = false, length = 64)
    private String taskId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "destination", nullable = false, length = 128)
    private String destination;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "traveler_count")
    private Integer travelerCount = 1;

    @Column(name = "budget", precision = 10, scale = 2)
    private BigDecimal budget;

    @Column(name = "preferences_json", columnDefinition = "text")
    private String preferencesJson;

    @Column(name = "status", length = 32)
    private String status = "QUEUED";

    @Column(name = "progress")
    private Integer progress = 0;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "result_json", columnDefinition = "text")
    private String resultJson;

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
