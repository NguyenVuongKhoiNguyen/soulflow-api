package com.poly.models.responses;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationResponse {
    private String id;
    private String type;
    private String event;
    private String message;
    private String timestamp;
    private Boolean read;
}
