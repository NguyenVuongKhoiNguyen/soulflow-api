package com.poly.models.services;

import java.time.LocalDateTime;

import com.poly.models.enums.SortOrder;
import com.poly.models.requests.CartRequest;
import com.poly.models.responses.CartResponse;
import com.poly.models.responses.PageResponse;

public interface CartService {
	CartResponse save(CartRequest request);
	void softDeleteById(Long cartId);
	CartResponse findById(Long cartId);
	PageResponse<CartResponse> filterAndPaginateCarts(
			String keyword,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Boolean expired,
            Boolean deleted,
            SortOrder sortOrder,
            Integer pageNumber,
            Integer pageSize);
    void checkAndExpireBeforePagination(
        String keyword,
        LocalDateTime fromDate,
        LocalDateTime toDate,
        Boolean expired,
        Boolean deleted
    );
}
