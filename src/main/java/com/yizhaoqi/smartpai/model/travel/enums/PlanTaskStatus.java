package com.yizhaoqi.smartpai.model.travel.enums;

public enum PlanTaskStatus {

    QUEUED("QUEUED", "排队中"),
    PROCESSING("PROCESSING", "处理中"),
    COMPLETED("COMPLETED", "已完成"),
    FAILED("FAILED", "失败");

    private final String code;
    private final String desc;

    PlanTaskStatus(String code, String desc) {
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
