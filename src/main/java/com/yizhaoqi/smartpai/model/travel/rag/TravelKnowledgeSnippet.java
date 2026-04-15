package com.yizhaoqi.smartpai.model.travel.rag;

import lombok.Data;

@Data
public class TravelKnowledgeSnippet {

    private String sourceId;

    private String sourceTitle;

    private Integer chunkId;

    private Double score;

    private String excerpt;

    private String summary;
}
