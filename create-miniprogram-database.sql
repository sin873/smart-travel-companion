-- ========================================
-- 智能伴旅小程序 - 数据库扩展脚本
-- ========================================

USE pai_smart_travel;

-- ========================================
-- 5. 用户行程表 (itinerary)
-- ========================================
CREATE TABLE IF NOT EXISTS itinerary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    itinerary_id VARCHAR(64) UNIQUE NOT NULL COMMENT '行程ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    title VARCHAR(256) NOT NULL COMMENT '行程标题',
    destination VARCHAR(128) NOT NULL COMMENT '目的地',
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE NOT NULL COMMENT '结束日期',
    traveler_count INT DEFAULT 1 COMMENT '旅行人数',
    budget DECIMAL(10,2) COMMENT '预算',
    preferences JSON COMMENT '用户偏好 (JSON)',
    status VARCHAR(32) DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PLANNED/COMPLETED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_destination (destination),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户行程表';

-- ========================================
-- 6. 每日行程表 (itinerary_day)
-- ========================================
CREATE TABLE IF NOT EXISTS itinerary_day (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    day_id VARCHAR(64) UNIQUE NOT NULL COMMENT '每日行程ID',
    itinerary_id VARCHAR(64) NOT NULL COMMENT '所属行程ID',
    day_number INT NOT NULL COMMENT '第几天',
    date DATE NOT NULL COMMENT '日期',
    title VARCHAR(256) COMMENT '当日主题',
    notes TEXT COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_itinerary_id (itinerary_id),
    INDEX idx_day_number (day_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日行程表';

-- ========================================
-- 7. 行程景点表 (itinerary_attraction)
-- ========================================
CREATE TABLE IF NOT EXISTS itinerary_attraction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    item_id VARCHAR(64) UNIQUE NOT NULL COMMENT '行程项ID',
    day_id VARCHAR(64) NOT NULL COMMENT '所属每日行程ID',
    attraction_id VARCHAR(64) COMMENT '景点ID',
    attraction_name VARCHAR(256) NOT NULL COMMENT '景点名称',
    address VARCHAR(512) COMMENT '地址',
    latitude DECIMAL(10,7) COMMENT '纬度',
    longitude DECIMAL(10,7) COMMENT '经度',
    start_time TIME COMMENT '开始时间',
    end_time TIME COMMENT '结束时间',
    duration_minutes INT COMMENT '预计时长(分钟)',
    order_index INT NOT NULL COMMENT '排序',
    notes TEXT COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_day_id (day_id),
    INDEX idx_order_index (order_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行程景点表';

-- ========================================
-- 8. 行程规划任务表 (plan_task)
-- ========================================
CREATE TABLE IF NOT EXISTS plan_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(64) UNIQUE NOT NULL COMMENT '任务ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    request_data JSON COMMENT '请求数据',
    status VARCHAR(32) DEFAULT 'QUEUED' COMMENT '状态: QUEUED/PROCESSING/COMPLETED/FAILED',
    progress INT DEFAULT 0 COMMENT '进度 0-100',
    message TEXT COMMENT '消息',
    result_data JSON COMMENT '结果数据',
    error_message TEXT COMMENT '错误信息',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_task_id (task_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行程规划任务表';

-- ========================================
-- 9. 小程序用户表 (miniprogram_user)
-- ========================================
CREATE TABLE IF NOT EXISTS miniprogram_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) UNIQUE NOT NULL COMMENT '用户ID',
    openid VARCHAR(128) UNIQUE NOT NULL COMMENT '微信openid',
    unionid VARCHAR(128) COMMENT '微信unionid',
    nick_name VARCHAR(128) COMMENT '昵称',
    avatar_url VARCHAR(512) COMMENT '头像URL',
    phone VARCHAR(32) COMMENT '手机号',
    preferences JSON COMMENT '用户偏好',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_openid (openid),
    INDEX idx_unionid (unionid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序用户表';

-- ========================================
-- 初始化完成
-- ========================================
SELECT '小程序数据库扩展完成！' AS message;
