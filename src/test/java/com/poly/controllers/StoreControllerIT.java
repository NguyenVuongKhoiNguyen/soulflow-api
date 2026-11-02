package com.poly.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import com.poly.models.requests.StoreRequest;

class StoreControllerIT extends BaseControllerIT {

    // TC_STR_05: Admin tạo chi nhánh mới thành công
    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("POST /admin/store - Admin tạo chi nhánh trả về 200 OK")
    void saveStore_asAdmin_validRequest_returns200() throws Exception {
        StoreRequest request = new StoreRequest();
        request.setName("Chi nhánh Q3 - " + System.currentTimeMillis());
        request.setPhone("0908889999");
        request.setAddress("456 Cách Mạng Tháng 8, Q3, TP.HCM");

        mockMvc.perform(post("/admin/store")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value(request.getName()));
    }

    // TC_STR_07: Để trống các trường bắt buộc trả về 400 Bad Request
    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("POST /admin/store - để trống name trả về 400 Bad Request")
    void saveStore_blankName_returns400() throws Exception {
        StoreRequest request = new StoreRequest();
        request.setName("   ");
        request.setPhone("0901234567");
        request.setAddress("123 Lê Lợi");

        mockMvc.perform(post("/admin/store")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // TC_STR_10: USER thường gọi endpoint Admin bị 403 Forbidden
    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("POST /admin/store - quyền USER thường bị từ chối 403")
    void saveStore_asUser_returns403() throws Exception {
        StoreRequest request = new StoreRequest();
        request.setName("Chi nhánh USER");
        request.setPhone("0901234567");
        request.setAddress("123 Lê Lợi");

        mockMvc.perform(post("/admin/store")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // TC_STR_13: Chưa đăng nhập gọi endpoint Admin bị 401 Unauthorized
    @Test
    @DisplayName("DELETE /admin/store/{id} - chưa đăng nhập trả về 401")
    void deleteStore_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/admin/store/1"))
                .andExpect(status().isUnauthorized());
    }

    // TC_STR_15: Xem chi nhánh ID không tồn tại trả về 404 Not Found
    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("GET /admin/store/{id} - ID không tồn tại trả về 404 Not Found")
    void getStoreById_nonExistingId_returns404() throws Exception {
        mockMvc.perform(get("/admin/store/999999"))
                .andExpect(status().isNotFound());
    }

    // TC_STR_16: Lọc và phân trang chi nhánh chưa đăng nhập bị 401 Unauthorized
    @Test
    @DisplayName("GET /admin/store - chưa đăng nhập trả về 401")
    void filterStores_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/admin/store"))
                .andExpect(status().isUnauthorized());
    }
}
