package com.poly.models.responses;

public record DashboardCategorySalesResponse(
    String categoryName,
    Long sales
) {}
