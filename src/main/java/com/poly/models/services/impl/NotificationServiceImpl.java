package com.poly.models.services.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.poly.models.entities.AppNotification;
import com.poly.models.repositories.NotificationRepository;
import com.poly.models.responses.NotificationResponse;
import com.poly.models.responses.PageResponse;
import com.poly.models.services.NotificationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public NotificationResponse create(String type, String event, String message) {
        AppNotification notification = new AppNotification();
        notification.setType(type);
        notification.setEvent(event);
        notification.setMessage(message);
        notification.setCreatedDate(LocalDateTime.now());
        notification.setRead(false);
        return toResponse(notificationRepository.save(notification));
    }

    @Override
    public PageResponse<NotificationResponse> findAll(Integer pageNumber, Integer pageSize) {
        Page<AppNotification> page = notificationRepository.findAll(
            PageRequest.of(pageNumber, pageSize, Sort.by("createdDate").descending())
        );
        List<NotificationResponse> responses = page.getContent().stream().map(this::toResponse).toList();
        return new PageResponse<>(page, responses);
    }

    @Override
    @Transactional
    public void markAllRead() {
        notificationRepository.markAllRead();
    }

    @Override
    @Transactional
    public void clearAll() {
        notificationRepository.deleteAllInBatch();
    }

    private NotificationResponse toResponse(AppNotification notification) {
        return NotificationResponse.builder()
            .id(notification.getId().toString())
            .type(notification.getType())
            .event(notification.getEvent())
            .message(notification.getMessage())
            .timestamp(notification.getCreatedDate().toString())
            .read(Boolean.TRUE.equals(notification.getRead()))
            .build();
    }
}
