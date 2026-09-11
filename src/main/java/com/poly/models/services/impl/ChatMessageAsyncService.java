package com.poly.models.services.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.poly.models.entities.ChatMessage;
import com.poly.models.mappers.ChatMessageMapper;
import com.poly.models.repositories.ChatMessageRepository;
import com.poly.models.requests.ChatMessageRequest;
import com.poly.models.responses.ChatMessageResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatMessageAsyncService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageMapper chatMessageMapper;

    @Async("chatTaskExecutor")
    @Transactional
    public CompletableFuture<ChatMessageResponse> saveMessage(ChatMessageRequest request) {
        ChatMessage savedMessage = chatMessageRepository.save(chatMessageMapper.toEntity(request));
        ChatMessageResponse response = chatMessageMapper.toResponse(savedMessage);
        return CompletableFuture.completedFuture(response);
    }

    @Async("chatTaskExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<List<ChatMessageResponse>> getMessages() {
        List<ChatMessageResponse> responses = chatMessageMapper.toResponseList(
            chatMessageRepository.findAll()
        );
        return CompletableFuture.completedFuture(responses);
    }
}
