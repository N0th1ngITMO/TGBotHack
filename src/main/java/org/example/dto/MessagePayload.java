package org.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MessagePayload {
    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("text")
    private String text;

    public MessagePayload(Long userId, String text) {
        this.userId = userId;
        this.text   = text;
    }

    // геттеры/сеттеры
}
