package com.poly.models.services;

import com.poly.models.enums.SortOrder;
import com.poly.models.requests.ProductImageRequest;
import com.poly.models.responses.PageResponse;
import com.poly.models.responses.ProductImageResponse;

public interface ProductImageService {

	ProductImageResponse save(ProductImageRequest request);
	
	ProductImageResponse findById(Long id);

	void softDeleteById(Long id);
	
	PageResponse<ProductImageResponse> filterAndPagonateProductImages(
		String keyword,
		SortOrder sortOrder,
		Integer pageNumber,
		Integer pageSize
	);
}
