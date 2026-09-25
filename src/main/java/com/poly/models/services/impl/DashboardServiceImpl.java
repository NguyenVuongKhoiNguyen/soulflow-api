package com.poly.models.services.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.poly.models.repositories.DashboardPeriodProjection;
import com.poly.models.repositories.DashboardCategorySalesProjection;
import com.poly.models.repositories.DashboardProductSalesProjection;
import com.poly.models.repositories.OrderRepository;
import com.poly.models.responses.DashboardPeriodResponse;
import com.poly.models.responses.DashboardCategorySalesResponse;
import com.poly.models.responses.DashboardProductSalesResponse;
import com.poly.models.services.DashboardService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {
    private final OrderRepository orderRepository;

    @Override
    public List<DashboardPeriodResponse> findDaily(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid day range is required");
        }
        return toResponses(orderRepository.summarizeByDay(fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay()));
    }

    @Override
    public List<DashboardPeriodResponse> findQuarterly(Integer fromYear, Integer toYear) {
        validateYearRange(fromYear, toYear);
        return toResponses(orderRepository.summarizeByQuarter(fromYear, toYear));
    }

    @Override
    public List<DashboardPeriodResponse> findYearly(Integer fromYear, Integer toYear) {
        validateYearRange(fromYear, toYear);
        return toResponses(orderRepository.summarizeByYear(fromYear, toYear));
    }

    @Override
    public List<DashboardCategorySalesResponse> findDailyCategorySales(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid day range is required");
        }
        return toCategorySalesResponses(orderRepository.summarizeCategorySalesByDay(
            fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay()));
    }

    @Override
    public List<DashboardCategorySalesResponse> findQuarterlyCategorySales(Integer fromYear, Integer toYear) {
        validateYearRange(fromYear, toYear);
        return toCategorySalesResponses(orderRepository.summarizeCategorySalesByYearRange(fromYear, toYear));
    }

    @Override
    public List<DashboardCategorySalesResponse> findYearlyCategorySales(Integer fromYear, Integer toYear) {
        validateYearRange(fromYear, toYear);
        return toCategorySalesResponses(orderRepository.summarizeCategorySalesByYearRange(fromYear, toYear));
    }

    @Override
    public List<DashboardProductSalesResponse> findDailyProductSales(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid day range is required");
        }
        return toProductSalesResponses(orderRepository.summarizeProductSalesByDay(
            fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay()));
    }

    @Override
    public List<DashboardProductSalesResponse> findQuarterlyProductSales(Integer fromYear, Integer toYear) {
        validateYearRange(fromYear, toYear);
        return toProductSalesResponses(orderRepository.summarizeProductSalesByYearRange(fromYear, toYear));
    }

    @Override
    public List<DashboardProductSalesResponse> findYearlyProductSales(Integer fromYear, Integer toYear) {
        validateYearRange(fromYear, toYear);
        return toProductSalesResponses(orderRepository.summarizeProductSalesByYearRange(fromYear, toYear));
    }

    private void validateYearRange(Integer fromYear, Integer toYear) {
        if (fromYear == null || toYear == null || fromYear < 2000 || toYear > 9999 || fromYear > toYear) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid year range is required");
        }
    }

    private List<DashboardPeriodResponse> toResponses(List<DashboardPeriodProjection> projections) {
        return projections.stream()
            .map(value -> new DashboardPeriodResponse(value.getPeriod(), value.getOrderCount(), value.getRevenue()))
            .toList();
    }

    private List<DashboardCategorySalesResponse> toCategorySalesResponses(List<DashboardCategorySalesProjection> projections) {
        return projections.stream()
            .map(value -> new DashboardCategorySalesResponse(value.getCategoryName(), value.getSales()))
            .toList();
    }

    private List<DashboardProductSalesResponse> toProductSalesResponses(List<DashboardProductSalesProjection> projections) {
        return projections.stream()
            .map(value -> new DashboardProductSalesResponse(value.getProductName(), value.getSales()))
            .toList();
    }
}
