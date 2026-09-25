package com.poly.models.services;

import com.poly.models.responses.NotificationResponse;
import com.poly.models.responses.PageResponse;

public interface NotificationService {
    NotificationResponse create(String type, String event, String message);
    PageResponse<NotificationResponse> findAll(Integer pageNumber, Integer pageSize);
    void markAllRead();
    void clearAll();
}
