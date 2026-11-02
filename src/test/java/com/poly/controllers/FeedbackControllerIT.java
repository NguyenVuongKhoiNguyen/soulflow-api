package com.poly.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import com.poly.models.entities.Account;
import com.poly.models.entities.Category;
import com.poly.models.entities.Product;
import com.poly.models.repositories.AccountRepository;
import com.poly.models.repositories.CategoryRepository;
import com.poly.models.repositories.ProductRepository;
import com.poly.models.requests.CommentRequest;

class FeedbackControllerIT extends BaseControllerIT {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    private Long testAccountId;
    private Long testProductId;

    @BeforeEach
    void setUpData() {
        if (accountRepository.count() == 0) {
            Account acc = new Account();
            acc.setUsername("user_fb_" + System.currentTimeMillis());
            acc.setPassword("password");
            acc.setEmail("fb_" + System.currentTimeMillis() + "@example.com");
            acc = accountRepository.save(acc);
            testAccountId = acc.getId();
        } else {
            testAccountId = accountRepository.findAll().get(0).getId();
        }

        if (productRepository.count() == 0) {
            Category cat = new Category();
            cat.setName("Cat FB");
            cat = categoryRepository.save(cat);

            Product p = new Product();
            p.setName("Hoa FB");
            p.setPrice(BigDecimal.valueOf(100_000));
            p.setAvailable(true);
            p.setDeleted(false);
            p.setCategory(cat);
            p = productRepository.save(p);
            testProductId = p.getId();
        } else {
            testProductId = productRepository.findAll().get(0).getId();
        }
    }

    // TC_FB_01: User đăng bình luận thành công
    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("POST /user/comment - User đăng bình luận thành công trả về 200 OK")
    void saveComment_asUser_returns200() throws Exception {
        CommentRequest request = new CommentRequest();
        request.setContent("Hoa giao rất đúng giờ và tươi!");
        request.setProductId(testProductId);
        request.setAccountId(testAccountId);

        mockMvc.perform(post("/user/comment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.content").value("Hoa giao rất đúng giờ và tươi!"));
    }

    // TC_FB_02: Để trống nội dung bình luận trả về 400 Bad Request
    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("POST /user/comment - nội dung để trống trả về 400 Bad Request")
    void saveComment_blankContent_returns400() throws Exception {
        CommentRequest request = new CommentRequest();
        request.setContent("   ");
        request.setProductId(testProductId);

        mockMvc.perform(post("/user/comment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // TC_FB_03: Thiếu productId trả về 400 Bad Request
    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("POST /user/comment - thiếu productId trả về 400 Bad Request")
    void saveComment_missingProductId_returns400() throws Exception {
        CommentRequest request = new CommentRequest();
        request.setContent("Bình luận không có sản phẩm");

        mockMvc.perform(post("/user/comment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // TC_FB_05: Chưa đăng nhập bình luận bị 401 Unauthorized
    @Test
    @DisplayName("POST /user/comment - chưa đăng nhập trả về 401 Unauthorized")
    void saveComment_unauthenticated_returns401() throws Exception {
        CommentRequest request = new CommentRequest();
        request.setContent("Khách chưa đăng nhập");
        request.setProductId(testProductId);

        mockMvc.perform(post("/user/comment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // TC_FB_06: User xóa bình luận thành công
    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("DELETE /user/comment/{id} - User xóa bình luận trả về 200 OK")
    void deleteComment_asUser_returns200() throws Exception {
        mockMvc.perform(delete("/user/comment/1"))
                .andExpect(status().isOk());
    }

    // TC_FB_08: Lấy danh sách bình luận công khai (GET /comment) trả về 200 OK
    @Test
    @DisplayName("GET /comment - lấy bình luận công khai trả về 200 OK")
    void getComments_public_returns200() throws Exception {
        mockMvc.perform(get("/comment"))
                .andExpect(status().isOk());
    }

    // TC_FB_14: Lấy danh sách câu trả lời công khai (GET /reply) trả về 200 OK
    @Test
    @DisplayName("GET /reply - lấy câu trả lời công khai trả về 200 OK")
    void getReplies_public_returns200() throws Exception {
        mockMvc.perform(get("/reply"))
                .andExpect(status().isOk());
    }
}
