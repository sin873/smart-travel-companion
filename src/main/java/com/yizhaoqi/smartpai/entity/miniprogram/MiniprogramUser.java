package com.yizhaoqi.smartpai.entity.miniprogram;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "miniprogram_user")
public class MiniprogramUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true, nullable = false, length = 64)
    private String userId;

    @Column(name = "openid", unique = true, nullable = false, length = 128)
    private String openid;

    @Column(name = "unionid", length = 128)
    private String unionid;

    @Column(name = "nick_name", length = 128)
    private String nickName;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Column(name = "phone", length = 32)
    private String phone;

    @Column(name = "preferences", columnDefinition = "json")
    private String preferences;

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
