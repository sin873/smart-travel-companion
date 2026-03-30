-- ========================================
-- 智能伴旅 - 数据库初始化脚本
-- ========================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS pai_smart_travel DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE pai_smart_travel;

-- ========================================
-- 1. 景区文档表
-- ========================================
CREATE TABLE IF NOT EXISTS scenic_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    doc_id VARCHAR(64) UNIQUE NOT NULL COMMENT '文档唯一标识',
    title VARCHAR(256) NOT NULL COMMENT '文档标题',
    content TEXT NOT NULL COMMENT '文档内容',
    source_type VARCHAR(32) NOT NULL COMMENT '来源类型: OFFICIAL/USER_NOTE',
    source_url VARCHAR(512) COMMENT '来源URL',
    scenic_id VARCHAR(64) COMMENT '景区ID',
    scenic_name VARCHAR(128) COMMENT '景区名称',
    user_id VARCHAR(64) COMMENT '上传用户ID',
    org_tag VARCHAR(64) COMMENT '组织标签(多租户)',
    is_public BOOLEAN DEFAULT TRUE COMMENT '是否公开',
    status VARCHAR(32) DEFAULT 'PENDING' COMMENT '状态: PENDING/PROCESSED/FAILED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_scenic_id (scenic_id),
    INDEX idx_user_id (user_id),
    INDEX idx_org_tag (org_tag),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='景区文档表';

-- ========================================
-- 2. 文本分块表
-- ========================================
CREATE TABLE IF NOT EXISTS text_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    chunk_id VARCHAR(64) UNIQUE NOT NULL COMMENT '分块唯一标识',
    doc_id VARCHAR(64) NOT NULL COMMENT '所属文档ID',
    chunk_index INT NOT NULL COMMENT '分块序号',
    content TEXT NOT NULL COMMENT '分块内容',
    embedding_vector JSON COMMENT '向量数据(简化存储)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_doc_id (doc_id),
    INDEX idx_chunk_index (chunk_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文本分块表';

-- ========================================
-- 3. 对话会话表
-- ========================================
CREATE TABLE IF NOT EXISTS conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conv_id VARCHAR(64) UNIQUE NOT NULL COMMENT '会话ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    title VARCHAR(256) COMMENT '会话标题',
    scenic_id VARCHAR(64) COMMENT '关联景区',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_scenic_id (scenic_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话会话表';

-- ========================================
-- 4. 对话消息表
-- ========================================
CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    msg_id VARCHAR(64) UNIQUE NOT NULL COMMENT '消息ID',
    conv_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    role VARCHAR(32) NOT NULL COMMENT '角色: USER/ASSISTANT/SYSTEM',
    content TEXT NOT NULL COMMENT '消息内容',
    related_docs JSON COMMENT '关联文档列表',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conv_id (conv_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话消息表';

-- ========================================
-- 插入测试数据
-- ========================================

-- 插入测试文档
INSERT INTO scenic_document (doc_id, title, content, source_type, scenic_id, scenic_name, user_id, is_public, status) VALUES
('doc_gugong_001', '故宫博物院参观指南', '故宫博物院位于北京中轴线中心，是明清两代的皇家宫殿，现为故宫博物院。占地面积72万平方米，建筑面积约15万平方米。', 'OFFICIAL', 'gugong', '故宫博物院', 'system', TRUE, 'PROCESSED'),
('doc_gugong_002', '故宫三大殿介绍', '故宫三大殿包括太和殿、中和殿、保和殿。太和殿是皇帝举行大典的地方，中和殿是皇帝休息的地方，保和殿是举行殿试的地方。', 'OFFICIAL', 'gugong', '故宫博物院', 'system', TRUE, 'PROCESSED'),
('doc_gugong_003', '故宫最佳游览路线', '推荐游览路线：午门 → 太和殿 → 中和殿 → 保和殿 → 乾清宫 → 坤宁宫 → 御花园 → 神武门。全程约3-4小时。', 'USER_NOTE', 'gugong', '故宫博物院', 'user_001', TRUE, 'PROCESSED'),
('doc_summerpalace_001', '颐和园简介', '颐和园是中国清朝时期皇家园林，前身为清漪园，坐落在北京西郊，距城区15公里，占地约290公顷。', 'OFFICIAL', 'summerpalace', '颐和园', 'system', TRUE, 'PROCESSED'),
('doc_greatwall_001', '八达岭长城攻略', '八达岭长城是明长城中保存最好的一段，也是最具代表性的一段。建议早上去，人少景美，穿舒适运动鞋。', 'USER_NOTE', 'greatwall', '八达岭长城', 'user_002', TRUE, 'PROCESSED');

-- 插入测试分块
INSERT INTO text_chunk (chunk_id, doc_id, chunk_index, content) VALUES
('chunk_gugong_001_0', 'doc_gugong_001', 0, '故宫博物院位于北京中轴线中心，是明清两代的皇家宫殿，现为故宫博物院。'),
('chunk_gugong_001_1', 'doc_gugong_001', 1, '占地面积72万平方米，建筑面积约15万平方米。'),
('chunk_gugong_002_0', 'doc_gugong_002', 0, '故宫三大殿包括太和殿、中和殿、保和殿。'),
('chunk_gugong_002_1', 'doc_gugong_002', 1, '太和殿是皇帝举行大典的地方，中和殿是皇帝休息的地方，保和殿是举行殿试的地方。'),
('chunk_gugong_003_0', 'doc_gugong_003', 0, '推荐游览路线：午门 → 太和殿 → 中和殿 → 保和殿 → 乾清宫 → 坤宁宫 → 御花园 → 神武门。'),
('chunk_gugong_003_1', 'doc_gugong_003', 1, '全程约3-4小时。'),
('chunk_summerpalace_001_0', 'doc_summerpalace_001', 0, '颐和园是中国清朝时期皇家园林，前身为清漪园，坐落在北京西郊，距城区15公里，占地约290公顷。'),
('chunk_greatwall_001_0', 'doc_greatwall_001', 0, '八达岭长城是明长城中保存最好的一段，也是最具代表性的一段。'),
('chunk_greatwall_001_1', 'doc_greatwall_001', 1, '建议早上去，人少景美，穿舒适运动鞋。');

-- ========================================
-- 初始化完成
-- ========================================
SELECT '数据库初始化完成！' AS message;
