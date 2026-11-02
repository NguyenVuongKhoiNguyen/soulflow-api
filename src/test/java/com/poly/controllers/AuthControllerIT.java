package com.poly.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poly.models.requests.AuthRequest;

import jakarta.servlet.http.Cookie;

class AuthControllerIT extends BaseControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // TC_AUTH_IT_01: GET /admin/auth/check khi chưa đăng nhập -> 401
    @Test
    @DisplayName("GET /admin/auth/check - Chưa đăng nhập trả về 401 Unauthorized")
    void checkAdminAccess_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/admin/auth/check"))
                .andExpect(status().isUnauthorized());
    }

    // TC_AUTH_IT_02: GET /admin/auth/check với quyền USER -> 403
    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("GET /admin/auth/check - Quyền USER bị từ chối 403 Forbidden")
    void checkAdminAccess_asUser_returns403() throws Exception {
        mockMvc.perform(get("/admin/auth/check"))
                .andExpect(status().isForbidden());
    }

    // TC_AUTH_IT_03: GET /admin/auth/check với quyền ADMIN -> 204 No Content
    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("GET /admin/auth/check - Quyền ADMIN trả về 204 No Content")
    void checkAdminAccess_asAdmin_returns204() throws Exception {
        mockMvc.perform(get("/admin/auth/check"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    // TC_AUTH_IT_04: POST /login với thông tin đăng nhập sai -> 401 Unauthorized
    @Test
    @DisplayName("POST /login - Sai mật khẩu hoặc tài khoản không tồn tại trả về 401 Unauthorized")
    void login_invalidCredentials_returns401() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setUsername("nonexistent_user");
        request.setPassword("wrong_password");

        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // TC_AUTH_IT_05: POST /login để trống thông tin -> 400 Bad Request
    @Test
    @DisplayName("POST /login - Thiếu username/password trả về 400 Bad Request")
    void login_blankFields_returns400() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setUsername("");
        request.setPassword("");

        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // TC_AUTH_IT_06: POST /auth/logout -> 204 No Content và xóa cookie
    @Test
    @DisplayName("POST /auth/logout - Đăng xuất trả về 204 và xóa cookie")
    void logout_success_clearsCookies() throws Exception {
        mockMvc.perform(post("/auth/logout")
                .cookie(new Cookie("refresh_token", "dummy_token")))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("account_token", 0))
                .andExpect(cookie().maxAge("refresh_token", 0));
    }

    // TC_AUTH_IT_07: POST /auth/refresh với cookie không hợp lệ -> 401 Unauthorized
    @Test
    @DisplayName("POST /auth/refresh - Refresh token không hợp lệ trả về 401 Unauthorized")
    void refresh_invalidToken_returns401() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                .cookie(new Cookie("refresh_token", "invalid_refresh_token")))
                .andExpect(status().isUnauthorized());
    }
}
