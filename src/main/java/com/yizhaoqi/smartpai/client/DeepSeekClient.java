package com.yizhaoqi.smartpai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yizhaoqi.smartpai.config.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class DeepSeekClient {

    private static final Logger logger = LoggerFactory.getLogger(DeepSeekClient.class);
    private static final int MAX_HISTORY_MESSAGES = 8;

    private final WebClient webClient;
    private final String model;
    private final AiProperties aiProperties;

    public DeepSeekClient(@Value("${deepseek.api.url}") String apiUrl,
                         @Value("${deepseek.api.key}") String apiKey,
                         @Value("${deepseek.api.model}") String model,
                         AiProperties aiProperties) {
        WebClient.Builder builder = WebClient.builder().baseUrl(apiUrl);

        if (apiKey != null && !apiKey.trim().isEmpty()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }

        this.webClient = builder.build();
        this.model = model;
        this.aiProperties = aiProperties;
    }

    public void streamResponse(String userMessage,
                             String context,
                             List<Map<String, String>> history,
                             Consumer<String> onChunk,
                             Consumer<Throwable> onError) {

        Map<String, Object> request = buildRequest(userMessage, context, history);

        webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .subscribe(
                    chunk -> processChunk(chunk, onChunk),
                    onError
                );
    }

    private Map<String, Object> buildRequest(String userMessage,
                                           String context,
                                           List<Map<String, String>> history) {
        logger.info("构建请求，用户消息：{}，上下文长度：{}，历史消息数：{}",
                userMessage,
                context != null ? context.length() : 0,
                history != null ? history.size() : 0);

        Map<String, Object> request = new HashMap<>();
        request.put("model", model);
        request.put("messages", buildMessages(userMessage, context, history));
        request.put("stream", true);

        AiProperties.Generation gen = aiProperties.getGeneration();
        if (gen.getTemperature() != null) {
            request.put("temperature", gen.getTemperature());
        }
        if (gen.getTopP() != null) {
            request.put("top_p", gen.getTopP());
        }
        if (gen.getMaxTokens() != null) {
            request.put("max_tokens", gen.getMaxTokens());
        }
        return request;
    }

    private List<Map<String, String>> buildMessages(String userMessage,
                                                  String context,
                                                  List<Map<String, String>> history) {
        List<Map<String, String>> messages = new ArrayList<>();
        AiProperties.Prompt promptCfg = aiProperties.getPrompt();

        String rules = promptCfg.getRules();
        if (rules != null && !rules.isBlank()) {
            messages.add(Map.of(
                "role", "system",
                "content", rules
            ));
        }

        List<Map<String, String>> sanitizedHistory = sanitizeHistory(history);
        if (!sanitizedHistory.isEmpty()) {
            messages.addAll(sanitizedHistory);
        }

        String referenceContent = buildReferenceMessage(context, promptCfg);
        messages.add(Map.of(
            "role", "system",
            "content", referenceContent
        ));
        logger.debug("添加本轮参考消息，长度: {}", referenceContent.length());

        messages.add(Map.of(
            "role", "user",
            "content", buildCurrentUserMessage(userMessage, context)
        ));

        return messages;
    }

    private List<Map<String, String>> sanitizeHistory(List<Map<String, String>> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }

        int start = Math.max(0, history.size() - MAX_HISTORY_MESSAGES);
        List<Map<String, String>> sanitized = new ArrayList<>();

        for (int i = start; i < history.size(); i++) {
            Map<String, String> item = history.get(i);
            if (item == null) {
                continue;
            }

            String role = item.get("role");
            String content = item.get("content");
            if (role == null || content == null || content.isBlank()) {
                continue;
            }
            if (!"user".equals(role) && !"assistant".equals(role) && !"system".equals(role)) {
                continue;
            }

            sanitized.add(Map.of(
                "role", role,
                "content", content
            ));
        }

        return sanitized;
    }

    private String buildReferenceMessage(String context, AiProperties.Prompt promptCfg) {
        String refStart = promptCfg.getRefStart() != null ? promptCfg.getRefStart() : "<<REF>>";
        String refEnd = promptCfg.getRefEnd() != null ? promptCfg.getRefEnd() : "<<END>>";
        String noResult = promptCfg.getNoResultText() != null ? promptCfg.getNoResultText() : "（本轮无检索结果）";

        StringBuilder referenceBuilder = new StringBuilder();
        referenceBuilder.append("以下是本轮检索到的参考资料。若参考资料非空，必须优先依据其回答；仅当参考资料为空或与问题明显无关时，才可回答“暂无相关信息”。\n");
        referenceBuilder.append(refStart).append("\n");

        if (context != null && !context.isBlank()) {
            referenceBuilder.append(context).append("\n");
        } else {
            referenceBuilder.append(noResult).append("\n");
        }

        referenceBuilder.append(refEnd);
        return referenceBuilder.toString();
    }

    private String buildCurrentUserMessage(String userMessage, String context) {
        if (context != null && !context.isBlank()) {
            return "请严格根据“本轮检索到的参考资料”回答。如果参考资料已经提供了相关信息，不要回答“暂无相关信息”。\n用户问题：" + userMessage;
        }
        return userMessage;
    }

    private void processChunk(String chunk, Consumer<String> onChunk) {
        try {
            if ("[DONE]".equals(chunk)) {
                logger.debug("对话结束");
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(chunk);
            String content = node.path("choices")
                    .path(0)
                    .path("delta")
                    .path("content")
                    .asText("");

            if (!content.isEmpty()) {
                onChunk.accept(content);
            }
        } catch (Exception e) {
            logger.error("处理数据块时出错: {}", e.getMessage(), e);
        }
    }
}
