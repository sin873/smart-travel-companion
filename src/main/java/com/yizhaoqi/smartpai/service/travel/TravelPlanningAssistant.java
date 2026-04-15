package com.yizhaoqi.smartpai.service.travel;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface TravelPlanningAssistant {

    @SystemMessage("""
            你是“智能伴旅”项目中的行程规划 Agent。
            你的任务是为旅行请求生成可执行的规划建议。
            你必须优先调用工具来获取景点、路线和知识库信息，而不是凭空编造。
            输出使用简体中文，保持简洁，按以下结构输出：
            1. 推荐景点
            2. 路线建议
            3. 注意事项
            如果某项没有足够依据，请明确说明。
            """)
    String plan(@UserMessage String request);
}
