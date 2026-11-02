package com.poly.models.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.poly.models.repositories.DashboardCategorySalesProjection;
import com.poly.models.repositories.DashboardPeriodProjection;
import com.poly.models.repositories.DashboardProductSalesProjection;
import com.poly.models.repositories.OrderRepository;
import com.poly.models.responses.DashboardCategorySalesResponse;
import com.poly.models.responses.DashboardPeriodResponse;
import com.poly.models.responses.DashboardProductSalesResponse;
import com.poly.models.services.impl.DashboardServiceImpl;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    // TC_DASH_01: findDaily - thành công khi ngày hợp lệ
    @Test
    @DisplayName("findDaily - thành công trả về danh sách period metrics")
    void findDaily_validDates_returnsList() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 10);

        DashboardPeriodProjection proj = mock(DashboardPeriodProjection.class);
        when(proj.getPeriod()).thenReturn("2025-01-01");
        when(proj.getOrderCount()).thenReturn(5L);
        when(proj.getRevenue()).thenReturn(BigDecimal.valueOf(500000));

        when(orderRepository.summarizeByDay(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(proj));

        List<DashboardPeriodResponse> result = dashboardService.findDaily(from, to);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("2025-01-01", result.get(0).period());
        assertEquals(5L, result.get(0).orderCount());
        assertEquals(BigDecimal.valueOf(500000), result.get(0).revenue());
    }

    // TC_DASH_02: findDaily - ném ResponseStatusException khi fromDate > toDate hoặc null
    @Test
    @DisplayName("findDaily - ném lỗi khi date range không hợp lệ")
    void findDaily_invalidDates_throwsBadRequest() {
        LocalDate from = LocalDate.of(2025, 1, 10);
        LocalDate to = LocalDate.of(2025, 1, 1);

        assertThrows(ResponseStatusException.class, () -> dashboardService.findDaily(null, to));
        assertThrows(ResponseStatusException.class, () -> dashboardService.findDaily(from, null));
        assertThrows(ResponseStatusException.class, () -> dashboardService.findDaily(from, to));
    }

    // TC_DASH_03: findQuarterly - thành công khi năm hợp lệ
    @Test
    @DisplayName("findQuarterly - thành công trả về metrics theo quý")
    void findQuarterly_validYears_returnsList() {
        DashboardPeriodProjection proj = mock(DashboardPeriodProjection.class);
        when(proj.getPeriod()).thenReturn("Q1-2025");
        when(proj.getOrderCount()).thenReturn(10L);
        when(proj.getRevenue()).thenReturn(BigDecimal.valueOf(1000000));

        when(orderRepository.summarizeByQuarter(2025, 2025)).thenReturn(List.of(proj));

        List<DashboardPeriodResponse> result = dashboardService.findQuarterly(2025, 2025);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Q1-2025", result.get(0).period());
    }

    // TC_DASH_04: findQuarterly - ném lỗi khi year range không hợp lệ
    @Test
    @DisplayName("findQuarterly - ném lỗi khi year không hợp lệ")
    void findQuarterly_invalidYears_throwsBadRequest() {
        assertThrows(ResponseStatusException.class, () -> dashboardService.findQuarterly(null, 2025));
        assertThrows(ResponseStatusException.class, () -> dashboardService.findQuarterly(1999, 2025));
        assertThrows(ResponseStatusException.class, () -> dashboardService.findQuarterly(2026, 2025));
        assertThrows(ResponseStatusException.class, () -> dashboardService.findQuarterly(2025, 10000));
    }

    // TC_DASH_05: findYearly - thành công khi năm hợp lệ
    @Test
    @DisplayName("findYearly - thành công trả về metrics theo năm")
    void findYearly_validYears_returnsList() {
        DashboardPeriodProjection proj = mock(DashboardPeriodProjection.class);
        when(proj.getPeriod()).thenReturn("2025");
        when(proj.getOrderCount()).thenReturn(50L);
        when(proj.getRevenue()).thenReturn(BigDecimal.valueOf(50000000));

        when(orderRepository.summarizeByYear(2024, 2025)).thenReturn(List.of(proj));

        List<DashboardPeriodResponse> result = dashboardService.findYearly(2024, 2025);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("2025", result.get(0).period());
    }

    // TC_DASH_06: findDailyCategorySales - thành công và lỗi validation
    @Test
    @DisplayName("findDailyCategorySales - thành công trả về doanh số danh mục")
    void findDailyCategorySales_successAndFailure() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 10);

        DashboardCategorySalesProjection proj = mock(DashboardCategorySalesProjection.class);
        when(proj.getCategoryName()).thenReturn("Hoa Hồng");
        when(proj.getSales()).thenReturn(100L);

        when(orderRepository.summarizeCategorySalesByDay(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(proj));

        List<DashboardCategorySalesResponse> result = dashboardService.findDailyCategorySales(from, to);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Hoa Hồng", result.get(0).categoryName());
        assertEquals(100L, result.get(0).sales());

        assertThrows(ResponseStatusException.class, () -> dashboardService.findDailyCategorySales(to, from));
    }

    // TC_DASH_07: findQuarterlyCategorySales & findYearlyCategorySales
    @Test
    @DisplayName("findQuarterlyCategorySales và findYearlyCategorySales thành công")
    void findCategorySales_yearRange_returnsList() {
        DashboardCategorySalesProjection proj = mock(DashboardCategorySalesProjection.class);
        when(proj.getCategoryName()).thenReturn("Hoa Lan");
        when(proj.getSales()).thenReturn(50L);

        when(orderRepository.summarizeCategorySalesByYearRange(2024, 2025)).thenReturn(List.of(proj));

        List<DashboardCategorySalesResponse> qResult = dashboardService.findQuarterlyCategorySales(2024, 2025);
        List<DashboardCategorySalesResponse> yResult = dashboardService.findYearlyCategorySales(2024, 2025);

        assertEquals(1, qResult.size());
        assertEquals(1, yResult.size());
    }

    // TC_DASH_08: findDailyProductSales - thành công và thất bại
    @Test
    @DisplayName("findDailyProductSales - thành công trả về doanh số sản phẩm")
    void findDailyProductSales_successAndFailure() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 10);

        DashboardProductSalesProjection proj = mock(DashboardProductSalesProjection.class);
        when(proj.getProductName()).thenReturn("Bó hoa cưới");
        when(proj.getSales()).thenReturn(20L);

        when(orderRepository.summarizeProductSalesByDay(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(proj));

        List<DashboardProductSalesResponse> result = dashboardService.findDailyProductSales(from, to);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Bó hoa cưới", result.get(0).productName());

        assertThrows(ResponseStatusException.class, () -> dashboardService.findDailyProductSales(null, to));
    }

    // TC_DASH_09: findQuarterlyProductSales & findYearlyProductSales
    @Test
    @DisplayName("findQuarterlyProductSales và findYearlyProductSales thành công")
    void findProductSales_yearRange_returnsList() {
        DashboardProductSalesProjection proj = mock(DashboardProductSalesProjection.class);
        when(proj.getProductName()).thenReturn("Hoa Tulip");
        when(proj.getSales()).thenReturn(15L);

        when(orderRepository.summarizeProductSalesByYearRange(2024, 2025)).thenReturn(List.of(proj));

        List<DashboardProductSalesResponse> qResult = dashboardService.findQuarterlyProductSales(2024, 2025);
        List<DashboardProductSalesResponse> yResult = dashboardService.findYearlyProductSales(2024, 2025);

        assertEquals(1, qResult.size());
        assertEquals(1, yResult.size());
    }
}
