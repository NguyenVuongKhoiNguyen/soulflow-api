package com.poly.models.responses;

import java.math.BigDecimal;

public record DashboardPeriodResponse(
    String period,
    Long orderCount,
    BigDecimal revenue
) {}
