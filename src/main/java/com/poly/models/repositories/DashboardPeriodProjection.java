package com.poly.models.repositories;

import java.math.BigDecimal;

public interface DashboardPeriodProjection {
    String getPeriod();
    Long getOrderCount();
    BigDecimal getRevenue();
}
