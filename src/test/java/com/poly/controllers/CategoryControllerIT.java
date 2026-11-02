package com.poly.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import com.poly.models.requests.CategoryRequest;

class CategoryControllerIT extends BaseControllerIT {

    // TC_CAT_01: Lấy danh sách danh mục công khai (Public)
    @Test
    @DisplayName("GET /category/list - công khai trả về 200 OK")
    void getCategories_publicEndpoint_returns200() throws Exception {
        mockMvc.perform(get("/category/list")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    // TC_CAT_03: Phương thức sai - Gửi POST tới endpoint GET /category/list
    @Test
    @DisplayName("POST /category/list - phương thức sai trả về 405 Method Not Allowed")
    void postCategoryList_wrongMethod_returns405() throws Exception {
        mockMvc.perform(post("/category/list"))
                .andExpect(status().isMethodNotAllowed());
    }

    // TC_CAT_05: Admin tạo danh mục mới thành công
    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("POST /admin/category - Admin tạo danh mục trả về 200 OK")
    void saveCategory_asAdmin_validRequest_returns200() throws Exception {
        CategoryRequest request = new CategoryRequest(null, "Hoa Tình Yêu " + System.currentTimeMillis(), "Mô tả hoa");
        mockMvc.perform(post("/admin/category")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value(request.getName()));
    }

    // TC_CAT_07: Thất bại khi để trống tên danh mục (name blank)
    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("POST /admin/category - name để trống trả về 400 Bad Request")
    void saveCategory_blankName_returns400() throws Exception {
        CategoryRequest request = new CategoryRequest(null, "   ", "Mô tả");
        mockMvc.perform(post("/admin/category")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // TC_CAT_10: USER thường gọi endpoint Admin bị 403 Forbidden
    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("POST /admin/category - quyền USER thường bị từ chối 403")
    void saveCategory_asUser_returns403() throws Exception {
        CategoryRequest request = new CategoryRequest(null, "Hoa USER", "Mô tả");
        mockMvc.perform(post("/admin/category")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // TC_CAT_13: Chưa đăng nhập gọi endpoint Admin bị 401 Unauthorized
    @Test
    @DisplayName("DELETE /admin/category/{id} - chưa đăng nhập trả về 401")
    void deleteCategory_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/admin/category/1"))
                .andExpect(status().isUnauthorized());
    }

    // TC_CAT_15: Lấy chi tiết danh mục với ID không tồn tại
    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("GET /admin/category/{id} - ID không tồn tại trả về 404 Not Found")
    void getCategoryById_nonExistingId_returns404() throws Exception {
        mockMvc.perform(get("/admin/category/999999"))
                .andExpect(status().isNotFound());
    }

    // TC_CAT_19: Phân trang danh mục gọi không token bị 401 Unauthorized
    @Test
    @DisplayName("GET /admin/category - chưa đăng nhập trả về 401")
    void filterCategories_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/admin/category"))
                .andExpect(status().isUnauthorized());
    }
}
