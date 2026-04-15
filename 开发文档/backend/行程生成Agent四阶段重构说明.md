# 行程生成 Agent 四阶段重构说明

## 1. 主流程

当前行程生成链路已重构为四阶段显式流程：

```java
UserIntent intent = travelAgentService.perceive(request);
PlanningContext context = travelAgentService.plan(intent);
ItineraryDraft draft = travelAgentService.decide(intent, context);
ItineraryDTO result = travelAgentService.act(task, draft);
```

对外接口不变，仍由：
- `POST /api/v1/travel/plan`
- `GET /api/v1/travel/plan/{taskId}`

完成创建任务与查询结果。

## 2. 文件调整

新增：
- `src/main/java/com/yizhaoqi/smartpai/model/travel/UserIntent.java`
- `src/main/java/com/yizhaoqi/smartpai/model/travel/PoiCandidate.java`
- `src/main/java/com/yizhaoqi/smartpai/model/travel/PlanningContext.java`
- `src/main/java/com/yizhaoqi/smartpai/model/travel/ItineraryDraft.java`
- `开发文档/backend/行程生成Agent四阶段重构说明.md`

修改：
- `src/main/java/com/yizhaoqi/smartpai/service/travel/TravelAgentService.java`

## 3. 四阶段职责

### Perception
- 方法：`perceive(PlanItineraryRequest request)`
- 作用：解析用户输入，标准化为 `UserIntent`
- 输出：目的地、日期、人数、预算、兴趣、节奏、天数

### Planning
- 方法：`plan(UserIntent intent)`
- 作用：组织候选信息，形成 `PlanningContext`
- 当前能力：
  - 高德 POI 搜索
  - Mock 景点库回退
  - 知识检索字段预留

### Decision
- 方法：`decide(UserIntent intent, PlanningContext context)`
- 作用：筛选候选景点并分配到每日行程
- 输出：`ItineraryDraft`
- 当前仍以规则 + mock 分配为主

### Action
- 方法：`act(TravelPlanTask task, ItineraryDraft draft)`
- 作用：将草案转换为 `ItineraryDTO`，写入 `task.resultJson`
- 输出：前端现有接口所需结果结构

## 4. 中间对象

### UserIntent
- 用户结构化意图
- 对应“感知”阶段输出

### PlanningContext
- 候选 POI、知识检索结果、约束摘要
- 对应“规划”阶段输出

### PoiCandidate
- 统一高德 POI 与 Mock 景点候选结构

### ItineraryDraft
- 行程草案
- 对应“决策”阶段输出

## 5. 与高德 / 知识库 / 大模型对应关系

- 高德 POI：规划阶段
- 知识库检索（RAG）：规划阶段预留，当前返回空列表
- 大模型生成：决策阶段预留入口，当前仍采用规则 + mock 草案生成
- 结果输出与持久化：行动阶段

## 6. 当前仍是 Mock 的部分

- 行程排序与分配策略仍为本地规则
- 知识检索尚未真正接入
- 高德 POI 转候选后仍使用固定时长
- 大模型尚未真正接入决策阶段

## 7. Agent 调用链体现方式

代码中已通过：
- 显式四阶段方法
- 清晰的中间对象
- 分阶段日志

来体现 Agent 调用链。

关键日志示例：
- `【Perception】解析用户输入成功`
- `【Planning】高德POI候选数量：X`
- `【Planning】知识检索结果数量：X`
- `【Decision】生成 itinerary 草案成功`
- `【Action】结果持久化完成`

该结构可直接用于论文或答辩中说明：
- Agent 如何感知输入
- 如何使用工具与外部知识完成规划
- 如何完成决策与输出执行
