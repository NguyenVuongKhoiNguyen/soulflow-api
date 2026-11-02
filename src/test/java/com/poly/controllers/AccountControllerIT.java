package com.poly.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poly.models.entities.Account;
import com.poly.models.entities.Role;
import com.poly.models.enums.RoleCode;
import com.poly.models.repositories.AccountRepository;
import com.poly.models.repositories.RoleRepository;
import com.poly.models.requests.AccountRequest;

class AccountControllerIT extends BaseControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Long testAccountId;

    @BeforeEach
    void setUp() {
        if (roleRepository.count() == 0) {
            Role userRole = new Role();
            userRole.setCode(RoleCode.USER);
            roleRepository.save(userRole);

            Role adminRole = new Role();
            adminRole.setCode(RoleCode.ADMIN);
            roleRepository.save(adminRole);
        }

        Account acc = accountRepository.findByUsername("testuser_acc").orElse(null);
        if (acc == null) {
            acc = new Account();
            acc.setUsername("testuser_acc");
            acc.setEmail("testuser_acc@gmail.com");
            acc.setFullname("Test User Acc");
            acc.setPassword("123456");
            acc.setDeleted(false);
            acc.setDisabled(false);
            acc = accountRepository.save(acc);
        }
        testAccountId = acc.getId();
    }

    // TC_ACC_IT_01: GET /admin/account chưa xác thực -> 401
    @Test
    @DisplayName("GET /admin/account - Chưa đăng nhập trả về 401 Unauthorized")
    void filterAccounts_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/admin/account"))
                .andExpect(status().isUnauthorized());
    }

    // TC_ACC_IT_02: GET /admin/account với USER -> 403
    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("GET /admin/account - Quyền USER bị từ chối 403 Forbidden")
    void filterAccounts_asUser_returns403() throws Exception {
        mockMvc.perform(get("/admin/account"))
                .andExpect(status().isForbidden());
    }

    // TC_ACC_IT_03: GET /admin/account với ADMIN -> 200 OK
    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("GET /admin/account - Quyền ADMIN trả về 200 OK với phân trang")
    void filterAccounts_asAdmin_returns200() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/admin/account")
                .param("pageNumber", "0")
                .param("pageSize", "5"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // TC_ACC_IT_04: GET /admin/account/{id} tồn tại -> 200 OK
    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("GET /admin/account/{id} - Tìm thấy trả về 200 OK")
    void findAccountById_existing_returns200() throws Exception {
        mockMvc.perform(get("/admin/account/" + testAccountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser_acc"));
    }

    // TC_ACC_IT_05: GET /admin/account/{id} không tồn tại -> 404 Not Found
    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("GET /admin/account/{id} - Không tìm thấy trả về 404 Not Found")
    void findAccountById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/admin/account/99999"))
                .andExpect(status().isNotFound());
    }

    // TC_ACC_IT_06: POST /admin/account thiếu dữ liệu bắt buộc -> 400 Bad Request
    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("POST /admin/account - Thiếu username/email trả về 400 Bad Request")
    void saveAccount_invalidData_returns400() throws Exception {
        AccountRequest request = new AccountRequest();
        request.setUsername("");
        request.setEmail("invalid-email");

        MockMultipartFile part = new MockMultipartFile(
                "account", "", "application/json", objectMapper.writeValueAsBytes(request));

        mockMvc.perform(multipart("/admin/account").file(part))
                .andExpect(status().isBadRequest());
    }

    // TC_ACC_IT_07: DELETE /admin/account/{id} thành công -> 200 OK
    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("DELETE /admin/account/{id} - Xóa mềm tài khoản trả về 200 OK")
    void deleteAccount_asAdmin_returns200() throws Exception {
        mockMvc.perform(delete("/admin/account/" + testAccountId))
                .andExpect(status().isOk());
    }

    // TC_ACC_IT_08: POST /user/account/update khi đăng nhập USER hợp lệ -> 200 OK
    @Test
    @WithMockUser(username = "testuser_acc", authorities = "USER")
    @DisplayName("POST /user/account/update - User cập nhật profile trả về 200 OK")
    void updateProfile_asUser_returns200() throws Exception {
        AccountRequest request = new AccountRequest();
        request.setFullname("Test User Updated");
        request.setUsername("testuser_acc");
        request.setEmail("testuser_acc@gmail.com");

        MockMultipartFile part = new MockMultipartFile(
                "account", "", "application/json", objectMapper.writeValueAsBytes(request));

        mockMvc.perform(multipart("/user/account/update").file(part))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullname").value("Test User Updated"));
    }
}
