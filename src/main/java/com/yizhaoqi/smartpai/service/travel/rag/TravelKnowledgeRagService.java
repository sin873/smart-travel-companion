package com.yizhaoqi.smartpai.service.travel.rag;

import com.yizhaoqi.smartpai.config.TravelAgentProperties;
import com.yizhaoqi.smartpai.entity.SearchResult;
import com.yizhaoqi.smartpai.model.travel.UserIntent;
import com.yizhaoqi.smartpai.model.travel.rag.TravelKnowledgeSnippet;
import com.yizhaoqi.smartpai.service.HybridSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

@Service
public class TravelKnowledgeRagService {

    private static final Logger logger = LoggerFactory.getLogger(TravelKnowledgeRagService.class);

    private final HybridSearchService hybridSearchService;
    private final TravelAgentProperties travelAgentProperties;

    public TravelKnowledgeRagService(HybridSearchService hybridSearchService,
                                     TravelAgentProperties travelAgentProperties) {
        this.hybridSearchService = hybridSearchService;
        this.travelAgentProperties = travelAgentProperties;
    }

    public List<String> retrievePlanningKnowledge(UserIntent intent, String userId) {
        String query = buildPlanningQuery(intent);
        List<TravelKnowledgeSnippet> snippets = searchKnowledge(userId, query, resolveTopK());
        return snippets.stream()
                .map(this::formatPlanningSnippet)
                .toList();
    }

    public String searchKnowledgeForAgent(String userId, String query) {
        List<TravelKnowledgeSnippet> snippets = searchKnowledge(userId, query, resolveTopK());
        if (snippets.isEmpty()) {
            return "知识库中没有检索到明显相关的结果。";
        }

        StringJoiner joiner = new StringJoiner("\n");
        for (int i = 0; i < snippets.size(); i++) {
            TravelKnowledgeSnippet snippet = snippets.get(i);
            joiner.add(String.format(Locale.ROOT,
                    "%d. 来源:%s | 分块:%s | 相关度:%.3f | 摘要:%s",
                    i + 1,
                    safe(snippet.getSourceTitle()),
                    snippet.getChunkId() != null ? snippet.getChunkId() : -1,
                    snippet.getScore() != null ? snippet.getScore() : 0D,
                    safe(snippet.getSummary())));
        }
        return joiner.toString();
    }

    public List<TravelKnowledgeSnippet> searchKnowledge(String userId, String query, Integer topK) {
        String normalizedQuery = normalizeQuery(query);
        if (normalizedQuery.isBlank()) {
            return List.of();
        }

        int limit = resolveTopK(topK);
        int recallSize = Math.max(limit * 3, limit);

        List<SearchResult> rawResults;
        if (userId == null || userId.isBlank()) {
            rawResults = hybridSearchService.search(normalizedQuery, recallSize);
        } else {
            rawResults = hybridSearchService.searchWithPermission(normalizedQuery, userId, recallSize);
        }

        logger.debug("Travel knowledge recall finished, query={}, rawResultCount={}", normalizedQuery,
                rawResults == null ? 0 : rawResults.size());
        return deduplicate(rawResults, limit);
    }

    private List<TravelKnowledgeSnippet> deduplicate(List<SearchResult> rawResults, int limit) {
        if (rawResults == null || rawResults.isEmpty()) {
            return List.of();
        }

        Map<String, TravelKnowledgeSnippet> unique = new LinkedHashMap<>();
        for (SearchResult result : rawResults) {
            if (result == null || isBlank(result.getTextContent())) {
                continue;
            }

            String dedupeKey = buildDedupeKey(result);
            if (unique.containsKey(dedupeKey)) {
                continue;
            }

            unique.put(dedupeKey, toSnippet(result));
            if (unique.size() >= limit) {
                break;
            }
        }

        return new ArrayList<>(unique.values());
    }

    private TravelKnowledgeSnippet toSnippet(SearchResult result) {
        String sourceTitle = !isBlank(result.getFileName()) ? result.getFileName() : result.getFileMd5();
        String excerpt = compact(result.getTextContent(), 220);
        String summary = compact(result.getTextContent(), 120);

        TravelKnowledgeSnippet snippet = new TravelKnowledgeSnippet();
        snippet.setSourceId(result.getFileMd5());
        snippet.setSourceTitle(sourceTitle);
        snippet.setChunkId(result.getChunkId());
        snippet.setScore(result.getScore());
        snippet.setExcerpt(excerpt);
        snippet.setSummary(summary);
        return snippet;
    }

    private String formatPlanningSnippet(TravelKnowledgeSnippet snippet) {
        return String.format("%s：%s",
                safe(snippet.getSourceTitle()),
                safe(snippet.getSummary()));
    }

    private String buildPlanningQuery(UserIntent intent) {
        if (intent == null) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(" ");
        if (!isBlank(intent.getDestination())) {
            joiner.add(intent.getDestination());
        }
        joiner.add("旅游攻略 景点 交通 住宿 美食 注意事项");

        if (intent.getInterests() != null && !intent.getInterests().isEmpty()) {
            joiner.add(String.join(" ", intent.getInterests()));
        }
        if (intent.getBudget() != null) {
            joiner.add("预算 " + intent.getBudget());
        }
        if (!isBlank(intent.getPace())) {
            joiner.add("节奏 " + intent.getPace());
        }
        return joiner.toString();
    }

    private String buildDedupeKey(SearchResult result) {
        return String.join("|",
                Objects.toString(result.getFileMd5(), ""),
                Objects.toString(result.getChunkId(), "-1"),
                normalizeText(result.getTextContent()));
    }

    private String normalizeQuery(String query) {
        return query == null ? "" : query.replaceAll("\\s+", " ").trim();
    }

    private String normalizeText(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }

    private String compact(String text, int maxLength) {
        String normalized = normalizeText(text);
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    private int resolveTopK() {
        return resolveTopK(travelAgentProperties.getRagTopK());
    }

    private int resolveTopK(Integer topK) {
        return topK == null || topK <= 0 ? 4 : topK;
    }

    private String safe(String value) {
        return isBlank(value) ? "未知" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
