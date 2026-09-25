package com.poly.controllers;

import java.security.Principal;
import java.time.LocalDateTime;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.poly.models.enums.PaymentMethod;
import com.poly.models.enums.OrderStatus;
import com.poly.models.repositories.AccountRepository;
import com.poly.models.repositories.OrderRepository;
import com.poly.models.requests.OrderRequest;
import com.poly.models.services.OrderService;
import com.poly.models.services.QrService;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/checkout/orders")
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class CheckoutController {
    private final OrderService orderService;
    private final OrderRepository orders;
    private final AccountRepository accounts;
    private final QrService qrService;
    private final OrderNotificationWebSocketController notifications;

    public record PaymentStatus(String id, String total, String shippingFee, String status, String paymentMethod,
                                String paymentReference, String qrUrl, boolean expired, boolean paid) {}

    @PostMapping
    public ResponseEntity<PaymentStatus> create(@RequestBody @Valid OrderRequest request, Principal principal, HttpSession session) {
        Object selectedStoreId = session.getAttribute("selectedStoreId");
        if (!(selectedStoreId instanceof Long storeId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "A fulfilment store must be selected before an order can be placed");
        }
        request.setStoreId(storeId);
        if (request.getId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order ID must be null");
        }
        if (principal != null) {
            var account = accounts.findByUsername(principal.getName()).orElse(null);
            if (account != null) {
                request.setAccountId(account.getId());
            }
        }
        request.getOrderDetailRequests().forEach(item -> item.setId(null));
        request.setStatus(OrderStatus.PENDING);
        var saved = orderService.save(request);
        try { notifications.publishCreated(saved); }
        catch (RuntimeException ex) { log.error("Could not publish new order {}", saved.getId(), ex); }
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(new PaymentStatus(
            saved.getId(), saved.getTotal(), saved.getShippingFee(), saved.getStatus(), saved.getPaymentMethod(),
            saved.getPaymentReference(), saved.getQrUrl(), false, false));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<PaymentStatus> status(@PathVariable Long id, Principal principal) {
        var order = orders.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        boolean unauthorized = false;
        if (order.getAccount() != null) {
            if (principal == null || !order.getAccount().getUsername().equals(principal.getName())) {
                unauthorized = true;
            }
        }
        if (unauthorized || Boolean.TRUE.equals(order.getDeleted())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        var payment = order.getPayment();
        boolean paid = payment != null && Boolean.TRUE.equals(payment.getPaid());
        boolean expired = Boolean.TRUE.equals(order.getExpired()) || (order.getExpiredDate() != null
            && !order.getExpiredDate().isAfter(LocalDateTime.now()));
        boolean bank = payment != null && payment.getPaymentMethod() == PaymentMethod.E_BANKING;
        String reference = bank ? "DH" + id : null;
        String qr = bank && !paid && !expired && order.getStatus() == OrderStatus.PENDING
            ? qrService.buildQrUrl(order.getTotal(), reference) : null;
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new PaymentStatus(
            id.toString(), order.getTotal().toPlainString(), order.getShippingFee().toPlainString(), order.getStatus().name(),
            payment == null ? null : payment.getPaymentMethod().name(), reference, qr, expired, paid));
    }
}


