package com.poly.controllers;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.poly.models.responses.OrderResponse;
import com.poly.models.services.NotificationService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class OrderNotificationWebSocketController {

    public static final String ORDER_NOTIFICATION_TOPIC = "/topic/notifications/orders";

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    public void publishCreated(OrderResponse order) {
        publish("CREATED", order);
    }

    public void publishUpdated(OrderResponse order) {
        publish("UPDATED", order);
    }

    private void publish(String event, OrderResponse order) {
        var notification = notificationService.create(
            "order", event, "Order " + order.getId() + " was " + event.toLowerCase()
        );

        messagingTemplate.convertAndSend(ORDER_NOTIFICATION_TOPIC, notification);
    }
}
