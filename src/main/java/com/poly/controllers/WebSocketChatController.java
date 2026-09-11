package com.poly.controllers;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import com.poly.models.requests.WebSocketChatMessage;

@Controller
public class WebSocketChatController {

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public WebSocketChatMessage sendMessage(@Payload WebSocketChatMessage request) {
        if (request == null || request.getContent() == null || request.getContent().isBlank()) {
            throw new IllegalArgumentException("Message content must not be blank");
        }

        request.setContent(request.getContent().trim());
        return request;
    }
}
