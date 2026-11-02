package com.poly.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import com.poly.models.requests.DiscountRequest;

class DiscountControllerIT extends BaseControllerIT {

    // TC_DISC_05: Admin tạo mã giảm giá thành công
    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("POST /admin/discount - Admin tạo mã giảm giá hợp lệ trả về 200 OK")
    void saveDiscount_asAdmin_validRequest_returns200() throws Exception {
        DiscountRequest request = new DiscountRequest();
        request.setPercentage(BigDecimal.valueOf(20));
        request.setDescription("Giảm 20% cho khách hàng mới");
        request.setExpiredDate(LocalDateTime.now().plusMonths(1));

        mockMvc.perform(post("/admin/discount")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.percentage").value("20"));
    }

    // TC_DISC_07: Phần trăm giảm giá vượt quá 100% trả về 400 Bad Request
    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("POST /admin/discount - percentage > 100 trả về 400 Bad Request")
    void saveDiscount_percentageExceedsMax_returns400() throws Exception {
        DiscountRequest request = new DiscountRequest();
        request.setPercentage(BigDecimal.valueOf(120));
        request.setDescription("Giảm 120%");

        mockMvc.perform(post("/admin/discount")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // TC_DISC_08: Thiếu percentage trả về 400 Bad Request
    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("POST /admin/discount - thiếu percentage trả về 400 Bad Request")
    void saveDiscount_missingPercentage_returns400() throws Exception {
        DiscountRequest request = new DiscountRequest();
        request.setDescription("Không có percentage");

        mockMvc.perform(post("/admin/discount")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // TC_DISC_10: USER thường gọi tạo discount bị 403 Forbidden
    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("POST /admin/discount - quyền USER thường bị từ chối 403")
    void saveDiscount_asUser_returns403() throws Exception {
        DiscountRequest request = new DiscountRequest();
        request.setPercentage(BigDecimal.valueOf(10));
        request.setDescription("User tạo discount");

        mockMvc.perform(post("/admin/discount")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // TC_DISC_13: Chưa đăng nhập xóa discount bị 401 Unauthorized
    @Test
    @DisplayName("DELETE /admin/discount/{id} - chưa đăng nhập trả về 401")
    void deleteDiscount_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/admin/discount/1"))
                .andExpect(status().isUnauthorized());
    }

    // TC_DISC_15: Xem chi tiết discount với ID không tồn tại trả về 404 Not Found
    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("GET /admin/discount/{id} - ID không tồn tại trả về 404 Not Found")
    void getDiscountById_nonExistingId_returns404() throws Exception {
        mockMvc.perform(get("/admin/discount/999999"))
                .andExpect(status().isNotFound());
    }

    // TC_DISC_16: Phân trang discount chưa đăng nhập bị 401 Unauthorized
    @Test
    @DisplayName("GET /admin/discount - chưa đăng nhập trả về 401")
    void filterDiscounts_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/admin/discount"))
                .andExpect(status().isUnauthorized());
    }
}
