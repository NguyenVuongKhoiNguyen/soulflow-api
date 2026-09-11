package com.poly.controllers;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.poly.models.requests.ChatMessageRequest;
import com.poly.models.responses.ChatMessageResponse;
import com.poly.models.services.impl.ChatMessageAsyncService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/chat/messages")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageAsyncService chatMessageAsyncService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompletableFuture<ChatMessageResponse> saveMessage(@RequestBody ChatMessageRequest request) {
        if (request == null || request.getContent() == null || request.getContent().isBlank()) {
            throw new IllegalArgumentException("Message content must not be blank");
        }
        if (request.getSenderId() == null) {
            throw new IllegalArgumentException("Sender account id is required");
        }

        request.setContent(request.getContent().trim());
        return chatMessageAsyncService.saveMessage(request);
    }

    @GetMapping
    public CompletableFuture<List<ChatMessageResponse>> getMessages() {
        return chatMessageAsyncService.getMessages();
    }
}
