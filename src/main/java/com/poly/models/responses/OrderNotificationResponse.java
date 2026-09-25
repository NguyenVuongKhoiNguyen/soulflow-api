package com.poly.models.responses;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderNotificationResponse {

    private String event;

    private String orderId;

    private String status;

    private String customerName;

    private String message;

    private LocalDateTime timestamp;
}
