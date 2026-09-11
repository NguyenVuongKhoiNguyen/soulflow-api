package com.poly.models.services;

import java.time.LocalDateTime;
import java.util.List;

import com.poly.models.enums.SortOrder;
import com.poly.models.requests.DiscountRequest;
import com.poly.models.responses.DiscountResponse;
import com.poly.models.responses.PageResponse;
import com.poly.models.responses.ProductResponse;

public interface DiscountService {
	DiscountResponse save(DiscountRequest request);
	void softDeleteById(Long discountId);
	DiscountResponse findById(Long discountId);
	PageResponse<DiscountResponse> filterAndPaginateDiscounts(
        String keyword,
        LocalDateTime fromDate,
        LocalDateTime toDate,
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
        Boolean expired,
        Boolean deleted
    );

    ProductResponse addDiscounts(
        Long productId,
        List<Long> discountIds
    );

    DiscountResponse updateProducts(Long discountId, List<Long> productIds);
}
