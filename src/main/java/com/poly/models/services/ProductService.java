package com.poly.models.services;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.poly.models.enums.SortOrder;
import com.poly.models.requests.ProductRequest;
import com.poly.models.responses.PageResponse;
import com.poly.models.responses.ProductResponse;

public interface ProductService {
	ProductResponse save(ProductRequest request);
	void softDeleteById(Long productId);
	ProductResponse findById(Long productId);
	ProductResponse findProductDetailById(Long productId);
	PageResponse<ProductResponse> filterAndPaginateProducts(
			String keyword, 
			BigDecimal minPrice, 
			BigDecimal maxPrice, 
			List<Long> categoryIds, 
			Boolean customised,
			Boolean available,
			Boolean deleted,
			LocalDateTime fromDate,
			LocalDateTime toDate,
			SortOrder sortOrder, 
			Integer pageNumber, 
			Integer pageSize
	);
	Integer decreaseQuantity(Long id, Integer amount);
	void warmupCache(Integer times);
}
