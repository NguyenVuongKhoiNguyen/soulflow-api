package com.poly.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poly.models.entities.Order;
import com.poly.models.entities.Payment;
import com.poly.models.enums.OrderStatus;
import com.poly.models.enums.PaymentMethod;
import com.poly.models.repositories.OrderRepository;
import com.poly.models.repositories.PaymentRepository;
import com.poly.models.requests.OrderRequest;
import com.poly.models.requests.SepayWebhookRequest;

class PaymentControllerIT extends BaseControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private Long testOrderId;

    @BeforeEach
    void setUp() {
        Order ord = new Order();
        ord.setFullname("Guest Buyer");
        ord.setPhone("0912345678");
        ord.setAddress("123 Street");
        ord.setStatus(OrderStatus.PENDING);
        ord.setDeleted(false);
        org.springframework.test.util.ReflectionTestUtils.setField(ord, "total", BigDecimal.valueOf(100_000));
        org.springframework.test.util.ReflectionTestUtils.setField(ord, "shippingFee", BigDecimal.ZERO);
        ord = orderRepository.save(ord);
        testOrderId = ord.getId();

        Payment payment = new Payment();
        payment.setOrder(ord);
        payment.setAmount(BigDecimal.valueOf(100_000));
        payment.setPaymentMethod(PaymentMethod.E_BANKING);
        payment.setPaid(false);
        payment = paymentRepository.save(payment);

        ord.setPayment(payment);
        orderRepository.save(ord);
    }

    // TC_PAY_IT_01: GET /checkout/orders/{id} tìm thấy đơn trả về trạng thái thanh toán và mã QR
    @Test
    @DisplayName("GET /checkout/orders/{id} - Lấy trạng thái thanh toán đơn hàng thành công")
    void getPaymentStatus_existingOrder_returns200() throws Exception {
        mockMvc.perform(get("/checkout/orders/" + testOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testOrderId.toString()))
                .andExpect(jsonPath("$.paid").value(false))
                .andExpect(jsonPath("$.paymentMethod").value("E_BANKING"));
    }

    // TC_PAY_IT_02: GET /checkout/orders/{id} đơn hàng không tồn tại trả về 404
    @Test
    @DisplayName("GET /checkout/orders/{id} - Không tìm thấy đơn hàng trả về 404 Not Found")
    void getPaymentStatus_notFound_returns404() throws Exception {
        mockMvc.perform(get("/checkout/orders/999999"))
                .andExpect(status().isNotFound());
    }

    // TC_PAY_IT_03: POST /checkout/orders khi chưa chọn cửa hàng trong session trả về 400
    @Test
    @DisplayName("POST /checkout/orders - Chưa chọn cửa hàng fulfillment trả về 400 Bad Request")
    void createOrder_withoutStore_returns400() throws Exception {
        OrderRequest request = new OrderRequest();
        request.setFullname("Guest Buyer");
        request.setPhone("0912345678");
        request.setAddress("123 Street");
        request.setPaymentMethod(PaymentMethod.COD);

        mockMvc.perform(post("/checkout/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // TC_PAY_IT_04: POST /webhook/sepay không có Authorization header hợp lệ trả về 401 hoặc 503
    @Test
    @DisplayName("POST /webhook/sepay - Sai authentication trả về 401 hoặc 503 nếu chưa config")
    void sepayWebhook_unauthorized_returns401Or503() throws Exception {
        SepayWebhookRequest webhookReq = new SepayWebhookRequest(
                123L, "Vietcombank", "2026-01-01 10:00:00", "6011246825",
                null, "DH" + testOrderId, "DH" + testOrderId, "in",
                BigDecimal.valueOf(100_000), "REF123");

        mockMvc.perform(post("/webhook/sepay")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookReq)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(status == 401 || status == 503);
                });
    }

    // TC_PAY_IT_05: POST /webhook/sepay với body rỗng trả về 400 Bad Request
    @Test
    @DisplayName("POST /webhook/sepay - Body rỗng trả về 400 Bad Request")
    void sepayWebhook_blankBody_returns400() throws Exception {
        mockMvc.perform(post("/webhook/sepay")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
