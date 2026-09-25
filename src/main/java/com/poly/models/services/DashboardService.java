package com.poly.models.services;

import java.time.LocalDate;
import java.util.List;

import com.poly.models.responses.DashboardPeriodResponse;
import com.poly.models.responses.DashboardProductSalesResponse;
import com.poly.models.responses.DashboardCategorySalesResponse;

public interface DashboardService {
    List<DashboardPeriodResponse> findDaily(LocalDate fromDate, LocalDate toDate);
    List<DashboardPeriodResponse> findQuarterly(Integer fromYear, Integer toYear);
    List<DashboardPeriodResponse> findYearly(Integer fromYear, Integer toYear);
    List<DashboardCategorySalesResponse> findDailyCategorySales(LocalDate fromDate, LocalDate toDate);
    List<DashboardCategorySalesResponse> findQuarterlyCategorySales(Integer fromYear, Integer toYear);
    List<DashboardCategorySalesResponse> findYearlyCategorySales(Integer fromYear, Integer toYear);
    List<DashboardProductSalesResponse> findDailyProductSales(LocalDate fromDate, LocalDate toDate);
    List<DashboardProductSalesResponse> findQuarterlyProductSales(Integer fromYear, Integer toYear);
    List<DashboardProductSalesResponse> findYearlyProductSales(Integer fromYear, Integer toYear);
}
