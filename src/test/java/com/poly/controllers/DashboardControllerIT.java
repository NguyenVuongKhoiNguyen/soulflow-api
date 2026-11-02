package com.poly.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.poly.models.responses.DashboardPeriodResponse;
import com.poly.models.responses.DashboardCategorySalesResponse;
import com.poly.models.responses.DashboardProductSalesResponse;
import com.poly.models.services.DashboardService;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class DashboardControllerIT extends BaseControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    // TC_DASH_IT_01: GET /admin/dashboard/daily khi chưa xác thực -> 401 hoặc 403
    @Test
    @DisplayName("GET /admin/dashboard/daily - Chưa đăng nhập trả về 401/403")
    void findDaily_unauthenticated_returns401Or403() throws Exception {
        mockMvc.perform(get("/admin/dashboard/daily")
                .param("fromDate", "2025-01-01")
                .param("toDate", "2025-01-31"))
                .andExpect(status().isUnauthorized());
    }

    // TC_DASH_IT_02: GET /admin/dashboard/daily với quyền USER -> 403 Forbidden
    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("GET /admin/dashboard/daily - Quyền USER bị từ chối 403 Forbidden")
    void findDaily_asUser_returns403() throws Exception {
        mockMvc.perform(get("/admin/dashboard/daily")
                .param("fromDate", "2025-01-01")
                .param("toDate", "2025-01-31"))
                .andExpect(status().isForbidden());
    }

    // TC_DASH_IT_03: GET /admin/dashboard/daily với quyền ADMIN và tham số hợp lệ -> 200 OK
    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("GET /admin/dashboard/daily - Quyền ADMIN trả về 200 OK")
    void findDaily_asAdmin_returns200() throws Exception {
        when(dashboardService.findDaily(any(), any()))
                .thenReturn(List.of(new DashboardPeriodResponse("2025-01-01", 5L, BigDecimal.valueOf(500000))));
        MvcResult mvcResult = mockMvc.perform(get("/admin/dashboard/daily")
                .param("fromDate", "2025-01-01")
                .param("toDate", "2025-01-31"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // TC_DASH_IT_04: GET /admin/dashboard/daily thiếu param -> 400 Bad Request
    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("GET /admin/dashboard/daily - Thiếu param trả về 400 Bad Request")
    void findDaily_missingParams_returns400() throws Exception {
        mockMvc.perform(get("/admin/dashboard/daily"))
                .andExpect(status().isBadRequest());
    }

    // TC_DASH_IT_05: GET /admin/dashboard/quarterly với quyền ADMIN -> 200 OK
    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("GET /admin/dashboard/quarterly - Quyền ADMIN trả về 200 OK")
    void findQuarterly_asAdmin_returns200() throws Exception {
        when(dashboardService.findQuarterly(any(), any()))
                .thenReturn(List.of(new DashboardPeriodResponse("Q1-2025", 10L, BigDecimal.valueOf(1000000))));
        MvcResult mvcResult = mockMvc.perform(get("/admin/dashboard/quarterly")
                .param("fromYear", "2024")
                .param("toYear", "2025"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // TC_DASH_IT_06: GET /admin/dashboard/yearly với quyền ADMIN -> 200 OK
    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("GET /admin/dashboard/yearly - Quyền ADMIN trả về 200 OK")
    void findYearly_asAdmin_returns200() throws Exception {
        when(dashboardService.findYearly(any(), any()))
                .thenReturn(List.of(new DashboardPeriodResponse("2025", 50L, BigDecimal.valueOf(50000000))));
        MvcResult mvcResult = mockMvc.perform(get("/admin/dashboard/yearly")
                .param("fromYear", "2024")
                .param("toYear", "2025"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // TC_DASH_IT_07: GET /admin/dashboard/daily/categories với quyền ADMIN -> 200 OK
    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("GET /admin/dashboard/daily/categories - Quyền ADMIN trả về 200 OK")
    void findDailyCategories_asAdmin_returns200() throws Exception {
        when(dashboardService.findDailyCategorySales(any(), any()))
                .thenReturn(List.of(new DashboardCategorySalesResponse("Hoa", 10L)));
        MvcResult mvcResult = mockMvc.perform(get("/admin/dashboard/daily/categories")
                .param("fromDate", "2025-01-01")
                .param("toDate", "2025-01-31"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // TC_DASH_IT_08: GET /admin/dashboard/daily/products với quyền ADMIN -> 200 OK
    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("GET /admin/dashboard/daily/products - Quyền ADMIN trả về 200 OK")
    void findDailyProducts_asAdmin_returns200() throws Exception {
        when(dashboardService.findDailyProductSales(any(), any()))
                .thenReturn(List.of(new DashboardProductSalesResponse("Bó hoa", 5L)));
        MvcResult mvcResult = mockMvc.perform(get("/admin/dashboard/daily/products")
                .param("fromDate", "2025-01-01")
                .param("toDate", "2025-01-31"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
