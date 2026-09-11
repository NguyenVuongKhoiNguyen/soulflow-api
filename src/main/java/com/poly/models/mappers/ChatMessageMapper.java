package com.poly.models.mappers;

import java.time.LocalDateTime;
import java.util.List;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.poly.models.entities.Account;
import com.poly.models.entities.ChatMessage;
import com.poly.models.repositories.AccountRepository;
import com.poly.models.repositories.ChatMessageRepository;
import com.poly.models.requests.ChatMessageRequest;
import com.poly.models.responses.ChatMessageResponse;
import com.poly.models.services.ImageService;

import jakarta.persistence.EntityNotFoundException;

@Component
@Mapper(componentModel = "spring")
public abstract class ChatMessageMapper {

    @Autowired
    protected ImageService imageService;

    @Autowired
    protected ChatMessageRepository chatMessageRepo;

    @Autowired
    protected AccountRepository accountRepo;

    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "sender", ignore = true)
    @Mapping(target = "receiver", ignore = true)
    public abstract ChatMessage toEntity(ChatMessageRequest request);

    @Mapping(target = "id",               source = "id")
    @Mapping(target = "senderId",         source = "sender.id")
    @Mapping(target = "receiverId",       source = "receiver.id")
    @Mapping(target = "senderUsername",   source = "sender.username")
    @Mapping(target = "senderFullname",   source = "sender.fullname")
    @Mapping(target = "createdDate",      source = "createdDate", dateFormat = "dd-MM-yyyy HH:mm:ss")
    @Mapping(target = "senderUrl",        ignore = true)
    public abstract ChatMessageResponse toResponse(ChatMessage chatMessage);

    public abstract List<ChatMessageResponse> toResponseList(List<ChatMessage> chatMessages);

    @AfterMapping
    protected void afterToEntity(ChatMessageRequest request, @MappingTarget ChatMessage chatMessage) {

        if (request.getId() != null) {
            ChatMessage existing = chatMessageRepo.findById(request.getId()).orElseThrow(
                () -> new EntityNotFoundException("Chat message not found with id: " + request.getId())
            );
            chatMessage.setCreatedDate(existing.getCreatedDate());
            chatMessage.setSender(existing.getSender());
            chatMessage.setReceiver(existing.getReceiver());
            return;
        }

        Account sender = accountRepo.findById(request.getSenderId()).orElseThrow(
            () -> new EntityNotFoundException("Sender account not found with id: " + request.getSenderId())
        );
        Account receiver = request.getReceiverId() == null
            ? null
            : accountRepo.findById(request.getReceiverId()).orElseThrow(
                () -> new EntityNotFoundException("Receiver account not found with id: " + request.getReceiverId())
            );
        chatMessage.setCreatedDate(LocalDateTime.now());
        chatMessage.setSender(sender);
        chatMessage.setReceiver(receiver);
    }

    @AfterMapping
    protected void afterToResponse(ChatMessage chatMessage, @MappingTarget ChatMessageResponse response) {

        String photo = chatMessage.getSender().getPhoto();
        if (photo == null || photo.isBlank()) {
            return;
        }

        try {
            String url = imageService.getPublicUrl(photo);
            response.setSenderUrl(url);
        } catch (Exception e) {
            response.setSenderUrl(null);
        }
    }
}
