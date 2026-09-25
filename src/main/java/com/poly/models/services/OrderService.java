package com.poly.models.services;

import java.time.LocalDateTime;

import com.poly.models.enums.OrderStatus;
import com.poly.models.enums.SortOrder;
import com.poly.models.requests.OrderRequest;
import com.poly.models.requests.AdminOrderUpdateRequest;
import com.poly.models.responses.OrderResponse;
import com.poly.models.responses.PageResponse;

public interface OrderService {
	OrderResponse save(OrderRequest request);
	OrderResponse updateAdminInfo(Long orderId, AdminOrderUpdateRequest request);
	void softDeleteById(Long orderId);
	OrderResponse findById(Long orderId);
	PageResponse<OrderResponse> findMine(String username, Integer pageNumber, Integer pageSize);
	OrderResponse findMineById(String username, Long orderId);
	PageResponse<OrderResponse> filterAndPaginateOrders(
			String keyword,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            OrderStatus status,
            Boolean expired,
            Boolean deleted,
            SortOrder sortOrder,
            Integer pageNumber,
            Integer pageSize
    );
    void checkAndExpireBeforePagination(
        String keyword,
        LocalDateTime fromDate,
        LocalDateTime toDate,
        OrderStatus status,
        Boolean expired,
        Boolean deleted
    );

    Integer markOrderAsPaidIfFullyPaid(Long orderId);
}
