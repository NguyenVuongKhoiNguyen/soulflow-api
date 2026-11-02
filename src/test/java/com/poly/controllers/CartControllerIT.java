package com.poly.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import com.poly.models.entities.Account;
import com.poly.models.repositories.AccountRepository;
import com.poly.models.requests.CartRequest;

class CartControllerIT extends BaseControllerIT {

    @Autowired
    private AccountRepository accountRepository;

    private Long testAccountId;

    @BeforeEach
    void setUpAccount() {
        if (accountRepository.count() == 0) {
            Account acc = new Account();
            acc.setUsername("testuser_" + System.currentTimeMillis());
            acc.setPassword("password");
            acc.setEmail("test_" + System.currentTimeMillis() + "@example.com");
            acc = accountRepository.save(acc);
            testAccountId = acc.getId();
        } else {
            testAccountId = accountRepository.findAll().get(0).getId();
        }
    }

    // TC_CART_01: User lưu giỏ hàng thành công
    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("POST /user/cart - User lưu giỏ hàng trả về 200 OK")
    void saveCart_asUser_returns200() throws Exception {
        CartRequest request = new CartRequest();
        request.setAccountId(testAccountId);
        request.setItemRequests(Collections.emptyList());

        mockMvc.perform(post("/user/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
    }

    // TC_CART_04: Chưa đăng nhập lưu giỏ hàng bị 401 Unauthorized
    @Test
    @DisplayName("POST /user/cart - chưa đăng nhập trả về 401 Unauthorized")
    void saveCart_unauthenticated_returns401() throws Exception {
        CartRequest request = new CartRequest();
        request.setAccountId(testAccountId);

        mockMvc.perform(post("/user/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // TC_CART_05: User xóa giỏ hàng thành công
    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("DELETE /user/cart/{id} - User xóa giỏ hàng trả về 200 OK")
    void deleteCart_asUser_returns200() throws Exception {
        mockMvc.perform(delete("/user/cart/1"))
                .andExpect(status().isOk());
    }

    // TC_CART_08: Chưa đăng nhập xóa giỏ hàng bị 401 Unauthorized
    @Test
    @DisplayName("DELETE /user/cart/{id} - chưa đăng nhập trả về 401")
    void deleteCart_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/user/cart/1"))
                .andExpect(status().isUnauthorized());
    }

    // TC_CART_16: Chưa đăng nhập phân trang giỏ hàng bị 401 Unauthorized
    @Test
    @DisplayName("GET /user/cart - chưa đăng nhập trả về 401")
    void filterCarts_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/user/cart"))
                .andExpect(status().isUnauthorized());
    }
}
