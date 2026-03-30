package com.yizhaoqi.smartpai.config;

import co.elastic.clients.transport.endpoints.BooleanResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import org.apache.http.ConnectionClosedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.io.StringReader;

@Component
public class EsIndexInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(EsIndexInitializer.class);

    @Autowired
    private ElasticsearchClient esClient;

    @Value("classpath:es-mappings/knowledge_base.json") // 加载 JSON 文件
    private org.springframework.core.io.Resource mappingResource;

    @Override
    public void run(String... args) throws Exception {
        try {
            initializeIndex();
        } catch (Exception exception) {
            logger.warn("Elasticsearch索引初始化失败，应用将继续启动（搜索功能可能不可用）: {}", exception.getMessage());
            // 不抛出异常，让应用继续启动
        }
    }

    /**
     * 初始化索引的核心逻辑
     * @throws Exception
     */
    private void initializeIndex() throws Exception {
        // 检查索引是否存在
        BooleanResponse existsResponse = esClient.indices().exists(ExistsRequest.of(e -> e.index("knowledge_base")));
        if (!existsResponse.value()) {
            createIndex();
        } else {
            logger.info("索引 'knowledge_base' 已存在");
        }
    }

    /**
     * 创建索引
     * @throws Exception
     */
    private void createIndex() throws Exception {
        // 读取 JSON 文件内容
        String mappingJson = new String(Files.readAllBytes(mappingResource.getFile().toPath()), StandardCharsets.UTF_8);

        // 创建索引并应用映射
        CreateIndexRequest createIndexRequest = CreateIndexRequest.of(c -> c
                .index("knowledge_base") // 索引名称
                .withJson(new StringReader(mappingJson)) // 使用 JSON 文件定义映射
        );
        esClient.indices().create(createIndexRequest);
        logger.info("索引 'knowledge_base' 已创建");
    }
}