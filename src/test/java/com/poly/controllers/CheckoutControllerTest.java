package com.poly.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poly.config.JwtFilter;
import com.poly.config.SecurityConfig;
import com.poly.models.entities.Order;
import com.poly.models.enums.OrderStatus;
import com.poly.models.repositories.AccountRepository;
import com.poly.models.repositories.OrderRepository;
import com.poly.models.requests.OrderRequest;
import com.poly.models.responses.OrderResponse;
import com.poly.models.services.OrderService;
import com.poly.models.services.QrService;

@WebMvcTest(
    controllers = CheckoutController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class},
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {SecurityConfig.class, JwtFilter.class}
    )
)
@AutoConfigureMockMvc(addFilters = false)
public class CheckoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private OrderService orderService;
    @MockBean private OrderRepository orderRepository;
    @MockBean private AccountRepository accountRepository;
    @MockBean private QrService qrService;
    @MockBean private OrderNotificationWebSocketController notifications;

    private Principal mockPrincipal = () -> "testUser";

    @Test
    void testCreateOrder() throws Exception {
        String jsonReq = "{ \"fullname\": \"John\", \"phone\": \"0123456789\", \"address\": \"123 St\", \"paymentMethod\": \"COD\", \"orderDetailRequests\": [{\"productId\": 1, \"quantity\": 1}] }";
        
        OrderResponse res = new OrderResponse();
        res.setId("1");
        res.setTotal("100.0");
        res.setShippingFee("10.0");
        res.setStatus("PENDING");
        res.setPaymentMethod("COD");

        when(orderService.save(any(OrderRequest.class))).thenReturn(res);

        mockMvc.perform(post("/checkout/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonReq)
                .principal(mockPrincipal))
                .andExpect(status().isCreated());
    }

    @Test
    void testStatus() throws Exception {
        Order order = new Order();
        order.setId(1L);
        order.setShippingFee(BigDecimal.valueOf(10.0));
        order.calTotal();
        order.setStatus(OrderStatus.PENDING);
        
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        mockMvc.perform(get("/checkout/orders/1"))
                .andExpect(status().isOk());
    }
}
