package com.yizhaoqi.smartpai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "travel.agent")
@Data
public class TravelAgentProperties {

    private String provider = "legacy";
    private Integer ragTopK = 4;
    private Langchain4j langchain4j = new Langchain4j();

    public boolean useLangChain4j() {
        return "langchain4j".equalsIgnoreCase(provider)
                && langchain4j != null
                && Boolean.TRUE.equals(langchain4j.getEnabled());
    }

    @Data
    public static class Langchain4j {
        private Boolean enabled = Boolean.FALSE;
        private String baseUrl = "https://api.deepseek.com/v1";
        private String apiKey = "";
        private String model = "deepseek-chat";
        private Double temperature = 0.2;
        private Integer timeoutSeconds = 60;
    }
}
