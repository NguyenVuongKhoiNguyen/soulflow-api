package com.poly.models.services.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.regex.Pattern;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.poly.models.entities.Order;
import com.poly.models.enums.OrderStatus;
import com.poly.models.enums.PaymentMethod;
import com.poly.models.repositories.OrderRepository;
import com.poly.models.mappers.OrderMapper;
import com.poly.controllers.OrderNotificationWebSocketController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poly.models.requests.SepayWebhookRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Slf4j
public class SepayWebhookService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final OrderRepository orders;
    private final OrderMapper orderMapper;
    private final CacheManager cacheManager;
    private final OrderNotificationWebSocketController notifications;
    private static final Pattern REFERENCE = Pattern.compile("(?i)(?<![A-Z0-9])DH([1-9][0-9]*)(?![A-Z0-9])");

    @Value("${SEPAY_WEBHOOK_API_KEY:}") private String apiKey;
    @Value("${SEPAY_ACCOUNT_NUMBER:}") private String accountNumber;

    public void authenticate(String authorization) {
        if (apiKey.isBlank() || accountNumber.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SePay webhook is not configured");
        }
        if (authorization == null || !authorization.regionMatches(true, 0, "Apikey ", 0, 7)
                || !MessageDigest.isEqual(apiKey.getBytes(StandardCharsets.UTF_8),
                    authorization.substring(7).getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook authentication");
        }
    }

    @Transactional
    public void receive(SepayWebhookRequest request) {
        if (!accountNumber.equals(request.accountNumber())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unexpected receiving account");
        }
        if (!"in".equals(request.transferType())) return;

        final String payload;
        try {
            payload = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize webhook", ex);
        }
        // Lock the transaction ID range so simultaneous deliveries cannot insert twice.
        jdbc.update("""
            INSERT INTO sepay_webhook_receipts (transaction_id, payload)
            SELECT ?, ? WHERE NOT EXISTS (
                SELECT 1 FROM sepay_webhook_receipts WITH (UPDLOCK, HOLDLOCK)
                WHERE transaction_id = ?
            )
            """, request.id(), payload, request.id());

        // Use the first authenticated payload, never an altered duplicate delivery.
        var receipts = jdbc.query("""
            SELECT payload FROM sepay_webhook_receipts WITH (UPDLOCK, HOLDLOCK)
            WHERE transaction_id = ? AND processed = 0
            """, (rs, row) -> rs.getString("payload"), request.id());
        if (receipts.isEmpty()) return;
        final SepayWebhookRequest receipt;
        try {
            receipt = objectMapper.readValue(receipts.get(0), SepayWebhookRequest.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not read stored webhook", ex);
        }
        if (!accountNumber.equals(receipt.accountNumber()) || !"in".equals(receipt.transferType())) {
            finish(receipt.id(), null, "INVALID_TRANSFER");
            return;
        }
        Long orderId = resolveOrderId(receipt);
        if (orderId == null) {
            finish(receipt.id(), null, "INVALID_REFERENCE");
            return;
        }
        Order order = orders.findForPayment(orderId).orElse(null);
        if (order == null) {
            finish(receipt.id(), orderId, "ORDER_NOT_FOUND");
            return;
        }
        var payment = order.getPayment();
        String outcome = null;
        if (Boolean.TRUE.equals(order.getDeleted()) || order.getStatus() == OrderStatus.CANCELLED) {
            outcome = "ORDER_UNAVAILABLE";
        } else if (payment != null && Boolean.TRUE.equals(payment.getPaid())) {
            outcome = "ALREADY_PAID";
        } else if (order.getStatus() != OrderStatus.PENDING) {
            outcome = "ORDER_NOT_PENDING";
        } else if (Boolean.TRUE.equals(order.getExpired()) || (order.getExpiredDate() != null
                && !order.getExpiredDate().isAfter(LocalDateTime.now()))) {
            outcome = "ORDER_EXPIRED";
        } else if (payment == null || payment.getPaymentMethod() != PaymentMethod.E_BANKING) {
            outcome = "WRONG_PAYMENT_METHOD";
        } else if (receipt.transferAmount() == null || receipt.transferAmount().signum() <= 0
                || order.getTotal() == null || order.getTotal().compareTo(receipt.transferAmount()) != 0) {
            outcome = "AMOUNT_MISMATCH";
        }
        if (outcome != null) {
            finish(receipt.id(), orderId, outcome);
            return;
        }

        payment.setAmount(receipt.transferAmount());
        payment.setPaid(true);
        payment.setPaymentDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PAID);
        orders.flush();
        finish(receipt.id(), orderId, "PAID");
        var response = orderMapper.toResponse(order);
        // Only publish and invalidate caches after payment and receipt both commit.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                try {
                    var details = cacheManager.getCache("orderList");
                    if (details != null) {
                        details.evict(orderId);
                        details.evict(orderId.toString());
                    }
                    var pages = cacheManager.getCache("orderPages");
                    if (pages != null) pages.clear();
                } catch (RuntimeException ex) {
                    log.error("Could not invalidate caches for paid order {}", orderId, ex);
                }
                try { notifications.publishUpdated(response); }
                catch (RuntimeException ex) {
                    log.error("Could not publish paid order {}", orderId, ex);
                }
            }
        });
    }

    private void finish(Long transactionId, Long orderId, String outcome) {
        jdbc.update("""
            UPDATE sepay_webhook_receipts
            SET processed = 1, order_id = ?, outcome = ?, processed_at = SYSUTCDATETIME()
            WHERE transaction_id = ?
            """, orderId, outcome, transactionId);
    }

    private Long resolveOrderId(SepayWebhookRequest receipt) {
        String code = receipt.code() == null ? "" : receipt.code().trim();
        String content = receipt.content() == null ? "" : receipt.content();
        var matcher = REFERENCE.matcher(content);
        Long contentId = null;
        try {
            while (matcher.find()) {
                long id = Long.parseLong(matcher.group(1));
                if (contentId != null && contentId.longValue() != id) return null;
                contentId = id;
            }
            if (code.isEmpty()) return contentId;
            var codeMatcher = REFERENCE.matcher(code);
            if (!codeMatcher.matches()) return null;
            long codeId = Long.parseLong(codeMatcher.group(1));
            return contentId == null || contentId.longValue() == codeId ? codeId : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
