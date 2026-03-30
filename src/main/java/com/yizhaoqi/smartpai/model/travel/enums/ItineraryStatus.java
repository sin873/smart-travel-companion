package com.yizhaoqi.smartpai.model.travel.enums;

public enum ItineraryStatus {

    DRAFT("DRAFT", "草稿"),
    PLANNED("PLANNED", "已规划"),
    COMPLETED("COMPLETED", "已完成");

    private final String code;
    private final String desc;

    ItineraryStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
