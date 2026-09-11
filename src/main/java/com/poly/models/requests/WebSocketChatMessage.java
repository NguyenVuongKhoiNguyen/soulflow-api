package com.poly.models.requests;

import lombok.Data;

@Data
public class WebSocketChatMessage {

    private String content;

    private Long senderId;

    private Long receiverId;
}
