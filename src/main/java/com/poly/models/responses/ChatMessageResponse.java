package com.poly.models.responses;

import lombok.Data;

@Data
public class ChatMessageResponse {

    private String id;

    private String content;

    private String createdDate;

    private String senderId;

    private String receiverId;

    private String senderUsername;

    private String senderFullname;

    private String senderUrl;
}
